package com.believer.mypublisher.oort;

import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SrsPublisher {

    private static AudioRecord mic;
    private static AcousticEchoCanceler aec;
    private static AutomaticGainControl agc;
    private byte[] mPcmBuffer = new byte[4096];
    private Thread aworker;
    private Thread vworker;
    private final Object writeLock = new Object();
    private boolean sendVideoOnly = false;
    private boolean sendAudioOnly = false;
    private int videoFrameCount;
    private long lastTimeMillis;
    private double mSamplingFps;

    private SrsFlvMuxer mFlvMuxer;
    private volatile boolean mIsEncoding = false;
    private ByteBuffer mGLPreviewBuffer;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private final Object syncpusher = new Object();
    private final Object syncpusher2 = new Object();
    //    private ConcurrentLinkedQueue<byte[]> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<EncodeVideoData> mGLIntBufferCache = new ConcurrentLinkedQueue<>();

    public SrsPublisher() {

    }

    public void onGetRgbaFrame(byte[] data, int width, int height, long pts) {
        if (mPreviewWidth != width || height != mPreviewHeight) {
            mPreviewWidth = width;
            mPreviewHeight = height;
            mGLPreviewBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight * 4);
        }

        if (mIsEncoding) {
            if (data != null) {
                EncodeVideoData encodeVideoData = new EncodeVideoData();
                encodeVideoData.DATA = data;
                encodeVideoData.PTS = pts;
                mGLIntBufferCache.add(encodeVideoData);
                synchronized (writeLock) {
                    writeLock.notifyAll();
                }
            }

        }

    }

    public void setEncodelevel(int type) {
        Log.i("encodec", "setEncodelevel: " + type);
        switch (type) {
            case EncodeTyep.esSmooth:
                SrsEncoder.getInstance().setVideoVSmoothMode();//极速300k
            case EncodeTyep.eSmooth:
                SrsEncoder.getInstance().setVideoSmoothMode();//流畅500k
                break;
            case EncodeTyep.eStandard:
                SrsEncoder.getInstance().setVideoStanderMode();//标清700k
                break;
            case EncodeTyep.eHD:
                SrsEncoder.getInstance().setVideoHDMode();//高清 1M
                break;
            case EncodeTyep.eHHD:
                SrsEncoder.getInstance().setVideoHHDMode();//超清1.2M
                break;
            default:
                SrsEncoder.getInstance().setVideoSmoothMode();//标清512k
                break;
        }
        if (mIsEncoding) {
            resetEncoding();
        }
    }

    public void setEncodes(int type) {
        switch (type) {
            case EncodeTyep.eHardCodes:
                SrsEncoder.getInstance().switchToHardEncoder();
                break;
            case EncodeTyep.eSoftCodes:
                SrsEncoder.getInstance().switchToSoftEncoder();
                break;
            default:
                SrsEncoder.getInstance().switchToHardEncoder();
                break;
        }
        if (mIsEncoding) {
            resetEncoding();
        }
    }

    private void resetEncoding() {
        stopEncode();
        startEncode();
    }

    public void enableEncoding() {
        vworker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    while (!mGLIntBufferCache.isEmpty()) {
//                        byte[] data = mGLIntBufferCache.poll();
                        //mGLPreviewBuffer.put(picture.array());
                        EncodeVideoData data = mGLIntBufferCache.poll();
                        if (!sendAudioOnly) {
                            //Log.i("=============="," SrsEncoder.getInstance().onGetRgbaFrame data size"+data.length);
                            if (data.DATA != null && SrsEncoder.getInstance().isEncodeStart())
                                SrsEncoder.getInstance().onGetAbgrFrame(data.DATA, mPreviewWidth, mPreviewHeight, data.PTS);
                        }

                    }
                    // Waiting for next frame
                    synchronized (writeLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            writeLock.wait(500);
                        } catch (InterruptedException ie) {
                            vworker.interrupt();
                        }
                    }
                }
            }
        });
        vworker.start();
        mIsEncoding = true;
    }

    public void disableEncoding() {
        mIsEncoding = false;
        mGLIntBufferCache.clear();

        if (vworker != null) {
            vworker.interrupt();
            try {
                vworker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                vworker.interrupt();
            }
            vworker = null;
        }
    }

    private void calcSamplingFps() {
        // Calculate sampling FPS
        if (videoFrameCount == 0) {
            lastTimeMillis = System.nanoTime() / 1000000;
            videoFrameCount++;
        } else {
            if (++videoFrameCount >= SrsEncoder.VGOP) {
                long diffTimeMillis = System.nanoTime() / 1000000 - lastTimeMillis;
                mSamplingFps = (double) videoFrameCount * 1000 / diffTimeMillis;
                videoFrameCount = 0;
            }
        }
    }


    public void startAudio() {
        mic = SrsEncoder.getInstance().chooseAudioRecord();
        if (mic == null) {
            return;
        }

        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(mic.getAudioSessionId());
            if (aec != null) {
                aec.setEnabled(true);
            }
        }

        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(mic.getAudioSessionId());
            if (agc != null) {
                agc.setEnabled(true);
            }
        }

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                mic.startRecording();
                while (!Thread.interrupted()) {
                    if (sendVideoOnly) {
                        SrsEncoder.getInstance().onGetPcmFrame(mPcmBuffer, mPcmBuffer.length);
                        try {
                            // This is trivial...
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        if (SrsEncoder.getInstance().isEncodeStart()) {
                            int size = mic.read(mPcmBuffer, 0, mPcmBuffer.length);
                            if (size > 0) {
                                SrsEncoder.getInstance().onGetPcmFrame(mPcmBuffer, size);
                            }
                        } else {
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        });
        aworker.start();
    }

    public void stopAudio() {
        if (aworker != null) {
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                aworker.interrupt();
            }
            aworker = null;
        }

        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }

        if (aec != null) {
            aec.setEnabled(false);
            aec.release();
            aec = null;
        }

        if (agc != null) {
            agc.setEnabled(false);
            agc.release();
            agc = null;
        }
    }

    public void startEncode() {
        synchronized (syncpusher2) {
            enableEncoding();
            if (!SrsEncoder.getInstance().start()) {
                return;
            }

            startAudio();

        }
    }

    public void stopEncode() {
        synchronized (syncpusher2) {
            synchronized (syncpusher) {
                if (mIsEncoding)
                    disableEncoding();

                stopAudio();
                SrsEncoder.getInstance().stop();
            }
        }
    }

    public void startPublish(String rtmpUrl) {
        synchronized (syncpusher) {
            if (mFlvMuxer != null) {
                mFlvMuxer.start(rtmpUrl);
                startEncode();
            }
            //延时0.5秒，避免频繁操作导致崩溃
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void stopPublish() {
        synchronized (syncpusher) {
            if (mFlvMuxer != null) {
                stopEncode();
                mFlvMuxer.stop();
            }
//延时0.5秒，避免频繁操作导致崩溃
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void switchToSoftEncoder() {
        SrsEncoder.getInstance().switchToSoftEncoder();
    }

    public void switchToHardEncoder() {
        SrsEncoder.getInstance().switchToHardEncoder();
    }

    public boolean isSoftEncoder() {
        return SrsEncoder.getInstance().isSoftEncoder();
    }

    public int getPreviewWidth() {
        return SrsEncoder.getInstance().getPreviewWidth();
    }

    public int getPreviewHeight() {
        return SrsEncoder.getInstance().getPreviewHeight();
    }

    public double getmSamplingFps() {
        return mSamplingFps;
    }


    public void setPreviewResolution(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        //mGLPreviewBuffer = ByteBuffer.allocateDirect(width * height * 4);
        SrsEncoder.getInstance().setPreviewResolution(width, height);
    }

    public void setOutputResolution(int width, int height) {
        if (width <= height) {
            SrsEncoder.getInstance().setPortraitResolution(width, height);
        } else {
            SrsEncoder.getInstance().setLandscapeResolution(width, height);
        }
    }

    public void setScreenOrientation(int orientation) {
        SrsEncoder.getInstance().setScreenOrientation(orientation);
    }

    public void setVideoHDMode() {
        SrsEncoder.getInstance().setVideoHDMode();
    }

    public void setVideoSmoothMode() {
        SrsEncoder.getInstance().setVideoSmoothMode();
    }

    public void setSendVideoOnly(boolean flag) {
        if (mic != null) {
            if (flag) {
                mic.stop();
                mPcmBuffer = new byte[4096];
            } else {
                mic.startRecording();
            }
        }
        sendVideoOnly = flag;
    }

    public void setSendAudioOnly(boolean flag) {
        sendAudioOnly = flag;
    }


    public void setRtmpHandler(RtmpListener handler) {
        mFlvMuxer = new SrsFlvMuxer(handler);
        if (SrsEncoder.getInstance() != null) {
            SrsEncoder.getInstance().setFlvMuxer(mFlvMuxer);
        }
    }


    public void setEncodeHandler(SrsEncodeHandler handler) {
        SrsEncoder.getInstance().setEncodeHandler(handler);
        if (mFlvMuxer != null) {
            SrsEncoder.getInstance().setFlvMuxer(mFlvMuxer);
        }
    }
}

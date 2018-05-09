package com.believer.magicfilter.beautify.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 录制数据
 *
 * @author Created by jz on 2017/5/2 11:21
 */
public class RecordHelper extends Handler {

    private static final int MAX_CACHE_BUFFER_NUMBER = 24;

    private static final int PREVIEW_BITMAP = 0;

    private int[] mPixelData;
    private List<byte[]> mReusableBuffers;

    private List<PixelBuffer> mBuffers;
    private Thread mThread;

    private OnRecordListener mOnRecordListener;

    public RecordHelper() {
        super(Looper.getMainLooper());
        mReusableBuffers = Collections.synchronizedList(new ArrayList<byte[]>());

        mBuffers = Collections.synchronizedList(new ArrayList<PixelBuffer>());
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case PREVIEW_BITMAP:
                if (mOnRecordListener != null)
                    mOnRecordListener.onRecord((PixelBuffer) msg.obj);
                break;
        }
    }

    public void setOnRecordListener(OnRecordListener listener) {
        this.mOnRecordListener = listener;
    }

    public void onRecord(ByteBuffer buffer, int width, int height, int pixelStride, int rowStride, long timestamp) {

        if (mBuffers.size() >= MAX_CACHE_BUFFER_NUMBER) {
            return;
        }

        mBuffers.add(new PixelBuffer(buffer, width, height, pixelStride, rowStride, timestamp));

    }

    public void start() {
        if (mThread != null) {
            return;
        }
        mThread = new MyThread();
        mThread.start();
    }

    public void stop() {
        if (mThread == null) {
            return;
        }
        mThread.interrupt();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
    }

    private long start_time = 0;
    private long end_time = 0;

    private class MyThread extends Thread {//转换成Bitmap演示用效率低下，可以用libyuv代替

        @Override
        public void run() {
            while (!isInterrupted()) {
                if (mBuffers.isEmpty()) {
                    SystemClock.sleep(1);
                    continue;
                }

                PixelBuffer buffer = mBuffers.remove(0);

                byte[] data = new byte[buffer.getData().remaining()];
                buffer.getData().get(data, 0, data.length);
                buffer.getData().clear();
                sendMessage(obtainMessage(PREVIEW_BITMAP, buffer));
            }
            mBuffers.clear();
        }
    }

    private byte[] getBuffer(int length) {
        if (mReusableBuffers.isEmpty()) {
            return new byte[length];
        } else {
            return mReusableBuffers.remove(0);
        }
    }
}
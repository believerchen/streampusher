package com.believer.magicfilter.camera;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.believer.magicfilter.beautify.group.GLDefaultFilterGroup;
import com.believer.magicfilter.beautify.group.GLImageFilterGroup;
import com.believer.magicfilter.camera.base.BaseGlSurfaceView;
import com.believer.magicfilter.camera.interfaces.OnErrorListener;
import com.believer.magicfilter.camera.interfaces.OnFocusListener;
import com.believer.magicfilter.camera.interfaces.OnRecordListener;
import com.believer.magicfilter.camera.interfaces.OnSwitchCameraListener;
import com.believer.magicfilter.filter.base.MagicCameraInputFilter;
import com.believer.magicfilter.filter.base.MagicRecordFilter;
import com.believer.magicfilter.filter.helper.MagicFilterType;
import com.believer.magicfilter.utils.OpenGlUtils;
import com.believer.magicfilter.utils.TextureRotationUtil;
import com.believer.utilslibrary.FrameRateMeter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.abs;

/**
 * CameraGlSurfaceView
 *
 * @author Created by jz on 2017/5/2 11:21
 */
public class CameraGlSurfaceView extends BaseGlSurfaceView implements GLSurfaceView.Renderer,
        SensorHelper.OnSensorListener,
        Camera.AutoFocusCallback {

    public static final int RECORD_WIDTH = 480, RECORD_HEIGHT = 720;
    //    public static final int RECORD_WIDTH = 550, RECORD_HEIGHT = 940;
    private final FloatBuffer mRecordCubeBuffer;//顶点坐标
    private final FloatBuffer mRecordTextureBuffer;//纹理坐标

    private MagicCameraInputFilter mCameraInputFilter;//绘制到屏幕上
    private MagicRecordFilter mRecordFilter;//绘制到FBO
    private SurfaceTexture mSurfaceTexture;//surface纹理
    // 实时滤镜组
    private GLImageFilterGroup mRealTimeFilter;

    private CameraHelper mCameraHelper;

    private SensorHelper mSensorHelper;
    private int mOrientation;
    private boolean mIsInversion;

    private ThreadHelper mThreadHelper;

    private OnFocusListener mOnFocusListener;
    private OnRecordListener mOnRecordListener;
    //    private int fpsRate = 1000 / 15;
    // 计算帧率
    private FrameRateMeter mFrameRateMeter;
    private boolean mPushStream = false;

    public CameraGlSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCameraHelper = CameraHelper.getInstance();
        mSensorHelper = new SensorHelper(context, this);
        mThreadHelper = new ThreadHelper();

        mScaleType = CENTER_CROP;

        mRecordCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mRecordTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    public void setPushStream(boolean b) {
        mPushStream = b;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        if (mCameraInputFilter == null) {
            mCameraInputFilter = new MagicCameraInputFilter();
            mCameraInputFilter.init(getContext());
        }
        if (mRecordFilter == null) {
            mRecordFilter = new MagicRecordFilter();
            mRecordFilter.init(getContext());
            mRecordFilter.setRecordListener(mOnRecordListener);
        }

        if (mTextureId == OpenGlUtils.NO_TEXTURE) {
            mTextureId = OpenGlUtils.getExternalOESTextureID();
            if (mTextureId != OpenGlUtils.NO_TEXTURE) {
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        requestRender();
                    }
                });
            }
        }

        mCameraHelper.startPreview(mSurfaceTexture);
        mFrameRateMeter = new FrameRateMeter();
        initFilters();

    }

    @Override
    protected void setDefaultFilter() {
        setFilter(MagicFilterType.BEAUTY);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        CameraHelper.CameraItem info = mCameraHelper.getCameraAngleInfo();
        adjustSize(info.orientation, info.isFront, !info.isFront);

        //重新计算录制顶点、纹理坐标
        float[][] data = adjustSize(mRecordWidth, mRecordHeight, info.orientation,
                info.isFront, !info.isFront);
        mRecordCubeBuffer.clear();
        mRecordCubeBuffer.put(data[0]).position(0);
        mRecordTextureBuffer.clear();
        mRecordTextureBuffer.put(data[1]).position(0);

        if (mRealTimeFilter != null) {
            Log.i("9999999999999999","width:"+width+"height:"+height);
            mRealTimeFilter.onInputSizeChanged(width, height);
        }
    }

    private long start_time = 0;
    private long end_time = 0;

    /**
     * 初始化滤镜
     */
    private void initFilters() {
        mRealTimeFilter = new GLDefaultFilterGroup();
//        if (mRealTimeFilter != null) {
//            mRealTimeFilter.onInputSizeChanged(CameraGlSurfaceView.RECORD_WIDTH, CameraGlSurfaceView.RECORD_HEIGHT);
//        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (mSurfaceTexture == null)
            return;
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        mSurfaceTexture.getTransformMatrix(mtx);

        //先将纹理绘制到fbo同时过滤镜
        mFilter.setTextureTransformMatrix(mtx);
        int id = mFilter.onDrawToTexture(mTextureId);

        // 如果存在滤镜，则绘制滤镜
        if (mRealTimeFilter != null) {
            id = mRealTimeFilter.drawFrameBuffer(id);
        }
        //绘制到屏幕上
        mCameraInputFilter.onDrawFrame(id, mGLCubeBuffer, mGLTextureBuffer);
        // 计算绘制帧
        if (mFrameRateMeter != null) {
            mFrameRateMeter.drawFrameCount();
            mRecordFilter.onCurFps(mFrameRateMeter.getFPS());
        }


        if (!mPushStream)
            start_time = 0;
        //是否推流
        if (mPushStream) {
            if (start_time == 0)
                start_time = System.nanoTime();

            end_time = System.nanoTime();
             mRecordFilter.onDrawToFbo(id, mRecordCubeBuffer, mRecordTextureBuffer,
                     (end_time -start_time)/1000);
        }



    }

    @Override
    protected void onFilterChanged() {
        super.onFilterChanged();


        mCameraInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);

        //初始化fbo，pbo
        mRecordFilter.initFrameBuffer(mRecordWidth, mRecordHeight);
        mRecordFilter.initPixelBuffer(mRecordWidth, mRecordHeight);
        mRecordFilter.onInputSizeChanged(mRecordWidth, mRecordHeight);
        mRecordFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);

//        if (mRealTimeFilter != null) {
//            mRealTimeFilter.onInputSizeChanged(mRecordWidth, mRecordHeight);
//        }

    }

    @Override
    public void onSensor(int orientation, boolean isInversion) {
        mOrientation = orientation;
        mIsInversion = isInversion;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (mOnFocusListener != null)
            mOnFocusListener.onFocusEnd();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mSurfaceWidth > 0 && mSurfaceHeight > 0) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (mOnFocusListener != null)
                mOnFocusListener.onFocusStart(x, y);
            int centerX = (x - mSurfaceWidth / 2) * 1000 / (mSurfaceWidth / 2);
            int centerY = (y - mSurfaceHeight / 2) * 1000 / (mSurfaceHeight / 2);
            mCameraHelper.selectCameraFocus(new Rect(centerX - 100, centerY - 100, centerX + 100, centerY + 100), this);
        }
        return true;
    }

    //调整view大小
    private void review() {
        mPreviewWidth = mCameraHelper.getPreviewWidth();
        mPreviewHeight = mCameraHelper.getPreviewHeight();
        mRecordWidth = mCameraHelper.getRecordWidth();
        mRecordHeight = mCameraHelper.getRecordHeight();
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        mRecordFilter.startRecord();
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        mRecordFilter.stopRecord();
    }

    public boolean isRecording() {
        return mRecordFilter.isRecording();
    }

    /**
     * 恢复摄像头，对应Activity生命周期
     */
    public void resume() {
        boolean rel = mCameraHelper.openCamera();
        if (rel) {
            review();
            if (mSurfaceTexture != null)
                mCameraHelper.startPreview();
        } else {
            mThreadHelper.sendError("摄像头开启失败，请检查是否被占用！");
        }
    }

    /**
     * 暂停摄像头，对应Activity生命周期
     */
    public void pause() {
        mCameraHelper.stopCamera();
    }

    /**
     * 停止摄像头，对应Activity的onDestroy
     */
    public void stop() {
        mCameraHelper.stopCamera();
        mSensorHelper.release();

        mFilter.destroy();
        mCameraInputFilter.destroy();
        mRecordFilter.destroy();
    }

    /**
     * 切换前后摄像头
     */
    public void switchCamera(OnSwitchCameraListener l) {
        mThreadHelper.setOnSwitchCameraListener(l);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean rel = mCameraHelper.switchCamera();
                mThreadHelper.sendSwitchCamera(rel, "切换摄像头失败，请检查是否被占用！");
            }
        }).start();
    }

    /**
     * 获得摄像头
     */
    public CameraHelper getCamera() {
        return mCameraHelper;
    }

    /**
     * 获得摄像头数量
     */
    public int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

    /**
     * 是否横屏
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * 是否倒置
     */
    public boolean isInversion() {
        return mIsInversion;
    }

    /**
     * 当前是否前置摄像头
     */
    public boolean isFrontCamera() {
        return mCameraHelper.isFrontCamera();
    }

    /**
     * 返回录制宽度
     */
    public int getRecordWidth() {
        return mRecordWidth;
    }

    /**
     * 返回录制高度
     */
    public int getRecordHeight() {
        return mRecordHeight;
    }

    /**
     * 设置浏览回调
     *
     * @param l 回调
     */
    public void setOnRecordListener(OnRecordListener l) {
        if (mRecordFilter != null) {
            mRecordFilter.setRecordListener(l);
        }
        this.mOnRecordListener = l;
    }

    /**
     * 设置摄像头焦点回调
     *
     * @param l 回调
     */
    public void setOnFocusListener(OnFocusListener l) {
        this.mOnFocusListener = l;
    }


    /**
     * 设置错误回调
     *
     * @param l 回调
     */
    public void setOnErrorListener(OnErrorListener l) {
        mThreadHelper.setOnErrorListener(l);
    }

    public void setBeautylevel(float percent){
       if(mRealTimeFilter!=null){
           mRealTimeFilter.setBeautifyLevel(percent / 100.0f);
       }
    }
}

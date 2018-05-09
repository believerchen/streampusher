package com.believer.magicfilter.beautify.filter;

import android.graphics.PointF;
import android.opengl.GLES30;
import android.opengl.Matrix;


import com.believer.magicfilter.beautify.utils.GlUtil;
import com.believer.magicfilter.beautify.utils.TextureRotationUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * 基类滤镜
 * Created by cain on 2017/7/9.
 */

public class GLImageFilter {

    protected static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;                                   \n" +
            "attribute vec4 aPosition;                                  \n" +
            "attribute vec4 aTextureCoord;                              \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "void main() {                                              \n" +
            "    gl_Position = uMVPMatrix * aPosition;                  \n" +
            "    textureCoordinate = aTextureCoord.xy;                  \n" +
            "}                                                          \n";

    protected static final String FRAGMENT_SHADER_2D =
            "precision mediump float;                                   \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "uniform sampler2D inputTexture;                                \n" +
            "void main() {                                              \n" +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate); \n" +
            "}                                                          \n";

    private static final FloatBuffer FULL_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(TextureRotationUtils.CubeVertices);

    protected FloatBuffer mVertexArray = FULL_RECTANGLE_BUF;
    protected FloatBuffer mTexCoordArray = GlUtil.createFloatBuffer(TextureRotationUtils.TextureVertices);
    protected int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    protected int mVertexCount = TextureRotationUtils.CubeVertices.length / mCoordsPerVertex;

    protected int mProgramHandle;
    protected int muMVPMatrixLoc;
    protected int maPositionLoc;
    protected int maTextureCoordLoc;
    protected int mInputTextureLoc;

    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;
    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    // 变换矩阵
    protected float[] mMVPMatrix = new float[16];
    // 缩放矩阵
    protected float[] mTexMatrix = new float[16];

    private final LinkedList<Runnable> mRunOnDraw;

    ////
//    private IntBuffer mPboIds;
//    private int mPboSize;
//
//    private final int mPixelStride = 4;//RGBA 4字节
//    private int mRowStride;//对齐4字节
//    private int mPboIndex;
//    private int mPboNewIndex;
//
//    private boolean mRecordEnabled;
//    private boolean mInitRecord;
//
//    protected int mInputWidth;
//    protected int mInputHeight;

    public GLImageFilter() {
        this(VERTEX_SHADER, FRAGMENT_SHADER_2D);
    }

    public GLImageFilter(String vertexShader, String fragmentShader) {
        mRunOnDraw = new LinkedList<>();
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
        initHandle();
        initIdentityMatrix();
    }

    /**
     * 初始化句柄
     */
    protected void initHandle() {
        maPositionLoc = GLES30.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES30.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muMVPMatrixLoc = GLES30.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        mInputTextureLoc = GLES30.glGetUniformLocation(mProgramHandle, "inputTexture");
    }

    /**
     * Surface发生变化时调用
     * @param width
     * @param height
     */
    public void onInputSizeChanged(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     *  显示视图发生变化时调用
     * @param width
     * @param height
     */
    public void onDisplayChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        //初始化fbo，pbo 用于 h264 编码数据采集
        //initPixelBuffer(width, height);
        onInputSizeChanged(width, height);

    }

    /**
     * 绘制Frame
     * @param textureId
     */
    public boolean drawFrame(int textureId) {
        return drawFrame(textureId, mVertexArray, mTexCoordArray);
    }

    /**
     * 绘制Frame
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer,
                          FloatBuffer textureBuffer) {
        if (textureId == GlUtil.GL_NOT_INIT) {
            return false;
        }
        GLES30.glUseProgram(mProgramHandle);
        runPendingOnDrawTasks();
        // 绑定数据
        bindValue(textureId, vertexBuffer, textureBuffer);
        onDrawArraysBegin();
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mVertexCount);
        onDrawArraysAfter();
//        bindPixelBuffer();
        unBindValue();
        GLES30.glUseProgram(0);
        return true;
    }

//    private void bindPixelBuffer() {
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboIndex));
//        MagicJni.glReadPixels(0, 0, mRowStride / mPixelStride, mInputHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE);
//        if (mInitRecord) {//第一帧没有数据跳出
//            unbindPixelBuffer();
//            mInitRecord = false;
//            return;
//        }
//
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboNewIndex));
//
//        //glMapBufferRange会等待DMA传输完成，所以需要交替使用pbo
//        ByteBuffer byteBuffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
//        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
//        unbindPixelBuffer();
////        mRecordHelper.onRecord(byteBuffer, mInputWidth, mInputHeight, mPixelStride, mRowStride, mLastTimestamp);
//    }


    //解绑pbo
//    private void unbindPixelBuffer() {
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
//
//        mPboIndex = (mPboIndex + 1) % 2;
//        mPboNewIndex = (mPboNewIndex + 1) % 2;
//    }
//
//
//    //初始化2个pbo，交替使用
//    public void initPixelBuffer(int width, int height) {
//        if (mPboIds != null && (mInputWidth != width || mInputHeight != height)) {
//            destroyPixelBuffers();
//        }
//        if (mPboIds != null) {
//            return;
//        }
//
//        //OpenGLES默认应该是4字节对齐应，但是不知道为什么在索尼Z2上效率反而降低
//        //并且跟ImageReader最终计算出来的rowStride也和我这样计算出来的不一样，这里怀疑跟硬件和分辨率有关
//        //这里默认取得128的倍数，这样效率反而高，为什么？
//        final int align = 128;//128字节对齐
//        mRowStride = (width * mPixelStride + (align - 1)) & ~(align - 1);
//
//        mPboSize = mRowStride * height;
//
//        mPboIds = IntBuffer.allocate(2);
//        GLES30.glGenBuffers(2, mPboIds);
//
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(0));
//        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES30.GL_STATIC_READ);
//
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(1));
//        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES30.GL_STATIC_READ);
//
//        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
//    }

//    private void destroyPixelBuffers() {
//        if (mPboIds != null) {
//            GLES30.glDeleteBuffers(2, mPboIds);
//            mPboIds = null;
//        }
//    }

    public void startRecord() {
//        if (mRecordEnabled) {
//            return;
//        }
//        mRecordEnabled = true;
//        mInitRecord = true;
//        mPboIndex = 0;
//        mPboNewIndex = 1;
    }

    public void stopRecord() {
//        if (!mRecordEnabled) {
//            return;
//        }
//        mRecordEnabled = false;
    }
    /**
     * 绑定数据
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    protected void bindValue(int textureId, FloatBuffer vertexBuffer,
                             FloatBuffer textureBuffer) {
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(maPositionLoc, mCoordsPerVertex,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(maPositionLoc);

        textureBuffer.position(0);
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES30.GL_FLOAT, false, 0, textureBuffer);
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc);

        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mMVPMatrix, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(getTextureType(), textureId);
        GLES30.glUniform1i(mInputTextureLoc, 0);
    }

    /**
     * 解除绑定
     */
    protected void unBindValue() {
        GLES30.glDisableVertexAttribArray(maPositionLoc);
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES30.glBindTexture(getTextureType(), 0);
    }

    /**
     * 获取Texture类型
     * GLES30.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    /**
     * 调用drawArrays之前，方便添加其他属性
     */
    public void onDrawArraysBegin() {

    }

    /**
     * drawArrays调用之后，方便销毁其他属性
     */
    public void onDrawArraysAfter() {

    }

    /**
     * 释放资源
     */
    public void release() {
        GLES30.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * 初始化单位矩阵
     */
    public void initIdentityMatrix() {
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(mTexMatrix, 0);
    }

    /**
     * 设置变换矩阵
     * @param matrix
     */
    public void setMVPMatrix(float[] matrix) {
        if (!Arrays.equals(mMVPMatrix, matrix)) {
            mMVPMatrix = matrix;
        }
    }

    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES30.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
}

package com.believer.magicfilter.camera.interfaces;

import android.graphics.Bitmap;

import com.believer.magicfilter.camera.bean.PixelBuffer;

/**
 * 录制数据
 *
 * @author Created by jz on 2017/5/2 11:10
 */
public interface OnRecordListener {
    void onRecord(PixelBuffer pixelBuffer);
    void onCurFps(float fps);
}

package com.believer.streampusher.liveStream.encoder;

import android.util.Log;

import com.believer.magicfilter.camera.bean.PixelBuffer;
import com.believer.magicfilter.camera.interfaces.OnRecordListener;
import com.believer.mypublisher.oort.SrsPublisher;

/**
 * Created by believer on 2018/4/2.
 */

public class MyPublisher extends SrsPublisher implements OnRecordListener {
    private  int mWidth = 0;
    private  int mHeigth = 0;

    public MyPublisher() {
      // setListener();
    }
    public void setEncodeParam(int width,int height){
        mWidth = width;
        mHeigth = height;
        //        srsPublisher.setPreviewResolution(1280, 720);
//        srsPublisher.setOutputResolution(720, 1280);
        setPreviewResolution(width,height);
        setOutputResolution(width,height);
        //设置流程码率
        //srsPublisher.setVideoSmoothMode();
        //设置高清码率
        //setVideoHDMode();
        setVideoSmoothMode();
    }


    private  long start_time= 0;
    private  long end_time= 0;
    @Override
    public void onRecord(PixelBuffer pixelBuffer) {
        start_time = System.currentTimeMillis();
        Log.i("6666","===========================> 耗时："+(start_time-end_time));
        //ByteBuffer buf = ByteBuffer.wrap(bitmap2RGBA(bitmap));
        onGetRgbaFrame(pixelBuffer.getData(),pixelBuffer.getWidth(),pixelBuffer.getHeight(), pixelBuffer.getTimestamp());
        end_time = System.currentTimeMillis();
    }

    @Override
    public void onCurFps(float fps) {

    }

}

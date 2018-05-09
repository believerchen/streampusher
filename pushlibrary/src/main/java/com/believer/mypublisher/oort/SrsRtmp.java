package com.believer.mypublisher.oort;


import android.util.Log;

import static com.believer.mypublisher.oort.RtmpListener.MSG_RTMP_CONNECTED;
import static com.believer.mypublisher.oort.RtmpListener.MSG_RTMP_CONNECTING;
import static com.believer.mypublisher.oort.RtmpListener.MSG_RTMP_DISCONNECTED;
import static com.believer.mypublisher.oort.RtmpListener.MSG_RTMP_EXCEPTION;
import static com.believer.mypublisher.oort.RtmpListener.MSG_RTMP_NET_ERROR;

public class SrsRtmp {

    static {
        System.loadLibrary("pusher");
    }

    private static RtmpListener rtmpListener;
    private static SrsRtmp mInstance;

    private SrsRtmp() {

    }

    public static SrsRtmp getInstance() {
        if (mInstance == null) {
            mInstance = new SrsRtmp();
        }
        return mInstance;
    }

    public void setRtmpListener(RtmpListener l) {
        this.rtmpListener = l;
    }

    public native long open(String url, boolean isPublishMode);

    public native int read(long rtmpPointer, byte[] data, int offset, int size);

    public native int write(long rtmpPointer, byte[] data, int size, int type, int ts);

    public native int close(long rtmpPointer);

    public native String getIpAddr(long rtmpPointer);

    public static void RtmpNotify(int messageId, int value1, int value2) {
        Log.i("<======RtmpNotify=====>", 11 + "  messageId: " + messageId + "  value1: " + value1 + "  value2: " + value2);
        switch (messageId) {
            case 1://rtmp 正在建立连接
                rtmpListener.callRtmpState(MSG_RTMP_CONNECTING);
                if (value1 == 1) {
                    Log.i("<======RtmpNotify=====>", " rtmp 正在建立连接 ");
                } else if (value1 == 1) {
                    Log.i("<======RtmpNotify=====>", " rtmp 正在建立连接  初始化rtmp结构");
                } else if (value1 == 2) {
                    Log.i("<======RtmpNotify=====>", " rtmp 正在建立连接  打开rtmp写权限");
                } else if (value1 == 3) {
                    Log.i("<======RtmpNotify=====>", " rtmp 正在建立连接  连接rtmp服务器");
                } else if (value1 == 4) {
                    Log.i("<======RtmpNotify=====>", " rtmp 正在建立连接  创建rtmp stream channel");
                }
                break;
            case 2://rtmp 连接建立成功
                rtmpListener.callRtmpState(MSG_RTMP_CONNECTED);
                Log.i("<======RtmpNotify=====>", " rtmp 连接建立成功 ");
                break;
            case 3://rtmp 连接建立失败
                Log.i("<======RtmpNotify=====>", " rtmp 连接建立失败 ");
                rtmpListener.callRtmpState(MSG_RTMP_DISCONNECTED);
                break;
            case 4://rtmp 连接已经断开
                Log.i("<======RtmpNotify=====>", " rtmp 连接已经断开 ");
                rtmpListener.callRtmpState(MSG_RTMP_EXCEPTION);
                break;
            case 5://rtmp 发送数据成功
                Log.i("<======RtmpNotify=====>", " rtmp 发送数据成功  ts = " + value1 + "发送时间 " + value2 + " 毫秒");
                break;
            case 6://rtmp 发送数据失败
                Log.i("<======RtmpNotify=====>", " rtmp 发送数据失败  ts = " + value1 + "发送时间 " + value2 + " 毫秒");
                rtmpListener.callRtmpState(MSG_RTMP_NET_ERROR);
                break;
            default:
                break;
        }
    }
}

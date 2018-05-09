package com.believer.mypublisher.oort;

/**
 * Created by bunny on 2017/12/15.
 */

public interface RtmpListener {
    //rtmp连接中
    public static final int MSG_RTMP_CONNECTING = 0;
    //rtmp连接成功
    public static final int MSG_RTMP_CONNECTED = 1;
    //视频流推送中
    public static final int MSG_RTMP_VIDEO_STREAMING = 2;
    //音频流推送中
    public static final int MSG_RTMP_AUDIO_STREAMING = 3;
    //rtmp已停止
    public static final int MSG_RTMP_STOPPED = 4;
    //rtmp连接失败
    public static final int MSG_RTMP_DISCONNECTED = 5;
    //视频帧率切换
    public static final int MSG_RTMP_VIDEO_FPS_CHANGED = 6;
    //视频码率切换
    public static final int MSG_RTMP_VIDEO_BITRATE_CHANGED = 7;
    //音频码率切换
    public static final int MSG_RTMP_AUDIO_BITRATE_CHANGED = 8;
    //rtmp I/O错误
    public static final int MSG_RTMP_IO_EXCEPTION = 10;
    //rtmp中断
    public static final int MSG_RTMP_EXCEPTION = 11;
    //rtmp 正在重连
    public static final int MSG_RTMP_RECONNECTION = 12;
    //rtmp 网络质量差，发送当前帧失败
    public static final int MSG_RTMP_NET_ERROR = 13;

    public void callRtmpState(int msg);
}

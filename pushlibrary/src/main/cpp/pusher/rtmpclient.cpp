#include <malloc.h>
#include "rtmpclient.h"
#include "rtmp.h"
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/time.h>
/*
 * Class:     com_yaooort_oort_SrsRtmp
 * Method:    open
 * Signature: (Ljava/lang/String;)I
 */

JNIEnv *g_env;
jclass g_jcl;
jmethodID g_mid;
static RTMP *g_pRtmp = NULL;
static pthread_t g_phread = 0;

int open_rtmp(char *url);

int close_rtmp();

int write_rtmp(jbyte *buffer, int size, int type, int ts);

int read_rtmp(char *data, int size);

int close_rtmp();

char *get_rtmp_addr();

int get_rtmp_Connect();

static void *thread_func(void *vptr_args) {

    while (1) {
        sleep(2);
        LOGD("线程中  线程中 线程中  ！！！！！");
    }

    return NULL;
}
//std::list
//using namespce std;
//int streamPush(char *data,int size,int type,int ts){
//      //将数据放入队列中，队列 >100 则需要删除队列
//}
////数据解码线程
//static void *thread_func_encode(void *vptr_args) {
//
//    while (1) {
//         //判断是音频数据直接发送
//        // 视频数据处理后发送
//
//    }
//
//    return NULL;
//std::list
//using namespce std;
//int streamPush(char *data,int size,int type,int ts){
//      //将数据放入队列中，队列 >100 则需要删除队列
//}
////数据解码线程
//static void *thread_func_encode(void *vptr_args) {
//
//    while (1) {
//         //判断是音频数据直接发送
//        // 视频数据处理后发送
//
//    }
//
//    return NULL;
//}
long getCurrentTime()
{
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int open_rtmp( char *url) {

// if(g_phread==0){
//     if (pthread_create(&g_phread, NULL, thread_func, NULL) != 0){
//         LOGD("创建线程 失败   ！！！！！");
//     }else{
//         LOGD("创建线程 成功   ！！！！！");
//     }
// }
    //如果之前打开过先释放
    if (g_pRtmp != NULL) {
        close_rtmp();
    }

    g_pRtmp = RTMP_Alloc();
    if (g_pRtmp == NULL) {
        LOGD("RTMP_Alloc=NULL");
        return -1;
    }
    g_env->CallStaticVoidMethod( g_jcl, g_mid, 1,1,0);
    RTMP_Init(g_pRtmp);
    int ret = RTMP_SetupURL(g_pRtmp, url);

    if (!ret) {
        RTMP_Free(g_pRtmp);
        g_pRtmp = NULL;
        LOGD("RTMP_SetupURL=ret");
        return -2;
    }
    g_env->CallStaticVoidMethod( g_jcl, g_mid, 1, 2,0);
    RTMP_EnableWrite(g_pRtmp);

    g_env->CallStaticVoidMethod( g_jcl, g_mid, 1, 3,0);
    ret = RTMP_Connect(g_pRtmp, NULL);
    if (!ret) {
        RTMP_Free(g_pRtmp);
        g_pRtmp = NULL;
        LOGD("RTMP_Connect=ret %d", ret);
        return -3;
    }
    g_env->CallStaticVoidMethod( g_jcl, g_mid, 1,4,0);
    ret = RTMP_ConnectStream(g_pRtmp, 0);

    if (!ret) {
        // ret = RTMP_ConnectStream(g_pRtmp, 0);
        RTMP_Close(g_pRtmp);
        RTMP_Free(g_pRtmp);
        g_pRtmp = NULL;
        LOGD("RTMP_ConnectStream=ret %d", ret);
        return -4;
    }
    return ret;
}

int read_rtmp(char *data, int size) {
    if (g_pRtmp)
        return RTMP_Read(g_pRtmp, data, size);
    else
        return -99;
}

int close_rtmp() {
    if (g_pRtmp) {
        RTMP_Close((RTMP *) g_pRtmp);
        RTMP_Free((RTMP *) g_pRtmp);
        g_pRtmp = NULL;
    }
    return 1;
}

int write_rtmp(jbyte *buffer, int size, int type, int ts) {
    //发送数据前判断网络是否断开
    if (g_pRtmp == NULL ) {
        if(RTMP_IsConnected(g_pRtmp))
        {
            LOGD("JNI ----》 rtmp 连接已经断开");
        }
        return -99;
    }


    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, size);
    RTMPPacket_Reset(packet);

    if (type == RTMP_PACKET_TYPE_INFO) { // metadata
        packet->m_nChannel = 0x03;
    } else if (type == RTMP_PACKET_TYPE_VIDEO) { // video
        packet->m_nChannel = 0x04;
    } else if (type == RTMP_PACKET_TYPE_AUDIO) { //audio
        packet->m_nChannel = 0x05;
    } else {
        packet->m_nChannel = -1;
    }
    packet->m_nInfoField2 = g_pRtmp->m_stream_id;

    LOGD("write data type -------->: %d, ts %d", type, ts);
    memcpy(packet->m_body, buffer, size);
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_hasAbsTimestamp = FALSE;
    packet->m_nTimeStamp = ts;
    packet->m_packetType = type;
    packet->m_nBodySize = size;

    int ret = RTMP_SendPacket(g_pRtmp, packet, 0);

    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

char *get_rtmp_addr() {
    if (g_pRtmp) {
        return g_pRtmp->ipaddr;
    } else {
        return "";
    }
}

int get_rtmp_Connect() {
    if (g_pRtmp && RTMP_IsConnected(g_pRtmp)) {
        return 0;
    }
    return -1;

}


JNIEXPORT jlong JNICALL Java_com_believer_mypublisher_oort_SrsRtmp_open
        (JNIEnv *env, jobject thiz, jstring url_, jboolean isPublishMode) {
    g_env = env;
    LOGD("RTMP_OPENING  打开推流中:1111");
    g_jcl = g_env->FindClass( "com/believer/mypublisher/oort/SrsRtmp");
    g_mid = g_env->GetStaticMethodID(g_jcl, "RtmpNotify", "(III)V");
    g_env->CallStaticVoidMethod( g_jcl, g_mid, 1, 0,0);

     char *url = (char*)(env)->GetStringUTFChars( url_, 0);
    LOGD("RTMP_OPENING  打开推流中:%s", url);
    int ret = open_rtmp(url);
    if (ret > 0) {
        LOGD("RTMP_OPENING  打开推流中 成功！！！！！！:%s", url);
        g_env->CallStaticVoidMethod( g_jcl, g_mid, 2, 0,0);
    } else {
        LOGD("RTMP_OPENING  打开推流中 失败！！！！！！:%s", url);
        g_env->CallStaticVoidMethod( g_jcl, g_mid, 3, 0,0);
    }
    (env)->ReleaseStringUTFChars( url_, url);
    return ret;
}

/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    read
 * Signature: ([CI)I
 */
JNIEXPORT jint JNICALL Java_com_believer_mypublisher_oort_SrsRtmp_read
        (JNIEnv *env, jobject thiz, jlong rtmp, jbyteArray data_, jint offset, jint size) {

//    char *data = (char*)malloc(size * sizeof(char));
//    int readCount = read_rtmp(data, size);
//    if (readCount > 0) {
//        (env)->SetByteArrayRegion(data_, offset, readCount, data);  // copy
//    }
//    free(data);
    return 0;
}

/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    write
 * Signature: ([CI)I
 */
JNIEXPORT jint JNICALL Java_com_believer_mypublisher_oort_SrsRtmp_write
        (JNIEnv *env, jobject thiz, jlong rtmp, jbyteArray data, jint size, jint type, jint ts) {
    g_jcl = g_env->FindClass( "com/believer/mypublisher/oort/SrsRtmp");
    g_mid = g_env->GetStaticMethodID( g_jcl, "RtmpNotify", "(III)V");
    //每次推流前先判断rtmp是否断开
    LOGD("start write");
    jbyte *buffer = (env)->GetByteArrayElements(data, NULL);
    long beginTime = getCurrentTime();
    int ret = write_rtmp(buffer, size, type, ts);//jbyte *buffer, int size,int type,int ts
    int sendDalay = (int)(getCurrentTime()-beginTime);
    (env)->ReleaseByteArrayElements(data, buffer, 0);
    if (ret <= 0) {
        LOGD("end write error %d", sockerr);
        if (ret == -99)
            g_env->CallStaticVoidMethod( g_jcl, g_mid, 4, ts,sendDalay);
        else
            g_env->CallStaticVoidMethod( g_jcl, g_mid, 6, ts,sendDalay);
    }
    g_env->CallStaticVoidMethod( g_jcl, g_mid, 5, ts,sendDalay);
    LOGD("end write success");
    return ret;
}

/*
 * Class:     me_lake_librestreaming_rtmp_RtmpClient
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_believer_mypublisher_oort_SrsRtmp_close
        (JNIEnv *env, jlong rtmp, jobject thiz) {
    return close_rtmp();
}

JNIEXPORT jstring JNICALL Java_com_believer_mypublisher_oort_SrsRtmp_getIpAddr
        (JNIEnv *env, jobject thiz, jlong rtmp) {
    return (env)->NewStringUTF(get_rtmp_addr());
}
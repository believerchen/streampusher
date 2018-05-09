package com.believer.livePlayer.bean;

/**
 * ========================================
 * <p/>
 * 版 权：dou361.com 版权所有 （C） 2015
 * <p/>
 * 作 者：陈冠明
 * <p/>
 * 个人网站：http://www.dou361.com
 * <p/>
 * 版 本：1.0
 * <p/>
 * 创建日期：2016/8/30
 * <p/>
 * 描 述：
 * <p/>
 * <p/>
 * 修订历史：
 * <p/>
 * ========================================
 */
public class LiveBean {

    /**
     * nickname : 🐈这只野喵有毒🕳
     * livestarttime : 1473031828564
     * liveStream : http://pull.kktv8.com/livekktv/109204379.flv
     * portrait : /portrait/20160814/10/109204379_588711.jpg!256
     */

    private String nickname;
    private long livestarttime;
    private String liveStream;
    private String portrait;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getLivestarttime() {
        return livestarttime;
    }

    public void setLivestarttime(long livestarttime) {
        this.livestarttime = livestarttime;
    }

    public String getLiveStream() {
        return liveStream;
    }

    public void setLiveStream(String liveStream) {
        this.liveStream = liveStream;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }
}

package com.believer.livePlayer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;


import com.believer.livePlayer.bean.LiveBean;
import com.believer.livePlayer.listener.OnShowThumbnailListener;
import com.believer.livePlayer.module.ApiServiceUtils;
import com.believer.livePlayer.util.MediaUtils;
import com.believer.livePlayer.widget.PlayStateParams;
import com.believer.livePlayer.widget.PlayerView;
import com.believer.mypublisher.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class BaseLivePlayer extends Activity {

    private PlayerView player;
    private Context mContext;
    private View rootView;
    private List<LiveBean> list;
    private String play_url = "rtmp://10.72.1.196:1935/hls/123";
    private String title = "标题";
    private PowerManager.WakeLock wakeLock;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (list.size() > 1) {
                play_url = list.get(1).getLiveStream();
                title = list.get(1).getNickname();
            }
            player.setPlaySource(play_url)
                    .startPlay();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_live_player);
        this.mContext = this;
        rootView = getLayoutInflater().from(this).inflate(R.layout.simple_player_view_player, null);
        setContentView(rootView);

        Intent intent = getIntent();
        String push_url = intent.getStringExtra("play_url");

        if(!push_url.isEmpty())
            play_url = push_url;

        /**常亮*/
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "liveTAG");
        wakeLock.acquire();

        player = new PlayerView(this, rootView);
        player.setTitle(title);
        player.setScaleType(PlayStateParams.fillparent);
        player.hideMenu(true);
        player.hideSteam(true);
        player.setForbidDoulbeUp(true);
        player.hideCenterPlayer(true);
        player.hideControlPanl(true);

        player.showThumbnail(new OnShowThumbnailListener() {
            @Override
            public void onShowThumbnail(ImageView ivThumbnail) {
                Glide.with(mContext)
                        .load("http://pic2.nipic.com/20090413/406638_125424003_2.jpg")
                        .placeholder(R.color.cl_default)
                        .error(R.color.cl_error)
                        .into(ivThumbnail);
            }
        });
        new Thread() {
            @Override
            public void run() {
                //这里多有得罪啦，网上找的直播地址，如有不妥之处，可联系删除
                list = ApiServiceUtils.getLiveList();
                mHandler.sendEmptyMessage(0);
            }
        }.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.onPause();
        }
        MediaUtils.muteAudioFocus(mContext, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onResume();
        }
        MediaUtils.muteAudioFocus(mContext, false);
        if (wakeLock != null) {
            wakeLock.acquire();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.onDestroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (player != null) {
            player.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.onBackPressed()) {
            return;
        }
        super.onBackPressed();
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

}

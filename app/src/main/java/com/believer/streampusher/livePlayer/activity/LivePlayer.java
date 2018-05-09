package com.believer.streampusher.livePlayer.activity;


import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;

import com.believer.livePlayer.activity.PlayerActivity;


public class LivePlayer extends PlayerActivity {
    private PowerManager.WakeLock wakeLock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**常亮*/
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "liveTAG");
        wakeLock.acquire();
    }

}

package com.believer.streampusher;


import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.believer.streampusher.livePlayer.activity.LivePlayer;
import com.believer.streampusher.liveStream.activity.RecordActivity;
import com.believer.streampusher.liveStream.config.ConfigUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.Properties;

/**
 * 主界面
 *
 * @author Created by jz on 2017/5/2 17:01
 */
public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private Button mButton_push;
    private Button mButton_liveplayer;

    private EditText mEdit_push;
    private EditText mEdit_play;

    private String mPush_url = "rtmp://10.1.100.110:7935/iev/123";
    private String mPlay_url = "rtmp://10.1.100.110:7935/iev/123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Properties prop2 = ConfigUtil.getinstance().loadConfig(this, "/sdcard/config.dat");
        String push_url = (String) prop2.get("Push_url");
        String play_url = (String) prop2.get("Play_url");

        if (push_url!=null&&!push_url.isEmpty())
            mPush_url = push_url;

        if (play_url!=null&&!play_url.isEmpty())
            mPlay_url = play_url;

        findView();
        initView();
        setListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Properties prop = new Properties();
        prop.put("Push_url", mPush_url);
        prop.put("Play_url", mPlay_url);
        ConfigUtil.getinstance().saveConfig(this, "/sdcard/config.dat", prop);
    }

    private void findView() {
        mToolbar = findViewById(R.id.toolbar);
        mButton_push = findViewById(R.id.button_push);

        mEdit_push = findViewById(R.id.edit_push_url);
        mEdit_push.setText(mPush_url);
        mButton_liveplayer = findViewById(R.id.button_liveplayer);

        mEdit_play = findViewById(R.id.edit_play_url);
        mEdit_play.setText(mPlay_url);
    }

    private void initView() {
        setSupportActionBar(mToolbar);
    }

    private void setListener() {

        mButton_push.setOnClickListener(v -> {
            new RxPermissions(this)
                    .request(Manifest.permission.CAMERA)
                    .subscribe(granted -> {
                        if (granted) {
                            if (!mEdit_push.getText().toString().trim().isEmpty()) {
                                if (!mEdit_push.getText().toString().trim().equals(mPush_url.trim())) {
                                    mPush_url = mEdit_push.getText().toString().trim();
                                }

                                Intent intent = new Intent(this, RecordActivity.class);
                                intent.putExtra("push_url", mPush_url);
                                startActivity(intent);
                            }

                        } else {
                            Toast.makeText(MainActivity.this, "录制权限被拒绝！", Toast.LENGTH_LONG).show();
                        }
                    }, Throwable::printStackTrace);
        });


        mButton_liveplayer.setOnClickListener(v -> {
            if (!mEdit_play.getText().toString().trim().isEmpty()) {
                if (!mEdit_play.getText().toString().trim().equals(mPlay_url.trim())) {
                    mPlay_url = mEdit_play.getText().toString().trim();
                }

                Intent intent = new Intent(this, LivePlayer.class);
                intent.putExtra("play_url", mPlay_url);
                startActivity(intent);
            }
        });
    }
}

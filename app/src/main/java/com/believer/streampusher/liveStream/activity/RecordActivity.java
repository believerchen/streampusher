package com.believer.streampusher.liveStream.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.believer.magicfilter.camera.CameraGlSurfaceView;
import com.believer.magicfilter.camera.bean.PixelBuffer;
import com.believer.magicfilter.camera.interfaces.OnRecordListener;
import com.believer.magicfilter.filter.helper.MagicFilterType;
import com.believer.mypublisher.oort.Bitmap_Event;
import com.believer.mypublisher.oort.SrsRtmp;
import com.believer.streampusher.R;
import com.believer.mypublisher.oort.RtmpListener;
import com.believer.mypublisher.oort.SrsEncodeHandler;
import com.believer.streampusher.liveStream.encoder.MyPublisher;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 录制界面
 *
 * @author Created by jz on 2018/1/9 14:42
 */
public class RecordActivity extends AppCompatActivity implements OnRecordListener, SrsEncodeHandler.SrsEncodeListener, SeekBar.OnSeekBarChangeListener {

//    private Toolbar mToolbar;
    private CameraGlSurfaceView mGLSurfaceView;
    private ImageView mImgView;
    //private FloatingActionButton mRecordView;
    private FloatingActionButton mSwitchCameraView;
    private Button mBtnStreaming;
    //直播推流
    private MyPublisher mMyPublisher;
    private boolean mIsPublishing = false;
    //private String rtmpUrl = "rtmp://10.1.100.110:2935/proxypublish/16563555216751293|BA96A92762B92B63E683B2372FAF0E13";
    private String rtmpUrl = "rtmp://10.72.1.196:1935/live/888";
    private boolean mIsEncoding;

    // Seekbar的最大值
    private static final int SeekBarMax = 100;
    private SeekBar sb_beauty;

    private EditText edit_url;
    private PowerManager.WakeLock wakeLock;
    private final Object syncpusher = new Object();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        /**常亮*/
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "liveTAG");
        wakeLock.acquire();
        Intent intent = getIntent();
        String push_url = intent.getStringExtra("push_url");

        if (!push_url.isEmpty())
            rtmpUrl = push_url;

        findView();
        initView();
        setListener();
        initLiveStream();
        EventBus.getDefault().register(this);
    }

    private void initFilter() {
        setDefaultFilter();
    }

    private void setDefaultFilter() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.setFilter(MagicFilterType.BEAUTY);
        }
    }

    private void initLiveStream() {

        mBtnStreaming = this.findViewById(R.id.start_push);
        mBtnStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBtnStreaming.getText().equals("推流")) {
                    startPush();
                } else if (mBtnStreaming.getText().equals("结束")) {
                    stopPush();
                }
            }
        });

        mMyPublisher = new MyPublisher();
        RtmpListener rtmpListener = new RtmpListener() {
            @Override
            public void callRtmpState(int msg) {
                switch (msg) {
                    case MSG_RTMP_CONNECTING:
                        Log.e("TTTT", "rtmp连接中");
                        break;
                    case MSG_RTMP_CONNECTED:
                        Log.e("TTTT", "rtmp连接成功");
                        break;
                    case MSG_RTMP_VIDEO_STREAMING:
                        Log.e("TTTT", "视频流推送中");
                        break;
                    case MSG_RTMP_AUDIO_STREAMING:
                        Log.e("TTTT", "音频流推送中");
                        break;
                    case MSG_RTMP_STOPPED:
                        Log.e("TTTT", "rtmp已停止");
                        stopPush();
                        break;
                    case MSG_RTMP_DISCONNECTED:
                        Log.e("TTTT", "rtmp连接失败");
                        stopPush();
                        break;
                    case MSG_RTMP_VIDEO_FPS_CHANGED:
                        Log.e("TTTT", "视频帧率切换");
                        break;
                    case MSG_RTMP_VIDEO_BITRATE_CHANGED:
                        Log.e("TTTT", "视频码率切换");
                        break;
                    case MSG_RTMP_AUDIO_BITRATE_CHANGED:
                        Log.e("TTTT", "音频码率切换");
                        break;
                    case MSG_RTMP_IO_EXCEPTION:
                        Log.e("TTTT", "rtmp I/O错误");
                        break;
                    case MSG_RTMP_EXCEPTION:
                        Log.e("TTTT", "rtmp中断");
                        stopPush();
                        break;
                    case MSG_RTMP_RECONNECTION:
                        Log.e("TTTT", "rtmp 正在重连");
                        break;
                    case MSG_RTMP_NET_ERROR:
                        Log.e("TTTT", "rtmp 网络质量差，发送当前帧失败");
                        break;
                }
            }
        };

        mMyPublisher.setRtmpHandler(rtmpListener);
        SrsRtmp.getInstance().setRtmpListener(rtmpListener);

        mMyPublisher.setEncodeHandler(new SrsEncodeHandler(this));
//        mMyPublisher.setRecordHandler(new SrsRecordHandler(null));

        mMyPublisher.setEncodeParam(mGLSurfaceView.RECORD_WIDTH, mGLSurfaceView.RECORD_HEIGHT);


    }

    private void stopPush() {
        synchronized (syncpusher) {
            if (mIsPublishing) {
                mIsPublishing = false;
                mMyPublisher.stopPublish();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopRecord();
                        mBtnStreaming.setText("推流");
                    }
                });
            }
        }
    }

    private void startPush() {
        synchronized (syncpusher) {
            String curUrl = edit_url.getText().toString().trim();
            if (!(rtmpUrl.trim().equals(curUrl))) {
                rtmpUrl = curUrl;
            }
            if (!mIsPublishing) {
                startRecord();
                mIsPublishing = true;
                mMyPublisher.startPublish(rtmpUrl);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnStreaming.setText("结束");
                    }
                });
            }
        }
    }

    private void findView() {
//        mToolbar = findViewById(R.id.toolbar);
        mGLSurfaceView = findViewById(R.id.surface);
        mImgView = findViewById(R.id.img);
        mSwitchCameraView = findViewById(R.id.switchCamera);
    }

    private void initView() {
//        setSupportActionBar(mToolbar);
        Spinner spinnerFps = (Spinner) findViewById(R.id.spinner_fps);
        spinnerFps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                String[] languages = getResources().getStringArray(R.array.fps);
                Toast.makeText(RecordActivity.this, "你点击的是 " + pos + ": " + languages[pos], 2000).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        Spinner spinnerRates = (Spinner) findViewById(R.id.spinner_rates);
        spinnerRates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                mMyPublisher.setEncodelevel(pos);
                String[] languages = getResources().getStringArray(R.array.rates);
                Toast.makeText(RecordActivity.this, "你点击的是:" + languages[pos], 2000).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        spinnerRates.setSelection(1);//默认选择500k码率


        Spinner spinnerEncodes = (Spinner) findViewById(R.id.spinner_endcodes);
        spinnerEncodes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                mMyPublisher.setEncodes(pos);
                String[] languages = getResources().getStringArray(R.array.encodes);
                Toast.makeText(RecordActivity.this, "你点击的是:" + languages[pos], 2000).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        sb_beauty = (SeekBar) findViewById(R.id.seek_beauty);
        sb_beauty.setOnSeekBarChangeListener(this);
        sb_beauty.setProgress(SeekBarMax / 2);


        edit_url = (EditText) findViewById(R.id.edit_url);
        edit_url.setText(rtmpUrl);


        //红润
        CheckBox btn_radio = (CheckBox)findViewById(R.id.checkBox_red);
        btn_radio.setChecked(true);
        btn_radio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn_radio.isChecked()) {
                    mGLSurfaceView.setFilter(MagicFilterType.BEAUTY);
                } else if (!btn_radio.isChecked()) {
                    mGLSurfaceView.setFilter(MagicFilterType.NONE);
                }
            }
        });
    }

    private void setListener() {
        mSwitchCameraView.setOnClickListener(v -> {
            mGLSurfaceView.switchCamera((isSuccess, msg) -> {
                if (!isSuccess) {
                    Snackbar.make(mSwitchCameraView, msg, Snackbar.LENGTH_LONG).show();
                }
            });
        });
        mGLSurfaceView.setOnErrorListener(msg -> {
        });
        mGLSurfaceView.setOnRecordListener(this);
    }

    private void startRecord() {
        if (mGLSurfaceView.isRecording()) {
            return;
        }

        mGLSurfaceView.startRecord();
        mGLSurfaceView.setPushStream(true);
    }

    private void stopRecord() {
        if (!mGLSurfaceView.isRecording())
            return;
        mGLSurfaceView.setPushStream(false);
        mGLSurfaceView.stopRecord();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_none:
                mGLSurfaceView.setFilter(MagicFilterType.NONE);
                return true;
            case R.id.action_beauty:
                mGLSurfaceView.setFilter(MagicFilterType.BEAUTY);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.stop();
    }


    @Override
    public void onRecord(PixelBuffer pixelBuffer) {
        if (mIsPublishing)
            mMyPublisher.onRecord(pixelBuffer);
    }

    @Override
    public void onCurFps(float fps) {
        Log.i("6666666", "fps ; " + fps);
        TextView curFps = (TextView) findViewById(R.id.text_fps);
        curFps.setText("当前fps:" + fps);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateMessage(Bitmap_Event message) {
        /**收到服务器主动断开 或者 私聊信息 播放命令 不做处理*/
        if (message.getType() == 10086) {
            mImgView.setImageBitmap(message.getMsg());
        }
    }

    @Override
    public void onNetworkWeak() {

    }

    @Override
    public void onNetworkResume() {

    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        Log.i("000000000000000", "unknown msg" + e);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.seek_beauty) {
            mGLSurfaceView.setBeautylevel(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}

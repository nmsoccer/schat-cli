package com.app.schat;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class AudioActivity extends AppCompatActivity implements  MediaRecorder.OnInfoListener {
    private MediaRecorder mMediaRecorder = null;
    private String audio_file_name = "";
    private String playing_file_name = "";
    private boolean recording = false;
    private Button btn_ok;
    private Button btn_save;
    private ProgressBar pb_seconds;
    //private total_seconds = 0;
    private MediaPlayer mPlayer;
    private TextView tv_title;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        //permission
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }

        //widget
        tv_title = this.findViewById(R.id.tv_audio_title);
        tv_title.setText("最长录音时间:" + AppConfig.MAX_AUDIO_SECONDS + "秒");
        btn_ok = this.findViewById(R.id.btn_audio_start);
        btn_save = this.findViewById(R.id.btn_audio_save);
        pb_seconds = this.findViewById(R.id.pb_audio_progress);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_audio_start:
                if(!recording) {
                    recording = true;
                    pb_seconds.setVisibility(View.VISIBLE);
                    TickSeconds();
                    btn_ok.setText("停止");
                    btn_ok.setBackground(getDrawable(R.drawable.select_orange_solid_rect));
                    startRecord();
                } else {
                    recording = false;
                    pb_seconds.setVisibility(View.GONE);
                    btn_ok.setText("开始");
                    btn_ok.setBackground(getDrawable(R.drawable.select_blue_solid_rect));
                    stopRecord();
                }
                break;
            case R.id.btn_audio_play:
                playRecord(audio_file_name);
                break;
            case R.id.btn_audio_save:
                saveRecord();
                break;
            default:
                break;
        }

    }

    private void saveRecord() {
        AppConfig.RecordAudioFile = audio_file_name;
        finish();
    }


    private void playRecord(String audio_file_name) {
        final String log_label = "playRecord";
        if(mPlayer != null && mPlayer.isPlaying()) {
            if(TextUtils.equals(audio_file_name , playing_file_name)) {
                Log.d(log_label , "in playing file:" + playing_file_name);
                mPlayer.stop();
                return;
            } else {
                Log.d(log_label , "will play new file:" + audio_file_name + " old:" + playing_file_name);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
        }

        //play file
        final String file_path = AppConfig.GetTempMiscFilePath(audio_file_name);
        if(TextUtils.isEmpty(file_path)) {
            Log.e(log_label , "empty file");
            return;
        }

        Log.d(log_label , "playing file_path:" + file_path);
        playing_file_name = audio_file_name;
        //player
        if(mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(log_label , "complete:" + file_path);
                    //播放完毕
                    mPlayer.release();
                    mPlayer = null;
                }
            });

        }
        try {
            mPlayer.setDataSource(file_path);
            mPlayer.setLooping(false);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void startRecord() {
        String log_label = "startRecord";
        Log.d(log_label , "starts...");
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();


        try {
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            audio_file_name = "audio_" + AppConfig.CurrentUnixTimeMilli() + ".amr";
            /*
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            audio_file_name = "audio_" + AppConfig.CurrentUnixTimeMilli() + ".3gp";
             */
            String filePath = AppConfig.TEMP_MISC_DIR_PATH + "/" + audio_file_name;
            //File local_file = new File(filePath);
            //local_file.createNewFile();
            /* ③准备 */
            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.setMaxDuration(AppConfig.MAX_AUDIO_SECONDS*1000); //max 1min
            mMediaRecorder.setOnInfoListener(this);
            mMediaRecorder.prepare();

            /* ④开始 */
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void TickSeconds() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String log_label = "TickSeconds";
                //wait and tick
                int v = 0;
                while (v<=AppConfig.MAX_AUDIO_SECONDS && recording) {
                    try {
                        pb_seconds.setProgress(v*100/AppConfig.MAX_AUDIO_SECONDS);
                        Thread.sleep(1000); //sleep
                        v++;
                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }

            }
        }).start();
    }


    public void voidcancelRecord(){

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder=null;

        AppConfig.DelTempMiscFile(audio_file_name);
    }

    private void stopRecord() {
        String log_label = "stopRecord";
        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        } catch (RuntimeException e) {
            e.printStackTrace();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            AppConfig.DelTempMiscFile(audio_file_name);
            return;
        }

        //next
        /*
        byte[] data = AppConfig.ReadTempMiscFile(audio_file_name);
        String b64 = Base64.encodeToString(data , Base64.DEFAULT);
        Log.d(log_label , "saved file:" + audio_file_name + " size:" + data.length + " b64:" + b64.length());
        */
        btn_save.setVisibility(View.VISIBLE);
        //remove
        //AppConfig.DelTempMiscFile(audio_file_name);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int i1) {
        String log_label = "media.info";
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                Log.d(log_label , "timeout!");
                recording = false;
                pb_seconds.setVisibility(View.GONE);
                btn_ok.setText("开始");
                btn_ok.setBackground(getBaseContext().getDrawable(R.drawable.select_blue_solid_rect));
                stopRecord();
                break;
            default:
                break;
        }
    }
}
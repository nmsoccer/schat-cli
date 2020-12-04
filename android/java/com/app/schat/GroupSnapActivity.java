package com.app.schat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GroupSnapActivity extends AppCompatActivity {
    private TextView tv_chat_name = null;
    private TextView tv_grp_id = null;
    private TextView tv_mem_count = null;
    private TextView tv_desc = null;
    private ImageView iv_head = null;

    private String group_name = null;
    //private String pass = "";
    private String msg = "";
    private long grp_id = 0;
    private static Handler handler;

    private String log_label = "group_snap";
    private EditText et_pass;
    private EditText et_msg;
    private Bitmap bmp_img = null;
    private ProgressDialog progress_dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_snap);
        //Login
        if(!AppConfig.IsLogin()) {
            AppConfig.PrintInfo(this , "您未登录");
            finish();
            return;
        }

        //Widget
        tv_chat_name = (TextView)this.findViewById(R.id.grp_snap_name);
        tv_grp_id = (TextView)this.findViewById(R.id.grp_snap_grp_id);
        tv_mem_count = (TextView)this.findViewById(R.id.grp_snap_mem_count);
        tv_desc = (TextView)this.findViewById(R.id.grp_snap_desc);
        iv_head = (ImageView)this.findViewById(R.id.grp_snap_img);

        et_pass = (EditText)this.findViewById(R.id.grp_snap_password);
        et_msg = (EditText)this.findViewById(R.id.grp_snap_msg);

        /***RECVER*/
        Intent intent = this.getIntent();
        group_name = intent.getStringExtra("chat_name");
        grp_id = Long.parseLong(intent.getStringExtra("grp_id"));
        if(group_name==null || group_name.length()<=0 || grp_id<=0)
        {
            AppConfig.PrintInfo(getBaseContext(), "对不起，群组信息错误！");
            finish();
            return;
        }
        tv_chat_name.setText(group_name);
        tv_grp_id.setText(Long.toString(grp_id));
        tv_mem_count.setText(intent.getStringExtra("mem_count"));
        tv_desc.setText(intent.getStringExtra("desc"));
        String head_url = intent.getStringExtra("head_url");
        if(head_url != null && head_url.length()>0) {
            LoadHeadImg(head_url);
        } else
            iv_head.setBackgroundResource(R.drawable.group_main_white);



        //handler
        handler = new Handler();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.only_return, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return MenuChoice(item);
    }


    private boolean MenuChoice(MenuItem item)
    {

        switch(item.getItemId())
        {
            case android.R.id.home:
            case R.id.only_return_return:
                finish();
                overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
                break;
        }

        return true;
    }

    public void onBtnClick(View v)
    {
        //pass = et_pass.getText().toString();
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupSnapActivity.this);
        LayoutInflater inflater = LayoutInflater.from(GroupSnapActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("确认申请吗");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                msg = et_msg.getText().toString();
                TrytoApply();
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
            }
        });

    }

    private void ReqDone() {

        switch(AppConfig.apply_result) {
            case CSProto.APPLY_GRP_DONE:
                AppConfig.PrintInfo(getBaseContext() , "申请已发送");
                finish();
                return;
            case CSProto.APPLY_GRP_NONE:
                AppConfig.PrintInfo(getBaseContext(), "该群组已解散");
                break;
            case CSProto.APPLY_GRP_PASS:
                AppConfig.PrintInfo(getBaseContext(), "密码错误");
                break;
            case CSProto.APPLY_GRP_EXIST:
                AppConfig.PrintInfo(getBaseContext(), "您已加入");
                break;
            case CSProto.APPLY_GRP_ERR:
                AppConfig.PrintInfo(this , "系统错误");
                break;
            default:
                AppConfig.PrintInfo(getBaseContext(), "系统正忙");
                break;
        }

    }

    //@from:0 quit group; 1 kick member
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(GroupSnapActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("正在提交");
        progress_dialog.setIndeterminate(false);
        progress_dialog.show();

        WindowManager.LayoutParams lp = progress_dialog.getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        Window win = progress_dialog.getWindow();
        win.setAttributes(lp);

        // 只呈现1s
        new Thread(new Runnable() {

            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                int progress = 0;

                while (System.currentTimeMillis() - startTime < 1000) {
                    try {
                        progress += 10;
                        progress_dialog.setProgress(progress);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        progress_dialog.dismiss();
                    }
                }

                progress_dialog.dismiss();
                handler.post(new Runnable() {

                    @Override
                    public void run()
                    {
                        ReqDone();
                    }
                });
            }
        }).start();
    }



    private void TrytoApply() {
        AppConfig.apply_result = CSProto.APPLY_GRP_CLEAR;
        String req = CSProto.CSGroupApplyReq(grp_id , group_name , "" , msg);
        AppConfig.SendMsg(req);
        showProgressDialog(0);
    }


    private void LoadHeadImg(String head_url) {
        String log_label = "LoadHeadImg";
        Log.d(log_label , "head_url:" + head_url);
        //check head url
        if(head_url==null || head_url.length()<=0) {
            Log.d(log_label , "head_url empty!");
            return;
        }

        //convert file name
        String head_file_name = AppConfig.Url2RealFileName(head_url);
        if(head_file_name==null || head_file_name.length()<=0) {
            Log.e(log_label , "convert real file failed! url:" + head_url);
            return;
        }


        //get query
        String head_query = AppConfig.ParseServerUrl(head_url);
        if(head_query==null || head_query.length()<=0) {
            Log.e(log_label , "parse head_url failed!");
            return;
        }
        Log.d(log_label , "head_query:" + head_query + " head_file_name:" + head_file_name);

        //load
        new LoadFileTask().execute(head_query , head_file_name);
    }


    /*
     * HTTPTASK
     */
    private class LoadFileTask extends AsyncTask<String, Void, Bitmap>
    {
        private String file_name = "";
        private boolean from_local = false;

        @Override
        protected Bitmap doInBackground(String... params)
        {
            InputStream input_stream = null;
            byte[] data = null;
            file_name = params[1];
            Bitmap bmp_img_tmp = null;

            try
            {
                /*load from local*/
                bmp_img_tmp = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_GRP_HEAD , 0 , file_name);
                if(bmp_img_tmp != null) {
                    Log.d(log_label , "load from local! file:" + file_name);
                    from_local = true;
                    return bmp_img_tmp;
                }

                /*load from net*/
                Log.d(log_label , "load from net! file:" + file_name);
                /*open connection*/
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10 * 1000);
                conn.setAllowUserInteraction(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    input_stream = conn.getInputStream();
                    data = AppConfig.ReadInput2Bytes(input_stream);
                    bmp_img_tmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                }

            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return bmp_img_tmp;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(Bitmap bmp_img_loaded)
        {
//			progress_dialog.cancel();
            if(bmp_img_loaded == null)
                return;

            /*Parse BITMAP*/
            try
            {
                if(bmp_img_loaded != null)
                {
                    if(!from_local) { //from net will save to local
                        Log.i("download", "download img done");
                        AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_GRP_HEAD, grp_id, bmp_img_loaded, file_name);
                    }
                    bmp_img = bmp_img_loaded;
                    //iv is round
                    iv_head.setImageDrawable(null); //clear old drawable
                    Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_img_loaded);
                    iv_head.setImageBitmap(bmp_tmp);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.grp_snap_img:
                showFullImg();
                break;
            default:
                break;
        }
    }

    private void showFullImg() {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;

        if(bmp_img == null)
        {
            return;
        }

        //创建对话框
        img_dialog = new AlertDialog.Builder(GroupSnapActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(GroupSnapActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);
        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }

}
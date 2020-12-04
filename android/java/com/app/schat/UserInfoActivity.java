package com.app.schat;

import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class UserInfoActivity extends AppCompatActivity {
    private final int LOAD_FROM_CREATE = 0;
    private final int LOAD_FROM_SEARCH = 1;
    private final int LOAD_FROM_INVITE = 2;

    private long uid = 0;
    private String head_file_name = "";
    //widget
    private ImageView iv_user_img;
    private TextView tv_user_name;
    private TextView tv_user_role_name;
    private TextView tv_user_uid;
    private TextView tv_user_sex;
    private TextView tv_user_addr;
    private TextView tv_user_desc;
    private Button bt_user_join;
    private ProgressDialog progress_dialog;
    private static Handler handler;
    private Bitmap bmp_img = null;
    private int from_manage = 0;
    private int is_member = 0;
    private long invited_grp_id = 0;
    private String invited_grp_name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        //Login
        if(!AppConfig.IsLogin()) {
            AppConfig.PrintInfo(this , "您未登录");
            finish();
            return;
        }

        //Intent
        Intent intent = this.getIntent();
        uid = Long.parseLong(intent.getStringExtra("uid"));
        if(uid == 0) {
            AppConfig.PrintInfo(this , "信息错误");
            finish();
            return;
        }
        from_manage = intent.getIntExtra("from_manage" , 0);
        is_member = intent.getIntExtra("is_member" , 0);
        invited_grp_id = intent.getLongExtra("invited_grp_id" , 0);
        invited_grp_name = intent.getStringExtra("invited_grp_name");

        handler = new Handler();
        //widget
        iv_user_img = (ImageView)findViewById(R.id.user_info_img);
        tv_user_name = (TextView)findViewById(R.id.user_info_name);
        tv_user_uid = (TextView)findViewById(R.id.user_info_uid);
        tv_user_sex = (TextView)findViewById(R.id.user_info_sex);
        tv_user_role_name = (TextView)findViewById(R.id.user_info_role_name);
        tv_user_addr = (TextView)findViewById(R.id.user_info_addr);
        tv_user_desc = (TextView)findViewById(R.id.user_info_desc);
        bt_user_join = (Button)findViewById(R.id.bt_user_info_join);

        //load info
        if(LoadUserInfo(uid , LOAD_FROM_CREATE)) {
            return;
        }

        //Qury From Server
        TrytoQuery();
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

    private boolean LoadUserInfo(long uid , int from) {
        ArrayList<Long> uids = new ArrayList<>();
        uids.add(uid);
        HashMap<Long, UserProfile> profiles = UserInfo.GetUserProfiles(uids);
        if(profiles==null || !profiles.containsKey(uid)) {
            if(from == LOAD_FROM_SEARCH) {
                AppConfig.PrintInfo(this , "该用户不存在");
            }
            return false; //not exist local
        }

        UserProfile profile = profiles.get(uid);
        //local load
        tv_user_name.setText(profile.name);
        tv_user_role_name.setText(profile.name);
        tv_user_uid.setText(Long.toString(uid));
        tv_user_addr.setText(profile.addr);
        tv_user_desc.setText(profile.user_desc);
        if(profile.sex == AppConfig.SEX_MALE) {
            tv_user_sex.setText("男");
        } else {
            tv_user_sex.setText("女");
        }
        if(from_manage == 1 && is_member == 0) {
            bt_user_join.setVisibility(View.VISIBLE);
        }

        //head
        head_file_name = profile.head_file_name;
        do {
            //no head
            if (head_file_name == null || head_file_name.length() <= 0)
                break;

            //load from local
            if (LoadHeadImg(head_file_name))
                break;

            //download from server
            DownLoadHeadImg(profile.head_url);
            break;
        }while (false);

        return true;
    }

    private boolean LoadHeadImg(String file_name) {
        String log_label = "LoadHeadImg";
        if(file_name==null || file_name.length()<=0) {
            Log.e(log_label , "file_name empty!");
            return false;
        }

        bmp_img = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_HEAD , 0 , file_name);
        //no local
        if(bmp_img == null) {
            Log.d(log_label , "read head img empty! file_name:" + file_name);
            return false;
        }

        //load success
        Log.d(log_label , "read head img success! file_name:" + file_name);
        Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_img);
        iv_user_img.setImageBitmap(bmp_tmp);
        return true;
    }


    private void DownLoadHeadImg(String head_url) {
        String log_label = "DownLoadHeadImg";
        //String head_url = AppConfig.user_info.basic.head_url;
        //get head url
        if(head_url==null || head_url.length()<=0) {
            Log.d(log_label , "head_url empty!");
            return;
        }

        //get query
        String head_query = AppConfig.ParseServerUrl(head_url);
        if(head_query==null || head_query.length()<=0) {
            Log.e(log_label , "parse head_url failed!");
            return;
        }
        Log.d(log_label , "head_query:" + head_query + " head_file_name:" + head_file_name);

        //query
        new DownloadFileTask().execute(head_query);
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.user_info_img:
                showFullImg();
                break;
            default:
                //nothing
        }
    }

    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.bt_user_info_join:
                InvitedInGroup();
                break;
            default:
                //nothing
                break;
        }

    }

    private void InvitedInGroup() {
        String log_label = "InvitedInGroup";
        if(from_manage==0 || invited_grp_id==0 || invited_grp_name.length()<=0) {
            Log.e(log_label , "arg illegal!");
            return;
        }
        Log.d(log_label , "grp_id:" + invited_grp_id + " grp_name:" + invited_grp_name);
        String req = CSProto.CSInviteGroupReq(uid , invited_grp_id , invited_grp_name);
        AppConfig.SendMsg(req);
        showProgressDialog(LOAD_FROM_INVITE);
    }


    private void showFullImg() {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;

        if(bmp_img == null)
        {
            AppConfig.PrintInfo(UserInfoActivity.this, "还没有头像");
            return;
        }

        //创建对话框
        img_dialog = new AlertDialog.Builder(UserInfoActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(UserInfoActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);
        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }


    //@from:0
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(UserInfoActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("正在处理");
        progress_dialog.setIndeterminate(false);
        progress_dialog.show();

        WindowManager.LayoutParams lp = progress_dialog.getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        Window win = progress_dialog.getWindow();
        win.setAttributes(lp);

        // 呈现2s
        new Thread(new Runnable() {

            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                int progress = 0;

                while (System.currentTimeMillis() - startTime < 2000) {
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
                        //LoadUserInfo(uid , LOAD_FROM_SEARCH);
                        AfterShowProgress(from);
                    }
                });
            }
        }).start();
    }

    private void AfterShowProgress(int from) {
        switch (from) {
            case LOAD_FROM_SEARCH:
                LoadUserInfo(uid , LOAD_FROM_SEARCH);
                break;
            case LOAD_FROM_INVITE:
                AppConfig.PrintInfo(this , "已发送");
                finish();
                break;
            default:
                break;
        }
    }


    private void TrytoQuery() {
        ArrayList<Long> uids = new ArrayList<>();
        uids.add(uid);
        String req = CSProto.CSFetchUserProfileReq(uids);
        AppConfig.SendMsg(req);

        showProgressDialog(LOAD_FROM_SEARCH);
    }

    /*
     * HTTPSTRINGYTASK
     */
    private class DownloadFileTask extends AsyncTask<String, Void, byte[]>
    {

        @Override
        protected byte[] doInBackground(String... params)
        {
            InputStream input_stream = null;
            byte[] data = null;

            try
            {
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

            return data;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(byte[] data)
        {
//			progress_dialog.cancel();
            if(data == null)
                return;

            /*Parse BITMAP*/
            try
            {
                Bitmap bmp_head = BitmapFactory.decodeByteArray(data, 0, data.length);
                if(bmp_head != null)
                {
                    Log.i("download", "download head done");
                    AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_HEAD , 0 , bmp_head , head_file_name);
                    bmp_img = bmp_head;
                    //iv is round
                    Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_head);
                    iv_user_img.setImageBitmap(bmp_tmp);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

}
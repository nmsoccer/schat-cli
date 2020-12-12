package com.app.schat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class UserSelfActivity extends AppCompatActivity {
    private TextView tv_save_img;
    private TextView tv_show_img;
    private TextView tv_name;
    private EditText et_pass;
    private EditText et_nick_name;
    private EditText et_addr;
    private EditText et_email;
    private EditText et_desc;
    private TextView tv_user_uid;


    private ProgressDialog progress_dialog;
    private static Handler handler;

    private ImageView iv_head;
    private int has_img = 0;
    private Bitmap bmp_img = null;
    private String head_file_name = null;
    private FileServInfo file_serv_info = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_self);

        //Check Login
        if(AppConfig.IsLogin() == false) {
            AppConfig.PrintInfo(this , "未登陆");
            return;
        }


        //widget
        UserBasic basic_info = AppConfig.user_info.basic;
        tv_name = (TextView)this.findViewById(R.id.tv_user_self_name);
        tv_name.setText(AppConfig.UserName);
        iv_head = (ImageView)this.findViewById(R.id.iv_user_self_head);
        et_pass = (EditText)this.findViewById(R.id.et_user_self_password);
        et_nick_name = (EditText)this.findViewById(R.id.et_user_self_nick_name);
        et_nick_name.setHint(basic_info.name);
        et_email = (EditText)this.findViewById(R.id.et_user_self_email);
        et_addr = (EditText)this.findViewById(R.id.et_user_self_addr);
        et_addr.setHint(basic_info.addr);
        tv_show_img = (TextView)this.findViewById(R.id.tv_user_self_show_img);
        tv_save_img = (TextView)this.findViewById(R.id.tv_user_self_save_img);
        tv_user_uid = (TextView)this.findViewById(R.id.tv_user_self_uid);
        tv_user_uid.setText("用户ID " + AppConfig.UserUid);

        et_desc = (EditText)this.findViewById(R.id.et_user_self_desc);
        et_desc.setHint(AppConfig.user_info.detail.desc);
        handler = new Handler();
        //load from local
        /*
        String file_name = "h_" + AppConfig.UserUid + ".jpg";
        bmp_img = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_HEAD , 0 , file_name);
        if(bmp_img != null) {
            Log.d("userself" , "read head img success! file_name:" + file_name);
            iv_head.setImageBitmap(bmp_img);
        } else
            Log.d("userself" , "read head img empty! file_name:" + file_name);
        */
        head_file_name = AppConfig.user_info.basic.head_file_name;
        if(LoadHeadImg(head_file_name)) {
            return;
        }

        //download
        if(head_file_name!=null && head_file_name.length()>0)
            DownLoadHeadImg();
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
        iv_head.setImageBitmap(bmp_tmp);
        has_img = 1;
        return true;
    }

    private void DownLoadHeadImg() {
        String log_label = "DownLoadHeadImg";
        String head_url = AppConfig.user_info.basic.head_url;
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

    //@from:0 quit group; 1 kick member
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(UserSelfActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("sending...");
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
                        FinishUpdate();
                    }
                });
            }
        }).start();
    }

    private void FinishUpdate() {
        String log_label = "FinishUpdate";
        if(AppConfig.update_user_self != CSProto.COMMON_RESULT_SUCCESS) {
            AppConfig.PrintInfo(this , "修改失败,请稍后再试");
        } else {
            AppConfig.PrintInfo(this , "修改成功");
        }

        AppConfig.update_user_self = -1;
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        String log_label = "onActivityResult";
        Bitmap bmp_get = null;
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode != RESULT_OK) {
            Log.e(log_label , "result failed! code:" + requestCode);
            return;
        }
        Log.d(log_label , "result code:" + requestCode);

        Uri uri;
        switch(requestCode)
        {
            case AppConfig.FILE_SELECT_CODE:
            case AppConfig.USE_CAM_CODE:
                uri = intent.getData();
                if(uri == null) {
                    Log.e(log_label , "uri null");
                    break;
                }

                ContentResolver cr = getContentResolver();
                try
                {
                    InputStream is_tmp;
                    is_tmp = cr.openInputStream(uri);
                    /*
                    if(is_tmp.available() > 200*1024)
                    {
                        AppConfig.PrintInfo(this, "您的头像大小已经超过200KB.需要进行裁剪");
                        startPhotoZoom(intent.getData());
                        return;
                    }*/
                    Bitmap bmp_tmp = BitmapFactory.decodeStream(is_tmp);
                    is_tmp.close();


                    //check size
                    AppConfig.TryNotifyImgSize(this , bmp_tmp.getByteCount());
                    bmp_get = bmp_tmp;
                    /*
                    bmp_get = AppConfig.compressImage(bmp_tmp, AppConfig.HEAD_FILE_SIZE/1024);
                    if(bmp_get == null)
                    {
                        AppConfig.PrintInfo(this, "压缩图片出错，请重新选择");
                        return;
                    }*/
                    tv_save_img.setVisibility(View.VISIBLE);
                    tv_show_img.setVisibility(View.GONE);

                }
                catch (FileNotFoundException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

        //修改之
        if(bmp_get == null)
            return;

        has_img = 1;
        bmp_img = bmp_get;
        iv_head.setImageBitmap(bmp_img);

    }




    private void onClickChooseImg()
    {
        AppConfig.PrintInfo(this, "选择图片");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try
        {
            startActivityForResult(intent, AppConfig.FILE_SELECT_CODE);
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            ex.printStackTrace();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_user_self_save_img:
                SaveHeadImg();
                break;
        }
    }

    private void SaveHeadImg() {
        String log_label = "SaveHeadImg";
        if(bmp_img == null) {
            Log.e(log_label , "bmp_img null!");
            return;
        }

        //Get Random FileAddr
        file_serv_info = AppConfig.GetFileServ(-1);
        if(file_serv_info == null) {
            Log.e(log_label , "get file serv failed!");
            AppConfig.PrintInfo(this , "系统错误");
            return;
        }

        //Save Head
        String upload_query = AppConfig.FileAddr2UploadUrl(file_serv_info.Addr);
        new UpdateHeadTask().execute(upload_query);

    }

    private void showFullImg() {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;

        if(bmp_img == null)
        {
            AppConfig.PrintInfo(UserSelfActivity.this, "还没有头像");
            return;
        }

        //创建对话框
        img_dialog = new AlertDialog.Builder(UserSelfActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(UserSelfActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);
        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }

    public void onImgClick(View v) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(UserSelfActivity.this);
        LayoutInflater inflater = LayoutInflater.from(UserSelfActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("选图");
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        bt_cancel.setText("预览");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                onClickChooseImg();
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
                showFullImg();
            }
        });
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
                    Log.i("download", "download head done bitmap:" + bmp_head.getByteCount()/1024 + "k data:" + data.length/1024 + "k");
//					Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_head);
                    AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_HEAD , 0 , bmp_head , head_file_name);
                    bmp_img = bmp_head;
                    //iv is round
                    Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_head);
                    iv_head.setImageBitmap(bmp_tmp);
                    has_img = 1;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }


    /*
     * Task
     */
    private class UpdateHeadTask extends AsyncTask<String, Void, String>
    {
        String log_label = "UpdateHeadTask";
        InputStream input_stream = null;
        @Override
        protected String doInBackground(String... params)
        {
            String result = null;
            if(file_serv_info == null) {
                return null;
            }

            try
            {
                /*open connection*/
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("url_type", AppConfig.URL_TYPE_HEAD);
                map.put("uid", AppConfig.UserUid);
                map.put("grp_id", 0);
                map.put("tmp_id", 111);
                map.put("token", file_serv_info.Token);

                ArrayList<InputStream> is_list = new ArrayList<InputStream>();
                InputStream img_is = null;
                if(bmp_img.getByteCount() > AppConfig.MAX_IMG_SIZE) {
                    int scale = AppConfig.MAX_IMG_SIZE * 100 / bmp_img.getByteCount();
                    Log.i(log_label , "will compress scale:" + scale);
                    img_is = AppConfig.Bitmap2IS(bmp_img, scale);
                }
                else
                    img_is = AppConfig.Bitmap2IS(bmp_img , 100);

                //check stream
                if(img_is == null)
                    return null;
                is_list.add(img_is);

                input_stream = AppConfig.uploadFile(params[0] , map , is_list);
                if(input_stream == null)
                {
                    return null;
                }

                /*read input*/
                result = AppConfig.ReadInputStream(input_stream);
                input_stream.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(String result)
        {
//			progress_dialog.dismiss();
            if(progress_dialog != null)
                progress_dialog.dismiss();


            Log.d("upload img" , "result:" + result);
            if(result == null || result.length()<=0)
            {
                return ;
            }

            /*检查最开始char是否为0*/
            /*Parse JSON*/
            try
            {
                JSONTokener parser = new JSONTokener(result);

                //1.get root
                JSONObject root = (JSONObject)parser.nextValue();

                //2.get ret
                if(root.getInt("result") == CSProto.COMMON_RESULT_SUCCESS)
                {
                    AppConfig.PrintInfo(getBaseContext(), "修改成功");
                    //跳到个人主页 will recv new head_url
                    Intent intent = new Intent("ChatPersonal");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                    finish();
                }
                else
                {
                    AppConfig.PrintInfo(getBaseContext(), "修改失败");
                }
                return;

            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }


        }

    }

    public void onBtnClick(View v) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(UserSelfActivity.this);
        LayoutInflater inflater = LayoutInflater.from(UserSelfActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
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
                DoUpdateAttr();
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

    private void DoUpdateAttr() {
        String log_label = "DoUpdateAttr";

        String pass = "";
        String desc = "";
        String role_name = "";
        String addr = "";
        //pass
        if(et_pass.getText() != null && et_pass.getText().toString().length()>0) {
            pass = et_pass.getText().toString();
        }

        //role_name
        if(et_nick_name.getText() != null && et_nick_name.getText().toString().length()>0) {
            role_name = et_nick_name.getText().toString();
        }

        //addr
        if(et_addr.getText() != null && et_addr.getText().toString().length()>0) {
            addr = et_addr.getText().toString();
        }

        //desc
        if(et_desc.getText() != null && et_desc.getText().toString().length()>0) {
            desc = et_desc.getText().toString();
        }

        Log.i(log_label , "role:" + role_name + " addr:" + addr + " pass:" + pass + " desc" + desc);
        if(role_name.length()==0 && addr.length()==0 && pass.length()==0 && desc.length()==0) {
            AppConfig.PrintInfo(this , "没有更改");
            return;
        }

        //create
        AppConfig.update_user_self = -1;
        String req = CSProto.CSUpdateUserReq(role_name , pass , desc , addr);
        AppConfig.SendMsg(req);

        //process
        showProgressDialog(0);
    }


}
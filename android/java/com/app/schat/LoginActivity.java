package com.app.schat;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class LoginActivity extends AppCompatActivity {
    EditText login_user;
    EditText login_pass;

    Button login_submit;

    String UserName = "";
    String Password = "";

    int saved_seconds = 0;

    private SharedPreferences shared_data;
    private SharedPreferences.Editor editor;
    char saved_before = 0;	/*之前是否保存*/
    private ProgressDialog progress_dialog;
    private static Handler handler;
    private String log_label = "login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /*get widget*/
        login_user = (EditText)findViewById(R.id.login_user);
        login_pass = (EditText)findViewById(R.id.login_pass);
        login_submit = (Button)findViewById(R.id.login_submit);
        /*get shared*/
        shared_data = getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        editor = shared_data.edit();
        handler = new Handler();
        /*自动填充*/
        if(shared_data.getString(AppConfig.KEY_SAVED_USER, null) != null)
        {
            login_user.setText(shared_data.getString(AppConfig.KEY_SAVED_USER, null));
        }
        if(shared_data.getString(AppConfig.KEY_SAVED_PASS, null) != null)
        {
            saved_before = 1;
            String ency_pass = shared_data.getString(AppConfig.KEY_SAVED_PASS , "");
            String real_pass = AppConfig.DecryptPasswd(shared_data.getString(AppConfig.KEY_SAVED_USER, "") , ency_pass);
            if(!TextUtils.isEmpty(real_pass)) { //decrypt success
                login_pass.setText(real_pass);
                //login_pass.setVisibility(View.GONE);
            }
        }

        /*set actionbar*/
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        String log_label = "login.on_destroy";
        super.onDestroy();
        Log.d(log_label , "done");
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
                finish();
                break;
            case R.id.only_return_return:
                finish();
                break;
        }

        return true;
    }

    public void onClickReg(View v)
    {
        Intent intent = new Intent("ChatReg");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        //finish();
    }


    public void onClick(View view)
    {
        UserName = login_user.getText().toString();
        Password = login_pass.getText().toString();

        /*Get UserName and Passwd*/
        if(UserName.length()<=0)
        {
            AppConfig.PrintInfo(this, "请输入您的用户名");
            return;
        }
        if(UserName.length() > AppConfig.USER_NAME_LEN)
        {
            AppConfig.PrintInfo(this, "用户名最长为 " + AppConfig.USER_NAME_LEN);
            return;
        }
        if(Password.length()<=0)
        {
            AppConfig.PrintInfo(this, "请输入您的密码");
            return;
        }
        if(Password.length() > AppConfig.PASSWORD_LEN)
        {
            AppConfig.PrintInfo(this, "密码最长为 " + AppConfig.PASSWORD_LEN);
            return;
        }



		//AppConfig.PrintInfo(this, "正在提交...用户名：" + UserName + " 密码：" + Password);
        progress_dialog = new ProgressDialog(LoginActivity.this);
        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setMessage("正在登陆...");
        progress_dialog.setIcon(R.drawable.ic_launcher_background);
        progress_dialog.setIndeterminate(false);
        progress_dialog.setCancelable(true);
        progress_dialog.show();

        //login
        TrytoLogin();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void LoginDone() {
        String log_label = "LoginDone";
        if(progress_dialog != null) {
            progress_dialog.cancel();
        }
        //check result
        switch(AppConfig.login_result) {
            case CSProto.COMMON_RESULT_SUCCESS:
                break;
            case CSProto.LOGIN_EMPTY:
                AppConfig.PrintInfo(getBaseContext(), "没有这个用户");
                break;
            case CSProto.LOGIN_PASS:
                AppConfig.PrintInfo(getBaseContext(), "您的密码错误");
                login_pass.setText("");
                break;
            case CSProto.LOGIN_ERR:
                AppConfig.PrintInfo(getBaseContext(), "服务器繁忙");
                login_pass.setText("");
                break;
            case CSProto.LOGIN_MULTI_ON:
                AppConfig.PrintInfo(getBaseContext(), "重新登陆");
                login_pass.setText("");
                break;
            default:
                AppConfig.PrintInfo(getBaseContext(), "登陆失败");
                login_user.setText("");
                login_pass.setText("");
                break;
        }
        if(AppConfig.login_result != CSProto.COMMON_RESULT_SUCCESS) {
            return;
        }

        //IsLogin
        if(AppConfig.IsLogin()) {
            Log.i(log_label , "already login!");
            return;
        }

        //Login Success
        //save info
        AppConfig.UserName = UserName;
        AppConfig.UserUid = AppConfig.user_info.basic.uid;
        editor.putString(AppConfig.KEY_SAVED_USER, UserName);
        //enc password
        String encry_pass = AppConfig.EncryptPasswd(this , UserName , Password);
        if(!TextUtils.isEmpty(encry_pass))
            editor.putString(AppConfig.KEY_SAVED_PASS, encry_pass);
        editor.commit();
        AppConfig.PrintInfo(getBaseContext(), "欢迎您," + AppConfig.user_info.account_name);
        //log print some info


        /***检查SQLITE*/
        String db_name = AppConfig.ServerSpace + "_" + UserName + "_" + DBHelper.DATABASE_NAME;	//与SPACE 和 用户名绑定的库
        DBHelper db_helper = new DBHelper(getBaseContext(),db_name,null, DBHelper.DATABASE_VERSION);
        AppConfig.db = db_helper.getWritableDatabase();
        if(AppConfig.db == null)
        {
            AppConfig.PrintInfo(getBaseContext(), "系统故障，将无法保存您的聊天记录");
        }
        else
        {
            //AppConfig.PrintInfo(getBaseContext(), "Open DB Success!\n");
            //调取聊天记录中所有姓名
            Log.i(log_label , "open db success!");
        }

        //打开或创建server_space_schat/user本地文件目录
        CreateFileDir(LoginActivity.this , UserName);

        //some info
        UserChatInfo chat_info = AppConfig.user_info.detail.chat_info;

        //FillLatestMsg from local
        if(AppConfig.db != null && chat_info.all_group>0) {
            Log.d("user_info" , "all_group:" + chat_info.all_group);
            String msg = "";
            for (UserChatGroup grp : chat_info.all_groups.values()) {
                InitLocalUserChatGroup(grp);
                Log.i("user_info" , "grp_id:" + grp.grp_id + " name:" + grp.grp_name + " last_msg:" + grp.last_msg);
            }
        }

        //create group local file dir
        if(chat_info.all_group > 0) {
            for (long grp_id : chat_info.all_groups.keySet()) {
                //create group dir
                AppConfig.CreateGroupFileDir(grp_id);
            }
        }

        //other parse
        InitUserBasic();

        Log.d(log_label , "i am here");
        //to space
        /*
        Intent intent = new Intent("ChatPersonal");
        startActivity(intent);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);*/
        //
        finish();
    }

    private void InitUserBasic() {
        String log_label = "InitUserBasic";
        UserBasic basic_info = AppConfig.user_info.basic;
        //head file
        if(basic_info.head_url!=null && basic_info.head_url.length()>0) {
            basic_info.head_file_name = AppConfig.Url2RealFileName(basic_info.head_url);
        }
        Log.d(log_label , "finish");
    }


    private void InitLocalUserChatGroup(UserChatGroup grp_info) {
        String log_label = "InitLocalUserChatGroup";

        //query from sql
        String tab_name = DBHelper.grp_id_2_tab_name(grp_info.grp_id);
        String sql =  "SELECT * FROM " + tab_name + " WHERE grp_id=" + grp_info.grp_id + " order by msg_id desc limit 1"; //
        Log.d(log_label , "sql:" + sql);
        Cursor c = AppConfig.db.rawQuery(sql, null);
        if(c == null) {
            return;
        }

        long msg_id = 0;
        long chat_flag = ChatInfo.CHAT_FLAG_NORMAL;
        String msg = "";
        while (c.moveToNext()) {
            msg = c.getString(c.getColumnIndex("chat_content"));
            String dec_msg = DBHelper.DecryptChatContent(msg , AppConfig.user_local_des_key);

            msg_id = c.getLong(c.getColumnIndex("msg_id"));
            chat_flag = c.getLong(c.getColumnIndex("flag"));
            grp_info.last_msg = dec_msg;
            if(chat_flag != ChatInfo.CHAT_FLAG_NORMAL)
                grp_info.last_msg = "...";
            grp_info.local_last_msg_id = msg_id;
            grp_info.local_last_ts = c.getLong(c.getColumnIndex("snd_time"));
            grp_info.local_last_chat_type = c.getInt(c.getColumnIndex("chat_type"));
            break;
        }
        c.close();

        //log
        Log.d(log_label , "msg_id:" + msg_id + " msg:" + grp_info.last_msg);

        //query
        if(grp_info.local_last_msg_id == 0) {
            Log.d(log_label , "local msg is null! query history from net! grp_id:" + grp_info.grp_id);
            String query = CSProto.CSChatHistoryReq(grp_info.grp_id , -1);
            AppConfig.SendMsg(query);
        }

        return;
    }


    private void TrytoLogin() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {   //clear
                    if(AppConfig.common_op_lock.compareAndSet(false , true)) {
                        break;
                    }
                }
                String req = CSProto.CsLoginReq(UserName , Password , AppConfig.version);
                AppConfig.SendMsg(req);

                //wait and check resp
                int v = 0;
                while (v<=AppConfig.REQ_TIMEOUT) {
                    try {
                        if(AppConfig.common_op_lock.get() == false) {
                            Log.i("Login" , "finish");
                            handler.post(new Runnable() {

                                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                                @Override
                                public void run()
                                {
                                    LoginDone();
                                }
                            });
                            return;
                        }
                        Thread.sleep(100); //sleep
                        v++;
                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }
                //end while
                AppConfig.common_op_lock.set(false);
                if (progress_dialog != null) {
                    progress_dialog.cancel();
                }
                Log.e(log_label , "req timeout");
            }
        }).start();
    }

    private void CreateFileDir(Context contentx , String user_name) {
        String log_label = "create_file_dir";
        //check stat
        if(!AppConfig.isExternalStorageWritable()) {
            Log.e(log_label , "external storage not writable");
            return;
        }
        Log.d(log_label , "external storage writable");
        if(!AppConfig.isExternalStorageReadable()) {
            Log.e(log_label , "external storage not readable");
            return;
        }
        Log.d(log_label , "external storage readable");


        //base dir
        //File sdcard_dir = Environment.getExternalStorageDirectory();
        File sdcard_dir = contentx.getExternalFilesDir(AppConfig.ServerSpace + "_"+ AppConfig.DATA_DIR);
        String dir_path = sdcard_dir.getAbsolutePath();
        dir_path = dir_path + "/" + user_name;
        File f_dir = new File(dir_path);
        if(!f_dir.exists())
        {
            f_dir.mkdirs();
            Log.i(log_label , "create dir:" + dir_path);
        }
        AppConfig.ABSOLUTE_DATA_DIR_PATH = dir_path;
        Log.d(log_label , "base dir:" + AppConfig.ABSOLUTE_DATA_DIR_PATH);

        //head
        dir_path = AppConfig.ABSOLUTE_DATA_DIR_PATH + "/head";
        f_dir = new File(dir_path);
        if(!f_dir.exists())
        {
            f_dir.mkdirs();
            Log.i(log_label , "create head dir:" + dir_path);
        }
        AppConfig.USER_HEAD_DIR_PATH = dir_path;
        Log.d(log_label , "head dir:" + AppConfig.USER_HEAD_DIR_PATH);

        //chat main
        dir_path = AppConfig.ABSOLUTE_DATA_DIR_PATH + "/chat_main";
        f_dir = new File(dir_path);
        if(!f_dir.exists())
        {
            f_dir.mkdirs();
            Log.i(log_label , "create chat_main dir:" + dir_path);
        }
        AppConfig.CHAT_MAIN_DIR_PATH = dir_path;
        Log.d(log_label , "chat_main dir:" + AppConfig.CHAT_MAIN_DIR_PATH);

        //group head main
        dir_path = AppConfig.ABSOLUTE_DATA_DIR_PATH + "/group_head";
        f_dir = new File(dir_path);
        if(!f_dir.exists())
        {
            f_dir.mkdirs();
            Log.i(log_label , "create group_head dir:" + dir_path);
        }
        AppConfig.GROUP_HEAD_DIR_PATH = dir_path;
        Log.d(log_label , "group_head dir:" + AppConfig.GROUP_HEAD_DIR_PATH);


    }



}
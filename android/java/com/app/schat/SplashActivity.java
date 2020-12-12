package com.app.schat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SplashActivity extends AppCompatActivity {
    private SharedPreferences shared_data;
    private SharedPreferences.Editor editor;

    private EditText et_server_name;
    private EditText et_server_addr;
    private Button btn_ok;

    private String server_space = "";
    private String dir_addr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String log_label = "splash.create";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /*检查网络*/
        if(AppConfig.checkNet(SplashActivity.this) == false)
        {
            AppConfig.PrintInfo(this, "网络连接未打开，请连接网络重新进入应用");
            return;
        }

        /*Check Storage*/
        //check stat
        if(!AppConfig.isExternalStorageWritable()) {
            AppConfig.PrintInfo(this , "应用需要存储权限用于缓存");
            Log.e(log_label , "external storage not writable");
            AppConfig.verifyStoragePermissions(this);
            return;
        }
        Log.d(log_label , "external storage writable");
        if(!AppConfig.isExternalStorageReadable()) {
            AppConfig.PrintInfo(this , "应用需要存储权限用于缓存");
            Log.e(log_label , "external storage not readable");
            AppConfig.verifyStoragePermissions(this);
            return;
        }

        if(AppConfig.IsLogin())
            return;

        //NoCheckCert
        if(!AppConfig.HttpsCertNoCheck()) {
            Log.e(log_label , "set https cert failed!");
            AppConfig.PrintInfo(this , "HTTP设置错误");
            return;
        }


        /*create shared preference*/
        shared_data = getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        editor = shared_data.edit();
        server_space = shared_data.getString(AppConfig.KEY_SERVER_SPACE, AppConfig.ServerSpace);
        dir_addr = shared_data.getString(AppConfig.KEY_DIR_ADDR , "");
        int req_timeout = shared_data.getInt(AppConfig.KEY_REQ_TIMEOUT , AppConfig.REQ_TIMEOUT); //or default
        Log.d(log_label , "req_timeout set to " + req_timeout);
        AppConfig.REQ_TIMEOUT = req_timeout;

        //widget
        et_server_name = (EditText)this.findViewById(R.id.splash_server_name);
        et_server_name.setHint(server_space);
        et_server_addr = (EditText)this.findViewById(R.id.splash_server_addr);
        btn_ok = (Button)this.findViewById(R.id.bt_splash_enter);
        if(dir_addr.length() > 0)
            et_server_addr.setHint(dir_addr);


        //get last logout
        long curr_ts = AppConfig.CurrentUnixTime();
        long last_exit = shared_data.getLong(AppConfig.KEY_LAST_EXIT , 0);
        String conn_serv_ip = shared_data.getString(AppConfig.KEY_CONN_SERV_IP , "");
        int conn_serv_port = shared_data.getInt(AppConfig.KEY_CONN_SERV_PORT , 0);
        Log.d(log_label , "last_exit:" + last_exit + " conn_serv:" + conn_serv_ip + ":" + conn_serv_port);
        if(conn_serv_ip.length()>0 && conn_serv_port>0) {
            if((curr_ts - last_exit) < AppConfig.LOGIN_SERVER_STICKY) { //10min
                Log.d(log_label , "within login sticky! will use old conn_serv to login!");
                AppConfig.ChatServHost = conn_serv_ip;
                AppConfig.ChatServPort = conn_serv_port;
                AppConfig.ServerSpace = server_space;
                //enter main
                Intent intent = new Intent("ChatMain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return;
            } else {
                Log.d(log_label , "expire 10min! will normal enter!");
            }
        }


        //dump
        if(dir_addr.length()<=0 || server_space.length()<=0) {
            //AppConfig.PrintInfo(this , "请先设置服务名及其地址");
            return;
        }


        //query
        btn_ok.setVisibility(View.GONE); //not now
        String query = "https://" + dir_addr + "/query?query_key=" + AppConfig.Dir_QUERY_KEY;
        new DirIPTask().execute(query);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_splash_enter:
                //get content
                if(et_server_name.getText() != null && et_server_name.getText().toString().length()>0)
                    server_space = et_server_name.getText().toString();
                if(et_server_addr.getText() != null && et_server_addr.getText().toString().length()>0)
                    dir_addr = et_server_addr.getText().toString();

                //check
                if(dir_addr.length()<=0 || server_space.length()<=0) {
                    AppConfig.PrintInfo(this , "请先设置服务名和地址");
                    return;
                }

                //query
                btn_ok.setVisibility(View.GONE); //not now
                String query = "https://" + dir_addr + "/query?query_key=" + AppConfig.Dir_QUERY_KEY;
                new DirIPTask().execute(query);
                break;
            default:
                //nothing
        }

    }

    /*
     * HTTPSTRINGYTASK
     */
    private class DirIPTask extends AsyncTask<String, Void, String>
    {
        String log_label = "DirIPTask";
        InputStream input_stream = null;
        @Override
        protected String doInBackground(String... params)
        {
            String result = null;
            try
            {
                /*open connection*/
                Log.d(log_label , "query:" + params[0]);
                input_stream = AppConfig.openHttpsConnGet(params[0]);
                if(input_stream == null)
                {
                    Log.e(log_label , "input stream null");
                    return null;
                }

                /*read input*/
                result = AppConfig.ReadInputStream(input_stream);
                input_stream.close();
            }
            catch(IOException e)
            {
                result = null;
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(String result)
        {
            if(TextUtils.isEmpty(result))
            {
                Log.e(log_label , "result empty");
                AppConfig.PrintInfo(getBaseContext() , "连接失败");
                btn_ok.setVisibility(View.VISIBLE);
                return ;
            }

            /*Parse JSON*/
            try
            {
                Log.i(log_label , "result: " + result);
                JSONTokener parser = new JSONTokener(result);

                //1.get root
                JSONObject root = (JSONObject)parser.nextValue();

                //2.get status
                //获取本次使用连接的服务器IP
                String conn_url = root.getString("conn_serv");
                if(conn_url==null || conn_url.length()<=0)
                {
                    AppConfig.PrintInfo(getBaseContext(), "获取地址失败");
                    btn_ok.setVisibility(View.VISIBLE);
                    return;
                }
                String[] strs = conn_url.split(":");
                if(strs.length != 2) {
                    AppConfig.PrintInfo(getBaseContext() , "解析地址失败");
                    btn_ok.setVisibility(View.VISIBLE);
                    return;
                }
                AppConfig.ChatServHost = strs[0];
                AppConfig.ChatServPort = Integer.parseInt(strs[1]);
                AppConfig.ServerSpace = server_space;
                Log.d(log_label , "ip:" + AppConfig.ChatServHost + " port:" + AppConfig.ChatServPort + " space:" + AppConfig.ServerSpace);

                //save
                editor.putString(AppConfig.KEY_SERVER_SPACE , server_space);
                editor.putString(AppConfig.KEY_DIR_ADDR , dir_addr);
                editor.putString(AppConfig.KEY_CONN_SERV_IP , AppConfig.ChatServHost);
                editor.putInt(AppConfig.KEY_CONN_SERV_PORT , AppConfig.ChatServPort);
                editor.commit();


                //enter main
                Intent intent = new Intent("ChatMain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();

            }
            catch(JSONException e)
            {
                btn_ok.setVisibility(View.VISIBLE);
                e.printStackTrace();
            }
        }

    }

}
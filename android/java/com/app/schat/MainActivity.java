package com.app.schat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    public static ChatClient chat_client = null;
    public static NewFragment new_frag = null;	//实际只用这一个就好了
    private SharedPreferences shared_data;
    private SharedPreferences.Editor editor;
    private String user_name;
    private boolean main_act_runs = true;
    private TextView tv_head_last_login;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String log_label = "main.create";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*create shared preference*/
        shared_data = getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        editor = shared_data.edit();

        /*移除用户及地址信息*/
        //editor.putString(AppConfig.KEY_USER_NAME , null);
        //editor.commit();
        //user_name = shared_data.getString(AppConfig.KEY_USER_NAME, null);
        //user_name = AppConfig.UserName;
        AppConfig.UserName = "";
        AppConfig.UserUid = 0;

        /*Get Widget*/
        new_frag = new NewFragment();
        AppConfig.main_act = this;

        /*当前是主页*/
        //FragmentTransaction transaction = getFragmentManager().beginTransaction();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main_frag_container , new_frag);
        transaction.commit();

        //test generate
        tv_head_last_login = this.findViewById(R.id.tv_main_head_last_login);

        //create dir
        //create_file_dir();

        //open client
        //lauchClient();
        CheckConn();
        TickSendMsg();
        RecvMsg();

        //check login
        if(!AppConfig.IsLogin()) {
            Intent intent = new Intent("ChatLogin");
            startActivity(intent);
            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        }
    }

    @Override
    public void onStop() {
        String log_label = "main.on_stop";
        super.onStop();
        Log.d(log_label , "done");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d("main.on_resume" , "done");
        if(AppConfig.IsLogin()) {
            String last_login = AppConfig.ConverUnixTime2MinStr(AppConfig.user_info.LastLogout);
            tv_head_last_login.setText("上次使用:\n" + last_login);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("main.on_pause" , "done");
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_main_head_launcher:
                Intent intent = new Intent("About");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            default:
                //nothing
                break;
        }
    }


    public void onClickBottom(View v)
    {
        Intent intent;

        switch(v.getId())
        {
            case R.id.ll_main_bottom_user:
                if(!AppConfig.IsLogin())
                {
                    AppConfig.PrintInfo(this, "请先登陆");
                    //try send
                    /*
                    Date date = new Date();
                    long curr_ts = date.getTime();
                    String cmd = String.format("{\"proto\":1 , \"sub\":{\"ts\":%d}}" , curr_ts);
                    AppConfig.SendMsg(cmd);*/

                    intent = new Intent("ChatLogin");
                    startActivity(intent);
                    overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                    return;
                }
                intent = new Intent("ChatPersonal");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            case R.id.btn_main_bottom_post:
                if(!AppConfig.IsLogin())
                {
                    AppConfig.PrintInfo(this, "登陆用户才能查看广场");
                    return;
                }
                intent = new Intent("ChatGround");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            default:
                break;
        }
    }



    /*
    check conn
     */
    private  void CheckConn() {
        new Thread(new Runnable() {
            private String log_label = "main.check_conn";
            private boolean conn_stat;
            @Override
            public void run() {
                while(main_act_runs) {
                    try {
                        conn_stat = ChatClient.InConnect();
                        if(! conn_stat) {
                            if(AppConfig.ChatServHost.length()>0 && AppConfig.ChatServPort>0)
                                ConnServ();
                            else
                                Log.e(log_label , "addr illegal!" + AppConfig.ChatServHost + ":" + AppConfig.ChatServPort);
                        }
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(log_label , "main_act_destroy!");
            }
        }).start();
    }


    /*
    connect to serv
     */
    private void ConnServ() {
        new Thread(new Runnable() {
            String log_label = "main.ConnServ";
            @Override
            public void run() {
                if(AppConfig.ChatServHost.length()>0 && AppConfig.ChatServPort>0) {
                    ChatClient.Connect(AppConfig.ChatServHost, AppConfig.ChatServPort);
                } else {
                    Log.e(log_label , "addr illegal!" + AppConfig.ChatServHost + ":" + AppConfig.ChatServPort);
                }
            }
        }).start();
    }

    private void TickSendMsg() {
        new Thread(new Runnable() {
            String log_label = "TickSendMsg";
            @Override
            public void run() {
                try {
                    while (true) {
                        if(!main_act_runs) {
                            Log.i(log_label , "main exit! out!");
                            break;
                        }
                        Thread.sleep(AppConfig.HEART_BEAT_FREQUENT * 1000); //sleep  per tick
                        if(!AppConfig.IsLogin())
                            continue;
                        //send heart
                        String cmd = CSProto.CSHeartReq();
                        AppConfig.SendMsg(cmd);
                    }
                } catch (InterruptedException b) {
                    b.printStackTrace();
                }
            }
        }).start();
    }


    private void RecvMsg() {
        new Thread(new Runnable() {
            private String log_label = "main.recv_msg";
            String[] pkgs = new String[10];
            int pkg_count;
            int i;
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                while (true) {
                    if(!main_act_runs) {
                        Log.i(log_label , "main exit! will close sock!");
                        ChatClient.CloseSock();
                        break;
                    }

                    try {
                        Thread.sleep(100); //sleep 100ms per tick
                        //recv
                        pkg_count = ChatClient.RecvMsg(pkgs);
                        if (pkg_count <= 0) {
                            continue;
                        }

                        //handle
                        Log.d("RecvMsg", "pkg-count:" + pkg_count);
                        for(i=0; i<pkg_count; i++) {
                            Log.d("RecvMsg" , "pkg:" + pkgs[i]);
                            CSProto.CsProto(pkgs[i]);
                        }

                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }
            }
        }).start();
    }


    @Override
    protected void onDestroy() {

        String log_label = "main.on_destroy";
        super.onDestroy();

        long exit_ts = AppConfig.CurrentUnixTime();
        Log.i(log_label , "destroy start");
        //logout
        String req = CSProto.CSLogoutReq();
        AppConfig.SendMsg(req);

        /*移除用户及地址信息*/
        //editor.putString(AppConfig.KEY_USER_NAME , null);

        //保存登出时间及其他
        editor.putLong(AppConfig.KEY_LAST_EXIT , exit_ts);
        Log.d(log_label , "last_exit:" + exit_ts);

        editor.putInt(AppConfig.KEY_REQ_TIMEOUT , AppConfig.REQ_TIMEOUT);
        Log.d(log_label , "req_timeout:" + AppConfig.REQ_TIMEOUT);

        editor.commit();

        //drop table
        //DBHelper.del_all_tables();

        /*关闭数据库链接*/
        if(AppConfig.db != null)
        {
            AppConfig.db.close();
//			AppConfig.PrintInfo(this, "Close DB Success!\n");
        }

        //sleep
        /*
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }

         */

        //close connection
        main_act_runs = false;



        Log.i(log_label , "destroy finish");
    }



}
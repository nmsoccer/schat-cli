package com.app.schat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SysSetActivity extends AppCompatActivity {
    private final int LOAD_FROM_RESET_ADDR = 1;
    private final int LOAD_FROM_CLEAR_CACHE = 2;
    private final int LOAD_FROM_MODIFY_PASS = 3;

    private SharedPreferences shared_data;
    private SharedPreferences.Editor editor;
    private TextView tv_dir_addr;
    private EditText et_pass;
    private String server_space = "";
    private String dir_addr = "";
    private ProgressDialog progress_dialog;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sys_set);

        /*create shared preference*/
        shared_data = getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        editor = shared_data.edit();
        server_space = shared_data.getString(AppConfig.KEY_SERVER_SPACE, AppConfig.ServerSpace);
        dir_addr = shared_data.getString(AppConfig.KEY_DIR_ADDR , "");


        //widget
        tv_dir_addr = (TextView)this.findViewById(R.id.tv_sys_set_serv_addr);
        tv_dir_addr.setText("<" + server_space + ">" + dir_addr);
        et_pass = (EditText)this.findViewById(R.id.et_sys_modify_pass);

        handler = new Handler();

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_sys_set_reset_dir:
                ResetDirAddr();
                break;
            case R.id.bt_sys_set_clear_cache:
                ClearLocalCache();
                break;
            case R.id.bt_sys_set_modify_pass:
                ModifyPass();
                break;
            default:
                //nothing
                break;
        }
    }

    private void ResetDirAddr() {

        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(SysSetActivity.this);
        LayoutInflater inflater = LayoutInflater.from(SysSetActivity.this);
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
                //reset
                dialog.dismiss();
                showProgressDialog(LOAD_FROM_RESET_ADDR);
                editor.putString(AppConfig.KEY_DIR_ADDR , "");
                editor.putString(AppConfig.KEY_CONN_SERV_IP , "");
                editor.putInt(AppConfig.KEY_CONN_SERV_PORT , 0);
                editor.commit();
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


    private void ClearLocalCache() {

        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(SysSetActivity.this);
        LayoutInflater inflater = LayoutInflater.from(SysSetActivity.this);
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
                //clear
                dialog.dismiss();
                AppConfig.ClearAllLocalDir();
                showProgressDialog(LOAD_FROM_CLEAR_CACHE);
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

    private void ModifyPass() {

        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(SysSetActivity.this);
        LayoutInflater inflater = LayoutInflater.from(SysSetActivity.this);
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
                //
                dialog.dismiss();
                if(et_pass.getText() == null) {
                    AppConfig.PrintInfo(getBaseContext() , "密码为空");
                    return;
                }
                //pass
                String pass = et_pass.getText().toString();
                if(AppConfig.CheckPassWord(getBaseContext() , pass , 6 , 12) < 0) {
                    return;
                }
                //Send
                AppConfig.update_user_self = -1;
                String req = CSProto.CSUpdateUserReq("" , pass , "" , "");
                AppConfig.SendMsg(req);
                AppConfig.PrintInfo(getBaseContext() , "发送成功");
                showProgressDialog(LOAD_FROM_MODIFY_PASS);
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


    //@from:0 quit group; 1 kick member
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(SysSetActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("正在设置...");
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
                        AfterProcessDialog(from);
                    }
                });
            }
        }).start();
    }

    private void AfterProcessDialog(int from) {
        switch (from) {
            case LOAD_FROM_RESET_ADDR:
                AppConfig.PrintInfo(getBaseContext() , "重置成功");
                finish();
                break;
            case LOAD_FROM_CLEAR_CACHE:
                AppConfig.PrintInfo(getBaseContext() , "清除成功");
                break;
            case LOAD_FROM_MODIFY_PASS:
                FinishUpdatePass();
                break;
            default:
                //nothing
                break;
        }

    }

    private void FinishUpdatePass() {
        String log_label = "FinishUpdatePass";
        if(AppConfig.update_user_self != CSProto.COMMON_RESULT_SUCCESS) {
            AppConfig.PrintInfo(this , "修改失败,请稍后再试");
            return;
        }
        AppConfig.PrintInfo(this , "修改成功");


        AppConfig.PrintInfo(getBaseContext() , "修改密码成功");
        editor.putString(AppConfig.KEY_SAVED_PASS, "");
        editor.commit();
        AppConfig.update_user_self = -1;
        finish();
    }

}
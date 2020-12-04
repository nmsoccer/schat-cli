package com.app.schat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CreateGroupActivity extends AppCompatActivity {
    private ProgressDialog progress_dialog;
    private Handler handler;

    private EditText et_grp_name;
    private EditText et_pass;
    private EditText et_desc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        //check login
        if(!AppConfig.IsLogin()) {
            AppConfig.PrintInfo(this , "未登陆");
            return;
        }

        //widget
        et_grp_name = (EditText)this.findViewById(R.id.et_create_grp_name);
        et_pass = (EditText)this.findViewById(R.id.et_create_grp_password);
        et_desc = (EditText)this.findViewById(R.id.et_create_grp_desc);

        //handler
        handler = new Handler();

    }



    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.bt_create_group_submit:
                onClickCreate();
                break;
        }
    }

    private void DoCreateGroup() {
        String log_label = "DoCreateGroup";
        //grp_name
        String grp_name = et_grp_name.getText().toString();
        if(grp_name==null || grp_name.length()==0)
        {
            AppConfig.PrintInfo(this, "请输入群组名");
            return;
        }
        if(AppConfig.CheckName(this , grp_name , 12) < 0)
        {
            AppConfig.PrintInfo(this , "用户名出错");
            return;
        }

        //pass
        String pass_world = et_pass.getText().toString();
        if(pass_world==null || pass_world.length()==0)
        {
            AppConfig.PrintInfo(this, "请输入密码");
            return;
        }
        if(AppConfig.CheckPassWord(this , pass_world , 6 , 24) < 0)
        {
            return;
        }

        //desc
        String desc = et_desc.getText().toString();
        if(desc==null || desc.length()<=0)
            desc = "...";


        Log.d(log_label , "grp_name:" + grp_name + " pass:" + pass_world + " desc:" + desc);
        AppConfig.create_group_result = CSProto.CREATE_GRP_RESET;;
        String req = CSProto.CSCreateGroupReq(grp_name , pass_world , desc);
        AppConfig.SendMsg(req);
        showProgressDialog(0 , 3000);
    }

    private void CreateGroupDone() {
        switch(AppConfig.create_group_result) {
            case CSProto.COMMON_RESULT_SUCCESS:
                AppConfig.PrintInfo(getBaseContext() , "创建成功");
                AppConfig.create_group_result = CSProto.CREATE_GRP_RESET;
                finish();
                return;
            case CSProto.CREATE_GRP_DUP_NAME:
                AppConfig.PrintInfo(getBaseContext(), "您已创建同名群组");
                break;
            case CSProto.CREATE_GRP_MAX_NUM:
                AppConfig.PrintInfo(getBaseContext(), "已到创建群组上限");
                break;
            case CSProto.CREATE_GRP_DB_ERR:
                AppConfig.PrintInfo(getBaseContext(), "系统错误");
                break;
            case CSProto.CREATE_GRP_RET_FAIL:
                AppConfig.PrintInfo(getBaseContext(), "系统异常");
                break;
            default:
                AppConfig.PrintInfo(getBaseContext(), "创建失败");
                break;
        }

        AppConfig.create_group_result = CSProto.CREATE_GRP_RESET;
    }

    private void onClickCreate() {

        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateGroupActivity.this);
        LayoutInflater inflater = LayoutInflater.from(CreateGroupActivity.this);
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
                DoCreateGroup();
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
    private void showProgressDialog(final int from , final int millisec) {
        progress_dialog = new ProgressDialog(CreateGroupActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("正在创建...");
        progress_dialog.setIndeterminate(false);
        progress_dialog.show();

        WindowManager.LayoutParams lp = progress_dialog.getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        Window win = progress_dialog.getWindow();
        win.setAttributes(lp);

        // 只呈现1.5s
        new Thread(new Runnable() {

            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                int progress = 0;

                while (System.currentTimeMillis() - startTime < millisec) {
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
                        CreateGroupDone();
                    }
                });
            }
        }).start();
    }


}
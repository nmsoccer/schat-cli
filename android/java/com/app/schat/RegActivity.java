package com.app.schat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegActivity extends AppCompatActivity {
    private String UserName = null;
    private String Password = null;
    private int Sex = 0;
    private String NickName = "";
    private String Email = null;
    private String City = null;
    private String SelfDesc = null;

    private EditText et_user;
    private EditText et_pass;
    private EditText et_nick;
    private RadioGroup rg_sex;
    private EditText et_email;
    private EditText et_city;
    private EditText et_selfdesc;
    private ImageView iv_reg;
    private InputStream img_is = null;
    private int has_img = 0;
    private Bitmap bmp_img = null;
    private ProgressDialog progress_dialog;

    private static Handler handler;
    private String log_label = "reg_activity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        /*get widge*/
        et_user = (EditText)findViewById(R.id.reg_user);
        et_pass = (EditText)findViewById(R.id.reg_password);
        et_nick = (EditText)findViewById(R.id.reg_nick_name);
        rg_sex = (RadioGroup)findViewById(R.id.reg_sexual);
        et_email = (EditText)findViewById(R.id.reg_email);
        et_city = (EditText)findViewById(R.id.reg_city);
        et_selfdesc = (EditText)findViewById(R.id.reg_self);
        iv_reg = (ImageView)findViewById(R.id.reg_img);

        handler = new Handler();
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
    public void onDestroy()
    {
        super.onDestroy();
        if(img_is != null)
        {
            try
            {
                img_is.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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

    public static boolean isEmail (String pInput) {
        if(pInput == null){
            return false;
        }
        String regEx = "([\\w[_-][\\.]]+@+[\\w[_-]]+\\.+[A-Za-z]{2,3})|([\\"
                + "w[_-][\\.]]+@+[\\w[_-]]+\\.+[\\w[_-]]+\\.+[A-Za-z]{2,3})|"
                + "([\\w[_-][\\.]]+@+[\\w[_-]]+\\.+[\\w[_-]]+\\.+[\\w[_-]]+"
                + "\\.+[A-Za-z]{2,3})";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(pInput);
        return matcher.matches();
    }

    public void onBtnClick(View v) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(RegActivity.this);
        LayoutInflater inflater = LayoutInflater.from(RegActivity.this);
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
                DoReg();
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


    private void DoReg()
    {

        /*Get User*/
        UserName = et_user.getText().toString();
        if(TextUtils.isEmpty(UserName))
        {
            AppConfig.PrintInfo(this, "请输入用户名");
            return;
        }
        if(AppConfig.CheckName(this , UserName , 12) != 0) {
            AppConfig.PrintInfo(this , "用户名出错");
            return;
        }

        NickName = et_nick.getText().toString();
        if(NickName==null || NickName.length()== 0)
        {
            NickName = UserName;
        }
        else {
            if(AppConfig.CheckName(this , NickName , 12) != 0)
            {
                AppConfig.PrintInfo(this , "昵称出错");
                return;
            }
        }

        /*Get Pass*/
        Password = et_pass.getText().toString();
        if(TextUtils.isEmpty(Password))
        {
            AppConfig.PrintInfo(this, "请输入密码");
            return;
        }
        if(AppConfig.CheckPassWord(this , Password , 6 , 12) != 0) {
            return;
        }

        /*Get SEX*/
        switch(rg_sex.getCheckedRadioButtonId())
        {
            case R.id.reg_male:
                Sex = AppConfig.SEX_MALE;
                break;
            case R.id.reg_female:
                Sex = AppConfig.SEX_FEMALE;
                break;
            default:
                AppConfig.PrintInfo(this, "请选择性别");
                return;
        }

        /*Get EMAIL*/
        Email = et_email.getText().toString();
		/*
		if(Email == null || Email.length()<=0)
		{
			Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show();
			return;
		}
		if(isEmail(Email) != true)
		{
			AppConfig.PrintInfo(this, "不是合法邮箱地址");
			return;
		}*/

        /*Get CITY*/
        City = et_city.getText().toString();
        if(City==null || City.length()<=0) {
            City = "moon"; //default
        }
        if(AppConfig.CheckName(this , City , 128) < 0)
        {
            AppConfig.PrintInfo(this , "地址出错");
            return;
        }

        /*Get SELF*/
        SelfDesc = et_selfdesc.getText().toString();


        /***Send*/
        progress_dialog = new ProgressDialog(RegActivity.this);
        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setMessage("正在注册...");
        progress_dialog.setIndeterminate(false);
        progress_dialog.setCancelable(true);
        progress_dialog.show();

        //req
        TrytoReg();
    }

    private void RegDone() {
        if (progress_dialog != null) {
            progress_dialog.cancel();
        }

        switch(AppConfig.reg_result) {
            case CSProto.COMMON_RESULT_SUCCESS:
                AppConfig.PrintInfo(getBaseContext() , "注册成功，请前往登陆");
                finish();
                break;
            case CSProto.REG_SAME_NAME:
                AppConfig.PrintInfo(getBaseContext(), "该用户名已存在");
                break;
            case CSProto.REG_SYS_ERR:
                AppConfig.PrintInfo(getBaseContext(), "服务器错误");
                break;
            default:
                AppConfig.PrintInfo(getBaseContext(), "注册失败");
                break;
        }

    }


    private void TrytoReg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //AppConfig.common_op_done.set(false);    //clear
                while(true) {   //clear
                    if(AppConfig.common_op_lock.compareAndSet(false , true)) {
                        break;
                    }
                }
                String req = CSProto.CsRegReq(UserName , Password , NickName , Sex , City);
                AppConfig.SendMsg(req);

                //wait rsp
                int v = 0;
                while (v <= AppConfig.REQ_TIMEOUT) {
                    try {
                        if(AppConfig.common_op_lock.get() == false) {
                            Log.i(log_label , "finish");
                            handler.post(new Runnable() {

                                @Override
                                public void run()
                                {
                                    RegDone();
                                }
                            });
                            return;
                        }
                        Thread.sleep(1000); //sleep
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

}
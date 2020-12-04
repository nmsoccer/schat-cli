package com.app.schat;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SpaceActivity extends AppCompatActivity {
    private SharedPreferences shared_data;
    private SharedPreferences.Editor editor;

    private ImageView iv_unreaded_msg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space);

        /*create shared preference*/
        shared_data = getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        editor = shared_data.edit();

        //UserName
        if(!AppConfig.IsLogin())
        {
            AppConfig.PrintInfo(this, "您的用户信息错误，请重新登录");
            return;
        }

        /*set actionbar*/
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //widget
        iv_unreaded_msg = (ImageView)this.findViewById(R.id.iv_space_sys_msg_new);

    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(!AppConfig.IsLogin())
            return;
        boolean un_readed = Message.UnReadedMsg();
        if(un_readed) {
            iv_unreaded_msg.setVisibility(View.VISIBLE);
        }else {
            iv_unreaded_msg.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
//		super.onCreateOptionsMenu(menu);
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
        Intent intent;

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

    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.ll_space_modify_self:
                intent = new Intent("UserSelf");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            case R.id.ll_space_my_message:
                intent = new Intent("NewMessage");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            case R.id.ll_space_master_group:
                intent = new Intent("MasterGroup");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            case R.id.ll_space_create_group:
                intent = new Intent("CreateGroup");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            case R.id.ll_space_sys_set:
                intent = new Intent("SysSet");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                break;
            default:
                break;
        }
    }

    public void onClickBottom(View v)
    {
        Intent intent;

        switch(v.getId())
        {
            case R.id.ll_main_bottom_home:
                /*
                intent = new Intent("ChatMain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);*/
                finish();
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

}
package com.app.schat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ForwardActivity extends AppCompatActivity {
    private ArrayList<HashMap<String , Object>> grp_list = new ArrayList<>();
    private ArrayList<CheckBox> cb_list = new ArrayList<>();

    private int chat_type = -1;
    private String chat_content = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String log_label = "forward.create";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forward);

        //Login
        if(!AppConfig.IsLogin())
            return;

        //NoGroup
        if(AppConfig.user_info.detail.chat_info.all_group<=0 || AppConfig.user_info.detail.chat_info.all_groups==null) {
            AppConfig.PrintInfo(this , "没有群组");
            return;
        }

        //Get Intent
        Intent intent = getIntent();
        chat_type = intent.getIntExtra("chat_type" , -1);
        chat_content = intent.getStringExtra("content");
        if(chat_type<0 || TextUtils.isEmpty(chat_content)) {
            Log.e(log_label , "chat info illegal! type:" + chat_type + " content:" + chat_content);
            AppConfig.PrintInfo(this , "信息出错");
            return;
        }
        Log.d(log_label , "chat info type:" + chat_type + " content:" + chat_content);


        //Fill Group
        for(UserChatGroup u_grp : AppConfig.user_info.detail.chat_info.all_groups.values()) {
            HashMap item = new HashMap();
            item.put("grp_id" , u_grp.grp_id);
            item.put("grp_name" , u_grp.grp_name);
            grp_list.add(item);
        }


        // 动态加载checkbox
        LinearLayout ll_groups = (LinearLayout) this.findViewById(R.id.ll_forward_groups);
        // 给指定的checkbox赋值
        for (int i = 0; i < grp_list.size(); i++) {
            // 先获得checkbox.xml的对象
            //CheckBox checkBox = new CheckBox(this);
            CheckBox checkBox = (CheckBox) getLayoutInflater().inflate(
                    R.layout.check_box, null);
            HashMap item = grp_list.get(i);
            checkBox.setText(item.get("grp_name").toString());
            //checkBox.setTextColor(getResources().getColor(R.color.bg_midnight_blue));
            //Log.d(log_label , "add cb:" + item.get("grp_name").toString());
            // 实现了在
            ll_groups.addView(checkBox);
            cb_list.add(checkBox);
            //tv
            TextView tv_span = (TextView)getLayoutInflater().inflate(R.layout.text_view , null);
            ll_groups.addView(tv_span);
        }



    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_forward_done:
                ForwardChat();
                break;
            default:
                break;
        }

    }

    private void ForwardChat() {
        String log_label = "ForwardChat";
        long grp_id = 0;
        for(int i=0; i<cb_list.size(); i++) {
            if(cb_list.get(i).isChecked()) {
                grp_id = Long.parseLong(grp_list.get(i).get("grp_id").toString());
                Log.d(log_label , "checked:" + i + " grp_name:" + grp_list.get(i).get("grp_name").toString() + " grp_id:" + grp_id);
                String req = CSProto.CSSendChatReq(chat_type , grp_id , chat_content , 111);
                AppConfig.SendMsg(req);
            }
        }

        AppConfig.PrintInfo(this , "转发成功");
        finish();
    }


}
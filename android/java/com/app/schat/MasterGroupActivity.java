package com.app.schat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MasterGroupActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MyAdapter adpater;
    private ArrayList<HashMap<String, Object>> content;
    private ListView ptor_list;
    private static Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_group);

        //login
        if(!AppConfig.IsLogin()) {
            AppConfig.PrintInfo(this , "您未登录");
            return;
        }

        //widget
        ptor_list = (ListView)findViewById(R.id.lv_master_group);
        content = new ArrayList<HashMap<String,Object>>();
        adpater = new MyAdapter(this , content);
        ptor_list.setAdapter(adpater);
        ptor_list.setOnItemClickListener(this);

        //load
        LoadGroup();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Intent intent = new Intent("GroupDetail");
        intent.putExtra("from_mange" , 1);
        HashMap<String , Object> map = content.get(position);
        intent.putExtra("grp_name", map.get("grp_name").toString());
        intent.putExtra("grp_id" , Long.parseLong(map.get("grp_id").toString()));
        startActivity(intent);
        this.overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
    }

    private  class MyAdapter extends BaseAdapter
    {
        private Context context = null;
        private List<HashMap<String, Object>> chatList = null;
        private LayoutInflater inflater = null;


        public MyAdapter(Context context,List<HashMap<String, Object>> chatList)
        {
            this.context = context;
            this.chatList = chatList;
            inflater = LayoutInflater.from(this.context);
        }

        @Override
        public int getCount() {
            return chatList.size();
        }

        @Override
        public Object getItem(int position) {
            return chatList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatHolder chatHolder = null;

            if (convertView == null)
            {

                chatHolder = new ChatHolder();
                convertView = inflater.inflate(R.layout.chat_main_list, null);

                chatHolder.iv_head = (ImageView) convertView.findViewById(R.id.img_chat_main_list);
                chatHolder.tv_group_name = (TextView)convertView.findViewById(R.id.grp_chat_main_list);
                chatHolder.tv_time = (TextView)convertView.findViewById(R.id.time_chat_main_list);
                chatHolder.tv_content = (TextView)convertView.findViewById(R.id.content_chat_main_list);
                chatHolder.tv_group_id = (TextView)convertView.findViewById(R.id.grp_id_chat_main_list);
                chatHolder.tv_msg_id = (TextView)convertView.findViewById(R.id.msg_id_chat_main_list);
                convertView.setTag(chatHolder);

            }
            else
            {
                chatHolder = (ChatHolder)convertView.getTag();
            }



            HashMap<String , Object> item = (HashMap<String, Object>)chatList.get(position);
            chatHolder.tv_group_name.setText(item.get("grp_name").toString());
            //String src_content = item.get("content").toString();
            //SpannableString sp = AppConfig.String2EmotionSp(getBaseContext() , getResources() , src_content);
            chatHolder.tv_content.setText("");
            chatHolder.iv_head.setBackgroundResource(R.drawable.group_main);
            chatHolder.tv_time.setText(item.get("update_time").toString());
            chatHolder.tv_group_id.setText(item.get("grp_id").toString());
            chatHolder.tv_msg_id.setText(item.get("msg_id").toString());

            return convertView;
        }

        private class ChatHolder
        {
            private ImageView iv_head;
            private TextView tv_group_name;
            private TextView tv_group_id;
            private TextView tv_time;
            private TextView tv_content;
            private TextView tv_msg_id;
        }

    }

    private void LoadGroup() {
        String log_label = "LoadGroup";
        if(!AppConfig.IsLogin()) {
            return;
        }
        if(AppConfig.user_info == null) {
            Log.d(log_label , "user info null");
            return;
        }
        if(AppConfig.user_info.detail.chat_info.master_group <= 0) {
            AppConfig.PrintInfo(this , "你未创建任何群组!");
            return;
        }

        //try lock
        if(AppConfig.TryLockBool(AppConfig.user_info.lock) == false) {
            AppConfig.PrintInfo(this , "加载失败");
            return;
        }

        //add
        for(Long grp_id : AppConfig.user_info.detail.chat_info.master_groups.keySet()) {
            UserChatGroup item = AppConfig.user_info.detail.chat_info.all_groups.get(grp_id);
            Log.d(log_label , "id:" + item.grp_id + " name:" + item.grp_name);
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("update_time" , AppConfig.ConverUnixTime2MinStr(item.enter_ts));
            map.put("content" , item.last_msg);
            map.put("grp_id" , item.grp_id);
            map.put("grp_name" , item.grp_name);
            map.put("msg_id" , item.local_last_msg_id);
            content.add(map);
        }
        adpater.notifyDataSetChanged();
        Log.d(log_label , "LoadGroup Finish");
        AppConfig.UnLockBool(AppConfig.user_info.lock);
    }

}
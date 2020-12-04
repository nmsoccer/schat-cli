package com.app.schat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MyAdapter adpater;
    private ArrayList<HashMap<String, Object>> content;
    private ListView lv_self;

    private String user_name;
    private long last_load_ts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //widget
        lv_self = (ListView)this.findViewById(R.id.lv_message);
        content = new ArrayList<HashMap<String,Object>>();
        /*
        int i  = 0;
        for(i=0; i<4; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("send_time" , i+1000);
            map.put("content" , "some big words");
            map.put("type" , i*200);
            map.put("author" , "group" + i);
            content.add(map);
        }
        */

        adpater = new MyAdapter(this, content);
        lv_self.setAdapter(adpater);
        lv_self.setOnItemClickListener(this);

        /*set actionbar*/
        ActionBar actionBar = getActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //load
        load_msg_list();
        last_load_ts = AppConfig.CurrentUnixTime();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
        Intent intent;
        HashMap<String , Object> item = (HashMap<String, Object>)parent.getItemAtPosition(position);

        //swtich type:
        int msg_type = Integer.parseInt(item.get("type").toString());
        int msg_id = Integer.parseInt(item.get("msg_id").toString());
        Message.read_msg_item(msg_id);

        switch (msg_type) {
            case Message.MESSAGE_TYPE_APPLY_GROUP:
                onClickGroupApply(position);
                break;
            default:
                //
                onClickDefault(position);
                break;
        }

    }

    private void onClickDefault(final int pos) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        LayoutInflater inflater = LayoutInflater.from(MessageActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("删除");
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
                //del
                HashMap<String , Object> item = (HashMap<String, Object>)content.get(pos);
                if(item == null) {
                    return;
                }
                int msg_id = Integer.parseInt(item.get("msg_id").toString());
                Message.del_msg_item(msg_id);
                content.remove(pos);
                adpater.notifyDataSetChanged();
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
                //set read
                HashMap<String , Object> item = (HashMap<String, Object>)content.get(pos);
                if(item == null) {
                    return;
                }
                int read = Integer.parseInt(item.get("read").toString());
                if(read == 0) {
                    item.put("read" , 1);
                    adpater.notifyDataSetChanged();
                }
            }
        });

    }

    //dispatch diff apply
    private void onClickGroupApply(final int pos) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        LayoutInflater inflater = LayoutInflater.from(MessageActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("同意");
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        bt_cancel.setText("拒绝");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                DoGroupAudit(pos , true);
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
                DoGroupAudit(pos , false);
            }
        });

    }

    //send group audit
    private void DoGroupAudit(int pos , boolean allow) {
        String log_label = "DoGroupAudit";
        int audit = 0;
        if(allow)
            audit = 1;

        HashMap<String , Object> item = (HashMap<String, Object>)content.get(pos);
        MessageItem msg_item = (MessageItem) item.get("msg_item");
        if(msg_item == null) {
            Log.e(log_label , "msg_item nil! pos:" + pos);
            return;
        }
        String req = CSProto.CSApplyGroupAudit(audit , msg_item.uid , msg_item.grp_id , msg_item.grp_name);
        AppConfig.SendMsg(req);

        //
        AppConfig.PrintInfo(this , "已发送");

        //del item
        Message.del_msg_item(msg_item.msg_id);
        content.remove(pos);
        adpater.notifyDataSetChanged();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_message_reload:
                long curr_ts = AppConfig.CurrentUnixTime();
                if((curr_ts - last_load_ts) <=3) {
                    AppConfig.PrintInfo(this , "刷新太频繁 请稍后再试");
                    return;
                }

                last_load_ts = curr_ts;
                content.clear();
                adpater.notifyDataSetChanged();
                load_msg_list();
                break;
            default:
                //nothing
                break;
        }
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
                convertView = inflater.inflate(R.layout.message_list, null);

                chatHolder.tv_author = (TextView)convertView.findViewById(R.id.author_msg_detail_list);
                chatHolder.tv_time = (TextView)convertView.findViewById(R.id.time_msg_detail_list);
                chatHolder.tv_content = (TextView)convertView.findViewById(R.id.content_msg_detail_list);
                chatHolder.tv_type = (TextView)convertView.findViewById(R.id.type_msg_detail_list);
                chatHolder.tv_read = (TextView)convertView.findViewById(R.id.read_msg_detail_list);
                convertView.setTag(chatHolder);

            }
            else
            {
                chatHolder = (ChatHolder)convertView.getTag();
            }



            HashMap<String , Object> item = (HashMap<String, Object>)chatList.get(position);
            chatHolder.tv_author.setText(item.get("author").toString());
            //这里将所有_xx_替换为表情
            String src_content = item.get("content").toString();
            chatHolder.tv_content.setText(src_content);
            chatHolder.tv_time.setText(item.get("send_time").toString());
            chatHolder.tv_type.setText(item.get("type").toString());
            int read = Integer.parseInt(item.get("read").toString());
            if(read == 0)
                chatHolder.tv_read.setText("未读");
            else
                chatHolder.tv_read.setText(" ");

            return convertView;
        }

        private class ChatHolder
        {
            private TextView tv_author;
            private TextView tv_time;
            private TextView tv_content;
            private TextView tv_type;
            private TextView tv_read;
        }

    }

    private void load_msg_list() {
        //load
        MessageItem msg_item;
        ArrayList<MessageItem> msg_list = Message.fetch_msg_item();
        for(int i=0; i<msg_list.size(); i++) {
            msg_item = msg_list.get(i);
            HashMap<String , Object> item = new HashMap<>();
            item.put("author" , msg_item.author);
            item.put("content" , msg_item.content);
            item.put("send_time" , AppConfig.ConverUnixTime2MinStr(msg_item.snd_ts));
            item.put("type" , msg_item.msg_type);
            item.put("read" , msg_item.read);
            item.put("msg_item" , msg_item);
            item.put("msg_id" , msg_item.msg_id);

            content.add(item);
        }
        if(msg_list.size() <= 0) {
            AppConfig.PrintInfo(this , "没有更多");
            return;
        }
        adpater.notifyDataSetChanged();
        lv_self.setSelection(msg_list.size() - 1);
    }


}
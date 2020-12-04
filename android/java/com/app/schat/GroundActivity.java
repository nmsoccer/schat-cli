package com.app.schat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroundActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private MyAdapter adpater;
    private ArrayList<HashMap<String, Object>> content;
    private ListView ptor_list;
    private EditText et_search;
    private static Handler handler;
    private String log_label = "gound_activity";

    private long search_group_id = 0;
    private ProgressDialog progress_dialog;

    private final int from_create = 0;
    private final int from_search = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ground);

        if(AppConfig.IsLogin() == false) {
            return;
        }

        handler = new Handler();
        //listview
        ptor_list = (ListView)findViewById(R.id.ptrl_ground);
        content = new ArrayList<HashMap<String,Object>>();
        //添加ITEM
        /*
        int i  = 0;
        for(i=0; i<4; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("update_time" , i+1000);
            map.put("content" , "some big words");
            map.put("grp_id" , i*200);
            map.put("grp_name" , "group" + i);
            map.put("msg_id" , 123);
            content.add(map);
        }
         */

        adpater = new MyAdapter(this , content);
        ptor_list.setAdapter(adpater);
        ptor_list.setOnItemClickListener(this);

        //widget
        et_search = (EditText)findViewById(R.id.et_ground_search);

        //query
        //CheckGroundGroup();
        String req = CSProto.CSGroupGroundReq(-1);
        AppConfig.SendMsg(req);

        //dialog
        showProgressDialog(from_create);
    }


    @Override
    public void onResume()
    {
        super.onResume();
        //fetch ground
        if(AppConfig.IsLogin()) {
            RefreshGround();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
        HashMap<String , Object> item = (HashMap<String, Object>)parent.getItemAtPosition(position);
        long grp_id = Long.parseLong(item.get("grp_id").toString());
        //in group
        if(UserInfo.IsInGroup(grp_id)) {
            Intent intent = new Intent("GroupDetail");
            intent.putExtra("grp_name", item.get("grp_name").toString());
            intent.putExtra("grp_id" , grp_id);
            startActivity(intent);
            this.overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
            return;
        }
        //not in group
        Intent intent = new Intent("GroupSnap");
        intent.putExtra("chat_name", item.get("grp_name").toString());
        intent.putExtra("grp_id" , item.get("grp_id").toString());
        intent.putExtra("mem_count" , item.get("mem_count").toString());
        intent.putExtra("desc" , item.get("desc").toString());
        intent.putExtra("head_url" , item.get("head_url").toString());
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
            GroundActivity.MyAdapter.ChatHolder chatHolder = null;

            if (convertView == null)
            {

                chatHolder = new GroundActivity.MyAdapter.ChatHolder();
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
                chatHolder = (GroundActivity.MyAdapter.ChatHolder)convertView.getTag();
            }



            HashMap<String , Object> item = (HashMap<String, Object>)chatList.get(position);
            chatHolder.tv_group_name.setText(item.get("grp_name").toString());
            String src_content = item.get("content").toString();
            SpannableString sp = AppConfig.String2EmotionSp(getBaseContext() , getResources() , src_content);
            chatHolder.tv_content.setText(sp);
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

    private void RefreshGround() {
        Log.d(log_label , "RefreshGround");
        HashMap<String, Object> map;
        long grp_id;
        long latest_msg_id;
        boolean change = false;
        /*
        for(int i=0; i<content.size(); i++) {
            map = content.get(i);
            grp_id = Long.parseLong(map.get("grp_id").toString());
            latest_msg_id = Long.parseLong(map.get("msg_id").toString());
            //check
            if(latest_msg_id != UserInfo.GetGrpLatestMsgId(grp_id)) {
                //map.put("content" , "......");
                map.put("msg_id" , UserInfo.GetGrpLatestMsgId(grp_id));
                change = true;
            }
        }*/
        if(change) {
            adpater.notifyDataSetChanged();
            Log.d(log_label , "RefreshGround changed!");
        }
        Log.d(log_label , "RefreshGround finish!");
    }

    /*
    private void GroudGroupDone() {
        Log.d(log_label, "GroudGroupDone");
        if(AppConfig.groupd_groups.item_map.size() <= 0) {
            return;
        }
        if(AppConfig.groupd_groups.fetch_count <= 0) {
            Log.d("new_frag" , "GroudGroupDone fetch nothing");
            return;
        }

        //add
        for (GroupGroundItem item : AppConfig.groupd_groups.item_map.values()) {
            Log.d(log_label , "GroudGroupDone id:" + item.getGrp_id() + " name:" + item.getGrp_name());
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("update_time" , "");
            map.put("content" , UserInfo.GetGrpLatestMsg(item.getGrp_id()));
            map.put("grp_id" , item.getGrp_id());
            map.put("grp_name" , item.getGrp_name());
            map.put("msg_id" , 0);
            content.add(map);
        }
        adpater.notifyDataSetChanged();
        Log.d(log_label , "GroudGroupFinish");
    }*/

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ground_do_search:
                String str = et_search.getText().toString();
                if(str==null || str.length()<=0) {
                    AppConfig.PrintInfo(this , "请输入群组ID");
                    break;
                }
                search_group_id = Long.parseLong(str);
                SearchGroup(search_group_id);
                break;
            default:
                break;
        }
    }


    private void SearchGroup(long grp_id) {
        //load from cache
        UserChatGroup grp_info = UserInfo.getChatGrp(grp_id);
            //got
        if(grp_info != null) {
            Intent intent = new Intent("GroupDetail");
            intent.putExtra("grp_name", grp_info.grp_name);
            intent.putExtra("grp_id" , grp_info.grp_id);
            startActivity(intent);
            this.overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
            return;
        }

            //snap
        GroupGroundItem grp_snap = ChatInfo.GetGroupSnap(grp_id);
        if(grp_snap != null) {
            Intent intent = new Intent("GroupSnap");
            intent.putExtra("chat_name", grp_snap.grp_name);
            intent.putExtra("grp_id" , Long.toString(grp_snap.grp_id));
            intent.putExtra("head_url" , grp_snap.head_url);
            startActivity(intent);
            this.overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
            return;
        }

        //query from web
        String req = CSProto.CSQueryGroupReq(grp_id);
        AppConfig.SendMsg(req);
        showProgressDialog(from_search);
    }

    private void AfterSearchGrp() {
        do {
            if (search_group_id <= 0) {
                AppConfig.PrintInfo(this, "未搜寻到该群组信息");
                break;
            }
            GroupGroundItem grp_snap = ChatInfo.GetGroupSnap(search_group_id);
            if (grp_snap == null) {
                AppConfig.PrintInfo(this, "未搜寻到该群组信息");
                break;
            }

            //go to snap
            Intent intent = new Intent("GroupSnap");
            intent.putExtra("chat_name", grp_snap.grp_name);
            intent.putExtra("grp_id", Long.toString(grp_snap.grp_id));
            intent.putExtra("mem_count" , Integer.toString(grp_snap.mem_count));
            intent.putExtra("desc" , grp_snap.desc);
            intent.putExtra("head_url" , grp_snap.head_url);
            startActivity(intent);
            this.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            break;
        }while (false);

        search_group_id = 0;
        return;
    }

    private void LoadGroundGroup() {
        String log_label = "LoadGroundGroup";
        ArrayList<GroupGroundItem> item_list = ChatInfo.GetGroupSnapList();
        if(item_list==null || item_list.size()<=0) {
            return;
        }

        //load
        GroupGroundItem item;
        for(int i=0; i<item_list.size(); i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            item = item_list.get(i);
            map.put("update_time" , "");
            String latest_content = UserInfo.GetGrpLatestMsg(item.grp_id);
            if(latest_content.length() > 0) {
                latest_content = "......";
            }
            map.put("content" , item.desc);
            map.put("grp_id" , item.grp_id);
            map.put("grp_name" , item.grp_name);
            map.put("msg_id" , 0);
            map.put("mem_count" , item.mem_count);
            map.put("desc" , item.desc);
            map.put("head_url" , item.head_url);
            content.add(map);
        }
        adpater.notifyDataSetChanged();
    }

    //@from:0 quit group; 1 kick member
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(GroundActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("正在拉取...");
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
                        switch (from) {
                            case from_create:
                                LoadGroundGroup();
                                break;
                            case from_search:
                                AfterSearchGrp();
                                break;
                            default:
                                break;
                        }

                    }
                });
            }
        }).start();
    }


    /*
    private void CheckGroundGroup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //query
                while(true) {
                    AppConfig.groupd_groups.in_proc.compareAndSet(false, true);
                    break;
                }
                AppConfig.groupd_groups.fetch_count = 0;
                String req = CSProto.CSGroupGroundReq(-1);
                AppConfig.SendMsg(req);

                //wait result
                int v = 0;
                while (v<=AppConfig.REQ_TIMEOUT) {
                    try {
                        if(AppConfig.groupd_groups.in_proc.get() == false) {
                            Log.d(log_label , "CheckGroundGroup finish");
                            handler.post(new Runnable() {

                                @Override
                                public void run()
                                {
                                    GroudGroupDone();
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
                AppConfig.groupd_groups.in_proc.set(false);
                Log.e(log_label , "req timeout");
            }
        }).start();
    }*/

}
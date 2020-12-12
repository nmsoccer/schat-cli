package com.app.schat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewFragment extends Fragment implements OnItemClickListener {
    private MyAdapter adpater;
    private ArrayList<HashMap<String, Object>> content;
    private ListView ptor_list;
    private static Handler handler;
    private String log_label = "new_frag";
    private AtomicBoolean in_loading = new AtomicBoolean(false);
    private boolean check_new = true;
    private boolean pause = false;
    @Override
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState) {
        View root_view =  inflater.inflate(R.layout.new_frag, container, false);

        //handler
        handler = new Handler();

        //listview
        ptor_list = (ListView)root_view.findViewById(R.id.ptrl_new_frag);
        //String[] str = {"新北市","台北市","台中市","台南市","高雄市"};
        //ArrayAdapter adpater = new ArrayAdapter(getActivity() , android.R.layout.simple_list_item_1 , str);
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
            content.add(map);
        }*/


        adpater = new MyAdapter(getActivity() , content);
        ptor_list.setAdapter(adpater);
        ptor_list.setOnItemClickListener(this);
        CheckGroup();
        return root_view;
    }


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //reload
        //loaded = false;
        //content.clear();
        LoadGroup();
        pause = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        check_new = false;
        content.clear();
        adpater.notifyDataSetChanged();
    }

    public void onPause() {

        super.onPause();
        pause = true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
        HashMap<String , Object> item = (HashMap<String, Object>)parent.getItemAtPosition(position);
        //AppConfig.PrintInfo(getActivity() , "group_id:" + item.get("grp_id").toString());
        Intent intent = new Intent("ChatDetail");

        intent.putExtra("chat_name", item.get("grp_name").toString());
        intent.putExtra("grp_id" , item.get("grp_id").toString());
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
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
                chatHolder.iv_red_point = (ImageView)convertView.findViewById(R.id.red_point_chat_main_list);
                chatHolder.iv_content = (ImageView)convertView.findViewById(R.id.content_iv_chat_main_list);
                convertView.setTag(chatHolder);

            }
            else
            {
                chatHolder = (ChatHolder)convertView.getTag();
            }



            HashMap<String , Object> item = (HashMap<String, Object>)chatList.get(position);
            long grp_id = Long.parseLong(item.get("grp_id").toString());
            chatHolder.tv_group_name.setText(item.get("grp_name").toString());
            //表情
            String src_content = item.get("content").toString();
            SpannableString sp = AppConfig.String2EmotionSp(getActivity() , getResources() , src_content);
            chatHolder.tv_content.setText(sp);
            chatHolder.tv_time.setText(item.get("update_time").toString());
            chatHolder.tv_group_id.setText(item.get("grp_id").toString());
            chatHolder.tv_msg_id.setText(item.get("msg_id").toString());
            int new_msg = Integer.parseInt(item.get("new_msg").toString());
            if(new_msg > 0) {
                chatHolder.iv_red_point.setVisibility(View.VISIBLE);
            } else {
                chatHolder.iv_red_point.setVisibility(View.GONE);
            }
            int chat_type = Integer.parseInt(item.get("chat_type").toString());
            Log.d(log_label , "chat_type:" + chat_type);
            chatHolder.iv_content.setImageDrawable(null);
            if(chat_type == CSProto.CHAT_MSG_TYPE_IMG) {
                chatHolder.tv_content.setVisibility(View.GONE);
                chatHolder.iv_content.setVisibility(View.VISIBLE);
                chatHolder.iv_content.setImageDrawable(getResources().getDrawable(R.drawable.pic));
            } else if(chat_type == CSProto.CHAT_MSG_TYPE_MP4) {
                chatHolder.tv_content.setVisibility(View.GONE);
                chatHolder.iv_content.setVisibility(View.VISIBLE);
                chatHolder.iv_content.setImageDrawable(getResources().getDrawable(R.drawable.video_choose));
            } else {
                chatHolder.tv_content.setVisibility(View.VISIBLE);
                chatHolder.iv_content.setVisibility(View.GONE);
            }


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
            private ImageView iv_red_point;
            private ImageView iv_content;
        }

    }

    public class UserChatGroupComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            UserChatGroup p1 = (UserChatGroup) o1;
            UserChatGroup p2 = (UserChatGroup) o2;
            if (p1.local_last_ts < p2.local_last_ts)
                return 1;
            else if(p1.local_last_ts > p2.local_last_ts)
                return -1;
            else
                return 0;
        }

    }

    private void LoadGroup() {
        String log_label = "LoadGroup";
        if(!AppConfig.IsLogin()) {
            return;
        }
        if(AppConfig.user_info == null) {
            Log.d(log_label , "LoadGroup user info null");
            return;
        }
        if(AppConfig.user_info.detail.chat_info.all_group <= 0) {
            AppConfig.PrintInfo(getActivity() , "你未加入任何群组!");
            return;
        }
        if(in_loading.get()) {
            return;
        }
        in_loading.set(true);
        content.clear();
        Log.d(log_label , "start...");
        ArrayList<UserChatGroup> grp_list = new ArrayList<>();
        for(UserChatGroup item : AppConfig.user_info.detail.chat_info.all_groups.values()) {
            grp_list.add(item);
        }
        //sort
        UserChatGroupComparator comp = new UserChatGroupComparator();
        Collections.sort(grp_list , comp);

        //add
        for(int i=0; i<grp_list.size(); i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            UserChatGroup item = grp_list.get(i);
            //Log.d(log_label , "grp_id:" + item.grp_id + " ts:" + item.local_last_ts);
            map.put("update_time" , AppConfig.ConverUnixTime2MinStr(item.local_last_ts));
            map.put("content" , item.last_msg);
            map.put("grp_id" , item.grp_id);
            map.put("grp_name" , item.grp_name);
            map.put("msg_id" , item.local_last_msg_id);
            map.put("new_msg" , 0);
            map.put("chat_type" , item.local_last_chat_type);
            content.add(map);
        }
        //add
        /*
        for(UserChatGroup item : AppConfig.user_info.detail.chat_info.all_groups.values()) {
            //Log.d(log_label , "GroudGroupDone id:" + item.getGrp_id() + " name:" + item.getGrp_name());
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("update_time" , AppConfig.ConverUnixTime2MinStr(item.local_last_ts));
            map.put("content" , item.last_msg);
            map.put("grp_id" , item.grp_id);
            map.put("grp_name" , item.grp_name);
            map.put("msg_id" , item.local_last_msg_id);
            map.put("new_msg" , 0);
            map.put("chat_type" , item.local_last_chat_type);
            content.add(map);
        }

         */
        adpater.notifyDataSetChanged();
        Log.d(log_label , "LoadGroup Finish");
        in_loading.set(false);
    }

    private void RefreshNew() {
        String log_label = "RefreshNew";
        if(in_loading.get()) {
            return;
        }

        //check each item
        long grp_id = 0;
        int new_msg = 0;
        boolean data_change = false;
        ArrayList<Integer> new_list = new ArrayList<>();
        HashMap<String , Object> item;
        for(int i=0; i<content.size(); i++) {
            item = content.get(i);
            grp_id = Long.parseLong(item.get("grp_id").toString());
            new_msg = Integer.parseInt(item.get("new_msg").toString());


            //check
            if(new_msg>0 && !UserInfo.getGrpNewMsgStat(grp_id)) {
                //not new anymore
                Log.d(log_label , "no new msg any more! grp_id:" + grp_id);
                item.put("new_msg" , 0);
                data_change = true;
            } else if(new_msg==0 && UserInfo.getGrpNewMsgStat(grp_id)) {
                Log.d(log_label , "new msg! grp_id:" + grp_id + " pos:" + i);
                item.put("new_msg" , 1);
                data_change = true;
                new_list.add(i);
            }

        }

        //reorder new
        if(new_list.size() > 0) {
            Log.d(log_label , "will resort list");
            int item_pos;
            ArrayList<HashMap> reoder_list = new ArrayList<>();
            //del
            for(int i=0; i<new_list.size(); i++) {
                item_pos = new_list.get(i);
                Log.d(log_label , "will top:" + item_pos + " size:" + content.size());
                if(item_pos>=0 && item_pos<content.size()) {
                    item = content.get(item_pos);
                    reoder_list.add(item);
                    content.remove(item_pos);
                }
            }

            //insert head
            for(int i=0; i<reoder_list.size(); i++) {
                item = reoder_list.get(i);
                content.add(0 , item);
            }
            Log.d(log_label , "resort done!");
        }


        if(data_change) {
            Log.d(log_label , "data changed");
            adpater.notifyDataSetChanged();
        } else {
            //Log.d(log_label , "no data changed");
        }

    }


    private void CheckGroup() {
        new Thread(new Runnable() {
            private String log_label = "CheckGroup";
            @Override
            public void run() {
                Log.d(log_label , "CheckGroup starts");

                //wait result
                while (check_new) {
                    try {
                        Thread.sleep(3000);
                        if(pause) {
                            //Log.d(log_label , "CheckGroup in pause");
                            continue;
                        }

                        //reload group
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                RefreshNew();
                            }
                        });


                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }
                Log.d(log_label , "CheckGroup exit!");
            }
        }).start();
    }


}

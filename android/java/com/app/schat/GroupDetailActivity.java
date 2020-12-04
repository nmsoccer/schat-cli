package com.app.schat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupDetailActivity extends AppCompatActivity {
    private final int DIALOG_FROM_QUIT_GROUP = 0;
    private final int DIALOG_FROM_KICK_MEMBER = 1;
    private final int DIALOG_FROM_CHG_ATTR = 2;

    private ChatGroup grp_info = null;
    private ImageView iv_grp_img = null;
    private TextView tv_grp_name = null;
    private TextView tv_grp_id = null;
    private TextView tv_create_ts = null;
    private TextView tv_member_count = null;
    private TextView tv_master = null;
    private TextView tv_visible = null;
    private TextView btn_exit_group = null;
    private TextView tv_group_show_img;
    private TextView tv_group_save_img;
    private GridView gv_members = null;
    private TextView tv_desc;
    private EditText et_desc;
    private EditText et_visible;
    private EditText et_change_name;
    private Button bt_edit;
    private Button bt_save;
    private ImageView iv_add_member;
    private EditText et_search_uid;
    private Button bt_do_search;
    private RelativeLayout rl_search_panel;

    private Bitmap bmp_img = null;
    private String group_name = null;
    private long grp_id = 0;
    private long master_uid = 0;
    private static Handler handler;
    private int del_member_pos = -1;
    private int member_count = 0;
    private boolean group_visible = false;

    private String log_label = "group_detail";
    private List<HashMap<String, Object>> list;
    private MyAdapter adapter;
    private ProgressDialog progress_dialog;
    private int from_manage = 0;    //from master manage
    private boolean edit_press = false;
    private FileServInfo file_serv_info = null;
    private boolean show_search_panel = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_group_detail);

        //Login
        if(!AppConfig.IsLogin()) {
            AppConfig.PrintInfo(this , "您未登录");
            finish();
            return;
        }



        //Widget
        iv_grp_img = (ImageView)this.findViewById(R.id.grp_detail_img);
        tv_grp_name = (TextView)this.findViewById(R.id.grp_detail_name);
        tv_grp_id = (TextView)this.findViewById(R.id.grp_detail_grp_id);
        tv_create_ts = (TextView)this.findViewById(R.id.grp_detail_ts);
        tv_member_count = (TextView)this.findViewById(R.id.grp_detail_member_count);
        tv_master = (TextView)this.findViewById(R.id.grp_detail_master);
        tv_visible = (TextView)this.findViewById(R.id.grp_detail_visible);
        gv_members = (GridView)this.findViewById(R.id.grp_detail_members);
        tv_desc = (TextView)this.findViewById(R.id.grp_detail_desc_tv);
        et_desc = (EditText)this.findViewById(R.id.grp_detail_desc_et);
        et_visible = (EditText)this.findViewById(R.id.grp_detail_visible_et);
        et_change_name = (EditText)this.findViewById(R.id.grp_detail_change_name);
        bt_edit = (Button)this.findViewById(R.id.grp_detail_edit);
        bt_save = (Button)this.findViewById(R.id.grp_detail_save);
        btn_exit_group = (Button)this.findViewById(R.id.grp_detail_exit);
        tv_group_show_img = (TextView)this.findViewById(R.id.tv_group_detail_show_img);
        tv_group_save_img = (TextView)this.findViewById(R.id.tv_group_detail_save_img);
        iv_add_member = (ImageView)this.findViewById(R.id.iv_grp_detail_add_member);
        et_search_uid = (EditText)this.findViewById(R.id.et_group_detail_search);
        bt_do_search = (Button) this.findViewById(R.id.btn_group_detail_do_search);
        rl_search_panel = (RelativeLayout)this.findViewById(R.id.rl_group_detail_search);

        //grid
        list = new ArrayList<HashMap<String, Object>>();
        /*
        for (int i = 0; i < 15; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("name", "图片" + i);
            map.put("uid" , i);
            list.add(map);
        }*/

        // 2.为数据源设置适配器
        adapter = new MyAdapter();
        // 3.将适配过后点数据显示在GridView 上
        gv_members.setAdapter(adapter);
        gv_members.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //AppConfig.PrintInfo(GroupDetailActivity.this , list.get(position).get("name").toString());
                onClickMember(position);
            }
        });



        /***RECVER*/
        Intent intent = this.getIntent();
        group_name = intent.getStringExtra("grp_name");
        grp_id = intent.getLongExtra("grp_id" , 0);
        from_manage = intent.getIntExtra("from_mange" , 0);
        if(group_name==null || group_name.length()<=0 || grp_id<=0)
        {
            AppConfig.PrintInfo(getBaseContext(), "对不起，群组信息错误！");
            finish();
            return;
        }
        tv_grp_name.setText(group_name);
        tv_grp_id.setText(Long.toString(grp_id));
        if(from_manage == 1 ) { //from master
            btn_exit_group.setText("解散群组");
        }


        //handler
        handler = new Handler();

        //load from cache
        if(LoadGroupInfo(grp_id)) {
            return;
        }

        //Load from server
        TrytoQuery();
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

    private boolean LoadGroupInfo(long grp_id) {
        grp_info = ChatInfo.GetChatGroup(grp_id);
        if(grp_info == null) {
            return false;
        }

        //load img
        if(grp_info.head_url!=null && grp_info.head_url.length()>0)
            LoadHeadImg(grp_info.head_url);
        else
            iv_grp_img.setBackgroundResource(R.drawable.group_main_white);

        //set widget
        tv_create_ts.setText(AppConfig.ConverUnixTime2Str(grp_info.create_ts));
        member_count = grp_info.mem_count;
        tv_member_count.setText(Integer.toString(member_count , 0));
        tv_master.setText(Long.toString(grp_info.master , 0));
        master_uid = grp_info.master;
        group_visible = grp_info.visible;
        if(grp_info.visible) {
            tv_visible.setText("是");
            et_visible.setHint("1");
        } else {
            tv_visible.setText("否");
            et_visible.setHint("0");
        }
        ArrayList<Long> master_uids = new ArrayList<>();
        master_uids.add(grp_info.master);
        HashMap<Long, UserProfile> master_profile = UserInfo.GetUserProfiles(master_uids);
        if(master_profile!=null && master_profile.containsKey(grp_info.master)) {
            tv_master.setText(master_profile.get(grp_info.master).name);
        }
        //not from mange and is master
        if(from_manage==1 || master_uid!=AppConfig.UserUid) {
            btn_exit_group.setVisibility(View.VISIBLE);
        }
        if(from_manage==1) {
            bt_edit.setVisibility(View.VISIBLE);
            tv_group_show_img.setVisibility(View.VISIBLE);
            iv_add_member.setVisibility(View.VISIBLE);
        }
        tv_desc.setText(grp_info.desc);
        et_desc.setHint(grp_info.desc);
        et_change_name.setHint(group_name);
        //members
        ArrayList<Long> members = grp_info.members;
        if(members!=null && members.size()>0) {
            Long uid;
            ArrayList<Long> empty_uids;
            UserProfile profile;
            do {
                //first load from cache
                HashMap<Long, UserProfile> profiles = UserInfo.GetUserProfiles(members);
                if(profiles == null) {
                    empty_uids = members;
                    break;
                }

                //other
                empty_uids = new ArrayList<>();
                for(int i=0; i<members.size(); i++) {
                    uid = members.get(i);
                    profile = profiles.get(uid);
                    if(profile == null) {
                        empty_uids.add(uid);
                    } else {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("name", profile.name);
                        map.put("uid" , profile.uid);
                        list.add(map);
                    }
                }
                break;
            }while (false);

            //not shoot
            for(int i=0; i<empty_uids.size(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("name", empty_uids.get(i));
                map.put("uid" , empty_uids.get(i));
                list.add(map);
            }
            adapter.notifyDataSetChanged();

        }

        return true;
    }


    private void onClickMember(final int pos) {
        //Is Self
        HashMap<String , Object> item = list.get(pos);
        long uid = Integer.parseInt(item.get("uid").toString());
        if(uid == AppConfig.UserUid) {
            return;
        }

        //not from master manage
        if(from_manage == 0) {
            //view
            Intent intent = new Intent("UserInfo");
            intent.putExtra("uid" , item.get("uid").toString());
            startActivity(intent);
            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            return;
        }

        boolean is_master = false;
        if(master_uid == AppConfig.UserUid) {   //group master
            is_master = true;
        }

        //muster have rights to kick
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(GroupDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("查看");
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        if(is_master)
            bt_cancel.setText("踢出");
        else
            bt_cancel.setText("取消");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                //view
                HashMap<String , Object> item = list.get(pos);
                Intent intent = new Intent("UserInfo");
                intent.putExtra("uid" , item.get("uid").toString());
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            }
        });

        //cancel
        if(!is_master) {
            bt_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //nothing
                    dialog.dismiss();
                }
            });
        } else { //kick
            bt_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //nothing
                    dialog.dismiss();
                    HashMap<String , Object> item = list.get(pos);
                    if(item == null || item.get("uid") == null) {
                        AppConfig.PrintInfo(GroupDetailActivity.this , "信息错误");
                        return;
                    }
                    long target_uid = Long.parseLong(item.get("uid").toString());
                    del_member_pos = pos;
                    KickMember(target_uid);
                }
            });
        }

    }

    private void TrytoQuery() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String req = CSProto.CSQueryGroupReq(grp_id);
                AppConfig.SendMsg(req);
                int v = 0;
                //wait rsp
                while (v<=AppConfig.REQ_TIMEOUT) {
                    try {
                        if(ChatInfo.GetChatGroup(grp_id) != null) {
                            Log.i(log_label , "finish");
                            handler.post(new Runnable() {

                                @Override
                                public void run()
                                {
                                    LoadGroupInfo(grp_id);
                                }
                            });
                            return;
                        }
                        Thread.sleep(1000); //sleep 5s
                        v++;
                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }
                Log.e(log_label , "req timeout");
            }
        }).start();
    }

    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.grp_detail_exit:
                onClickExit();
                break;
            case R.id.grp_detail_edit:
                onClickEdit();
                break;
            case R.id.grp_detail_save:
                onClickOk();
                break;
            default:
                break;
        }
    }

    private void onClickEdit() {
        //not edit
        if(!edit_press) {
            tv_desc.setVisibility(View.GONE);
            et_desc.setVisibility(View.VISIBLE);
            et_visible.setVisibility(View.VISIBLE);
            et_change_name.setVisibility(View.VISIBLE);
            bt_save.setVisibility(View.VISIBLE);
            edit_press = true;
            btn_exit_group.setVisibility(View.GONE);
            return;
        }

        //edit pressed
        tv_desc.setVisibility(View.VISIBLE);
        et_desc.setVisibility(View.GONE);
        et_visible.setVisibility(View.GONE);
        et_change_name.setVisibility(View.GONE);
        bt_save.setVisibility(View.GONE);
        edit_press = false;
        return;
    }

    private void onClickOk() {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(GroupDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("确认修改");
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
                DoChangeAttr();
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

    private void DoChangeAttr() {
        String log_label = "onClickOk";
        boolean chged = false;
        //attr visible
        do {
            //if change?
            if(et_visible.getText() == null || et_visible.getText().toString()==null || et_visible.getText().toString().length()==0) {
                Log.i(log_label , "visible no change");
                break;
            }

            int v = Integer.parseInt(et_visible.getText().toString());
            //set visible
            if (v == 1 && group_visible == false) {
                chged = true;
                Log.d(log_label , "chg attr set visible!");
                String req = CSProto.CSChgGroupAttrReq(CSProto.GROUP_ATTR_VISIBLE , grp_id , 0 , "");
                AppConfig.SendMsg(req);
                //temp change
                group_visible = true;
                tv_visible.setText("是");
                break;
            }

            //set invisible
            if (v == 0 && group_visible == true) {
                chged = true;
                Log.d(log_label , "chg attr set invisible!");
                String req = CSProto.CSChgGroupAttrReq(CSProto.GROUP_ATTR_INVISIBLE , grp_id , 0 , "");
                AppConfig.SendMsg(req);
                //temp change
                group_visible = false;
                tv_visible.setText("否");
                break;
            }

        }while (false);

        //attr desc
        String src_desc = tv_desc.getText().toString();
        do {
            //if change?
            if(et_desc.getText()==null || et_desc.getText().toString()==null || et_desc.getText().toString().length()==0) {
                Log.i(log_label , "desc no change");
                break;
            }

            String new_desc = et_desc.getText().toString();
            if (src_desc != new_desc) {
                chged = true;
                Log.d(log_label, "chg attr set desc from:" + src_desc + " to:" + new_desc);
                String req = CSProto.CSChgGroupAttrReq(CSProto.GROUP_ATTR_DESC, grp_id, 0, new_desc);
                AppConfig.SendMsg(req);
                //temp change
                tv_desc.setText(new_desc);
            }
            break;
        } while (false);

        //attr change name
        do {
            //if change?
            if(et_change_name.getText()==null || et_change_name.getText().toString()==null || et_change_name.getText().toString().length()<=0) {
                Log.i(log_label , "grp name no change");
                break;
            }

            String new_grp_name = et_change_name.getText().toString();
            if(new_grp_name != group_name) {
                chged = true;
                Log.d(log_label , "chg attr grp_name fomr:" + group_name + " to:" + new_grp_name);
                String req = CSProto.CSChgGroupAttrReq(CSProto.GROUP_ATTR_GRP_NAME, grp_id, 0, new_grp_name);
                AppConfig.SendMsg(req);
                //temp change
                tv_grp_name.setText(new_grp_name);
            }
            break;
        } while (false);


        //reset
        onClickEdit();
        Log.d(log_label , "chged:" + chged);
        if(chged) {
            showProgressDialog(DIALOG_FROM_CHG_ATTR);
        }

    }


    private void onClickExit() {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(GroupDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        if(from_manage==1 && master_uid == AppConfig.UserUid) {   //group master
            bt_ok.setText("解散群组");
        } else
            bt_ok.setText("退出群组");
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
                QuitGroup();
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

    private void QuitGroup() {
        String req = CSProto.CSExitGroupReq(grp_id);
        AppConfig.SendMsg(req);
        showProgressDialog(DIALOG_FROM_QUIT_GROUP);
    }

    private void KickMember(long target_uid) {
        String req = CSProto.CSKickGroupReq(grp_id , target_uid);
        AppConfig.SendMsg(req);
        showProgressDialog(DIALOG_FROM_KICK_MEMBER);
    }

    private void AfterProcessDialog(final int from) {
        switch (from) {
            case DIALOG_FROM_QUIT_GROUP:
                //to new msg
                Intent intent = new Intent("NewMessage");
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                finish();
                break;
            case DIALOG_FROM_KICK_MEMBER:
                AfterKickMember();
                break;
            case DIALOG_FROM_CHG_ATTR:
                //onClickEdit();
                finish();
                break;
            default:
                //nothing to do
                break;
        }

    }

    private void AfterKickMember() {
        //del it
        if(del_member_pos < 0) {
            return;
        }
        list.remove(del_member_pos);
        adapter.notifyDataSetChanged();
        Log.d(log_label , "AfterProcessDialog del member pos:" + del_member_pos);
        del_member_pos = -1;

        //modify count
        member_count--;
        tv_member_count.setText(Integer.toString(member_count));
    }


    //@from:0 quit group; 1 kick member
    private void showProgressDialog(final int from) {
        progress_dialog = new ProgressDialog(GroupDetailActivity.this);

        progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress_dialog.setProgress(100);
        progress_dialog.setMessage("sending...");
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

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                // 第一次加载创建View，其余复用 View
                convertView = LayoutInflater.from(GroupDetailActivity.this).inflate(
                        R.layout.group_grid, null);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView
                        .findViewById(R.id.group_grid_img);
                holder.tv_name = (TextView) convertView.findViewById(R.id.group_grid_tv_name);
                holder.tv_uid = (TextView)convertView.findViewById(R.id.group_grid_tv_uid);
                // 打标签
                convertView.setTag(holder);

            } else {
                // 从标签中获取数据
                holder = (ViewHolder) convertView.getTag();
            }

            // 根据key值设置不同数据内容
            HashMap<String , Object> map = list.get(position);
            holder.tv_name.setText(map.get("name").toString());
            holder.tv_uid.setText(map.get("uid").toString());

            return convertView;
        }
    }

    private class ViewHolder {
        ImageView imageView;
        TextView tv_name;
        TextView tv_uid;
    }

    private void showFullImg() {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;

        if(bmp_img == null)
        {
            return;
        }

        //创建对话框
        img_dialog = new AlertDialog.Builder(GroupDetailActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(GroupDetailActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);
        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.grp_detail_img:
                if(from_manage == 1)
                    onImgClick(v);
                else
                    showFullImg();
                break;
            case R.id.tv_group_detail_save_img:
                SaveHeadImg();
                break;
            case R.id.iv_grp_detail_add_member:
                ShowSearchPanel();
                break;
            case R.id.btn_group_detail_do_search:
                SearchUserId();
                break;
            default:
                //nothing
        }
    }

    private void SearchUserId() {
        String log_label = "SearchUserId";
        if(grp_info == null)
            return;

        if(et_search_uid.getText() == null) {
            AppConfig.PrintInfo(this , "请输入用户ID");
            return;
        }

        String uid_v = et_search_uid.getText().toString();
        if(uid_v.length() <= 0) {
            AppConfig.PrintInfo(this , "UID为空");
            return;
        }

        long uid = Long.parseLong(uid_v);
        if(uid == AppConfig.UserUid) { //本人
            return;
        }
        //is member?
        int is_member = 0;
        for(int i=0; i<grp_info.members.size(); i++) {
            if(grp_info.members.get(i) == uid) {
                AppConfig.PrintInfo(this , "已在群组");
                Log.d(log_label , "uid:" + uid + " is member!");
                is_member = 1;
                break;
            }
        }

        //view
        Intent intent = new Intent("UserInfo");
        intent.putExtra("uid" , uid_v);
        intent.putExtra("from_manage" , 1);
        intent.putExtra("is_member" , is_member);
        intent.putExtra("invited_grp_id" , grp_id);
        intent.putExtra("invited_grp_name" , grp_info.grp_name);
        startActivity(intent);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    private void ShowSearchPanel() {
        btn_exit_group.setVisibility(View.GONE);
        if(show_search_panel) {
            rl_search_panel.setVisibility(View.GONE);
            show_search_panel = false;
            return;
        }

        rl_search_panel.setVisibility(View.VISIBLE);
        show_search_panel = true;
        return;
    }


    private void SaveHeadImg() {
        String log_label = "SaveHeadImg";
        if(bmp_img == null) {
            Log.e(log_label , "bmp_img null!");
            return;
        }

        //Get Random FileAddr
        file_serv_info = AppConfig.GetFileServ(-1);
        if(file_serv_info == null) {
            Log.e(log_label , "get file serv failed!");
            AppConfig.PrintInfo(this , "系统错误,上传失败");
            return;
        }

        //Save Head
        String upload_query = AppConfig.FileAddr2UploadUrl(file_serv_info.Addr);
        new UpdateHeadTask().execute(upload_query);

    }


    private void LoadHeadImg(String head_url) {
        String log_label = "LoadHeadImg";
        Log.d(log_label , "head_url:" + head_url);
        //check head url
        if(head_url==null || head_url.length()<=0) {
            Log.d(log_label , "head_url empty!");
            return;
        }

        //convert file name
        String head_file_name = AppConfig.Url2RealFileName(head_url);
        if(head_file_name==null || head_file_name.length()<=0) {
            Log.e(log_label , "convert real file failed! url:" + head_url);
            return;
        }


        //get query
        String head_query = AppConfig.ParseServerUrl(head_url);
        if(head_query==null || head_query.length()<=0) {
            Log.e(log_label , "parse head_url failed!");
            return;
        }
        Log.d(log_label , "head_query:" + head_query + " head_file_name:" + head_file_name);

        //load
        new LoadFileTask().execute(head_query , head_file_name);
    }


    /*
     * HTTPTASK
     */
    private class LoadFileTask extends AsyncTask<String, Void, Bitmap>
    {
        private String file_name = "";
        private boolean from_local = false;

        @Override
        protected Bitmap doInBackground(String... params)
        {
            InputStream input_stream = null;
            byte[] data = null;
            file_name = params[1];
            Bitmap bmp_img_tmp = null;

            try
            {
                /*load from local*/
                bmp_img_tmp = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_GRP_HEAD , 0 , file_name);
                if(bmp_img_tmp != null) {
                    Log.d(log_label , "load from local! file:" + file_name);
                    from_local = true;
                    return bmp_img_tmp;
                }

                /*load from net*/
                Log.d(log_label , "load from net! file:" + file_name);
                /*open connection*/
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10 * 1000);
                conn.setAllowUserInteraction(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    input_stream = conn.getInputStream();
                    data = AppConfig.ReadInput2Bytes(input_stream);
                    bmp_img_tmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                }

            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            return bmp_img_tmp;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(Bitmap bmp_img_loaded)
        {
//			progress_dialog.cancel();
            if(bmp_img_loaded == null)
                return;

            /*Parse BITMAP*/
            try
            {
                if(bmp_img_loaded != null)
                {
                    if(!from_local) { //from net will save to local
                        Log.i("download", "download img done");
                        AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_GRP_HEAD, grp_id, bmp_img_loaded, file_name);
                    }
                    bmp_img = bmp_img_loaded;
                    //iv is round
                    iv_grp_img.setImageDrawable(null); //clear old drawable
                    Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_img_loaded);
                    iv_grp_img.setImageBitmap(bmp_tmp);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        String log_label = "onActivityResult";
        Bitmap bmp_get = null;
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode != RESULT_OK) {
            Log.e(log_label , "result failed! code:" + requestCode);
            return;
        }
        Log.d(log_label , "result code:" + requestCode);

        Uri uri;
        switch(requestCode)
        {
            case AppConfig.FILE_SELECT_CODE:
            case AppConfig.USE_CAM_CODE:
                uri = intent.getData();
                if(uri == null) {
                    Log.e(log_label , "uri null");
                    break;
                }

                ContentResolver cr = getContentResolver();
                try
                {
                    InputStream is_tmp;
                    is_tmp = cr.openInputStream(uri);
                    /*
                    if(is_tmp.available() > 200*1024)
                    {
                        AppConfig.PrintInfo(this, "您的头像大小已经超过200KB.需要进行裁剪");
                        startPhotoZoom(intent.getData());
                        return;
                    }*/
                    Bitmap bmp_tmp = BitmapFactory.decodeStream(is_tmp);
                    is_tmp.close();


                    //check size
                    AppConfig.TryNotifyImgSize(this , bmp_tmp.getByteCount());
                    bmp_get = bmp_tmp;
                    /*
                    bmp_get = AppConfig.compressImage(bmp_tmp, AppConfig.HEAD_FILE_SIZE/1024);
                    if(bmp_get == null)
                    {
                        AppConfig.PrintInfo(this, "压缩图片出错，请重新选择");
                        return;
                    }*/
                    tv_group_save_img.setVisibility(View.VISIBLE);
                    tv_group_show_img.setVisibility(View.GONE);

                }
                catch (FileNotFoundException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
            case AppConfig.FILE_CROP_CODE:
                Bitmap bmp_tmp = intent.getParcelableExtra("data");
                bmp_get = AppConfig.compressImage(bmp_tmp, AppConfig.UPLOAD_FILE_SIZE/1024);
                break;
            default:
                break;
        }

        //修改之
        if(bmp_get == null)
            return;

        bmp_img = bmp_get;
        iv_grp_img.setImageBitmap(bmp_img);

    }



    private void onClickChooseImg()
    {
        btn_exit_group.setVisibility(View.GONE);
        AppConfig.PrintInfo(this, "选择图片");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try
        {
            startActivityForResult(intent, AppConfig.FILE_SELECT_CODE);
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            ex.printStackTrace();
        }
    }

    public void onImgClick(View v) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(GroupDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("选图");
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        bt_cancel.setText("预览");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                onClickChooseImg();
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
                showFullImg();
            }
        });
    }


    /*
     * Task
     */
    private class UpdateHeadTask extends AsyncTask<String, Void, String>
    {
        String log_label = "UpdateHeadTask";
        InputStream input_stream = null;
        @Override
        protected String doInBackground(String... params)
        {
            String result = null;
            if(file_serv_info == null) {
                AppConfig.PrintInfo(getBaseContext() , "系统错误 无法修改头像");
                return null;
            }

            try
            {
                /*open connection*/
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("url_type", AppConfig.URL_TYPE_GROUP_HEAD);
                map.put("uid", AppConfig.UserUid);
                map.put("grp_id", grp_id);
                map.put("tmp_id", 111);
                map.put("token", file_serv_info.Token);

                ArrayList<InputStream> is_list = new ArrayList<InputStream>();
                InputStream img_is = null;
                if(bmp_img.getByteCount() > AppConfig.MAX_IMG_SIZE) {
                    int scale = AppConfig.MAX_IMG_SIZE * 100 / bmp_img.getByteCount();
                    Log.i(log_label , "will compress scale:" + scale);
                    img_is = AppConfig.Bitmap2IS(bmp_img, scale);
                }
                else
                    img_is = AppConfig.Bitmap2IS(bmp_img , 100);

                //check stream
                if(img_is == null)
                    return null;
                is_list.add(img_is);

                input_stream = AppConfig.uploadFile(params[0] , map , is_list);
                if(input_stream == null)
                {
                    return null;
                }

                /*read input*/
                result = AppConfig.ReadInputStream(input_stream);
                input_stream.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(String result)
        {
//			progress_dialog.dismiss();
            if(progress_dialog != null)
                progress_dialog.dismiss();


            Log.d("upload img" , "result:" + result);
            if(result == null || result.length()<=0)
            {
                return ;
            }

            /*检查最开始char是否为0*/
            /*Parse JSON*/
            try
            {
                JSONTokener parser = new JSONTokener(result);

                //1.get root
                JSONObject root = (JSONObject)parser.nextValue();

                //2.get ret
                if(root.getInt("result") == CSProto.COMMON_RESULT_SUCCESS)
                {
                    AppConfig.PrintInfo(getBaseContext(), "修改成功");
                    //跳到个人主页 will recv new head_url
                    /*
                    Intent intent = new Intent("ChatPersonal");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                     */
                    finish();
                }
                else
                {
                    AppConfig.PrintInfo(getBaseContext(), "修改失败");
                }
                return;

            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }


        }

    }

}
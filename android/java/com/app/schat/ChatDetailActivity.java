package com.app.schat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class ChatDetailActivity extends Activity {
    private final int LOAD_FLAG_CREATE = 0;
    private final int LOAD_FLAG_HIS = 1;
    private final int LOAD_FLAG_NEW = 2;

    private TextView tv_chat_name = null;
    private ImageView iv_chat_grp;
    private ListView chatListView = null;
    private List<ChatEntity> chatList = null;
    private ChatAdapter chatAdapter = null;
    private String log_label = "chat_detail";
    private ClipboardManager cmb;
    //from intent
    private String group_name = null;
    private long grp_id = 0;
    private static Handler handler;
    private boolean check_chat = true;
    private AtomicBoolean in_loading = new AtomicBoolean(false);

    private Dialog dialog;
    private ProgressDialog progress_dialog;
    private Bitmap bit_map = null;
    private Bitmap bmp_img = null; //will upload image
    //private InputStream img_is = null;
    private int send_type = CSProto.CHAT_MSG_TYPE_TEXT;
    private FileServInfo file_serv_info = null;
    private int standard_item_img_px = 100;
    private long last_click_history = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_detail);
        tv_chat_name = (TextView)this.findViewById(R.id.chat_detail_tv_chat_name);
        chatListView = (ListView) this.findViewById(R.id.chat_detail_listview);
        iv_chat_grp = (ImageView)this.findViewById(R.id.chat_detail_iv_grp);

        /***RECVER*/
        Intent intent = this.getIntent();
        group_name = intent.getStringExtra("chat_name");
        grp_id = Long.parseLong(intent.getStringExtra("grp_id"));
        if(group_name==null || group_name.length()<=0)
        {
            AppConfig.PrintInfo(getBaseContext(), "对不起，聊天信息错误！");
            finish();
            return;
        }
        tv_chat_name.setText(group_name);

        if(UserInfo.IsInGroup(grp_id) == false) {
            AppConfig.PrintInfo(this , "您未加入该群组!");
            finish();
            return;
        }


        //handler
        handler = new Handler();

        //size
        standard_item_img_px = AppConfig.dip2px(this , AppConfig.CHAT_IMG_STANDARD_SIZE);
        Log.d(log_label , "img_standard_size:" + standard_item_img_px);

        //clip
        //clipboard
        cmb = (ClipboardManager)this.getSystemService(Context.CLIPBOARD_SERVICE);

        //Reset Window
        UserInfo.ResetGrpOldestMsgId(grp_id);

        /***List*/
        chatList = new ArrayList<ChatEntity>();
        chatAdapter = new ChatAdapter(this,chatList);
        chatListView.setAdapter(chatAdapter);
        chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                onLongClickChat(position);
                return true;
            }

        });
        chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                onClickChat(position);
            }
        });

        //load head
        GroupGroundItem g_cache = ChatInfo.GetGroupSnap(grp_id);
        if(g_cache!=null && g_cache.head_url != null && g_cache.head_url.length()>0) {
            LoadHeadImg(g_cache.head_url);
        } else
            iv_chat_grp.setBackgroundResource(R.drawable.group_main_white);

        //load init chat
        LoadChatItem(grp_id , LOAD_FLAG_CREATE);

        //check new chat
        check_chat = true;
        CheckChatMsg();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        check_chat = false;
    }

    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.chat_detail_iv_grp:
                //AppConfig.PrintInfo(this , "grp:" + group_name);
                Intent intent = new Intent("GroupDetail");
                intent.putExtra("grp_name", group_name);
                intent.putExtra("grp_id" , grp_id);
                startActivity(intent);
                this.overridePendingTransition(R.anim.in_from_right , R.anim.out_to_left);
                break;

            case R.id.chat_detail_iv_his:
                UserChatGroup grp_info = UserInfo.getChatGrp(grp_id);
                long oldest_msg_id = grp_info.oldest_msg_id;
                long curr_ts = AppConfig.CurrentUnixTime();
                if((curr_ts - last_click_history) < 3) {
                    AppConfig.PrintInfo(this , "拉取太频繁，请稍候再试");
                    return;
                }

                last_click_history = curr_ts;
                if(grp_info.local_last_msg_id>0 && grp_info.oldest_msg_id <= 1) {
                    AppConfig.PrintInfo(this , "没有更多");
                    Log.d(log_label , "click history: lastest_id:" + oldest_msg_id + " server last:" + grp_info.serv_last_msg_id);
                    return;
                }


                //query history
                //AppConfig.PrintInfo(this, "正在提交...用户名：" + UserName + " 密码：" + Password);
                progress_dialog = new ProgressDialog(ChatDetailActivity.this);
                progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress_dialog.setMessage("正在拉取...");
                progress_dialog.setIcon(R.drawable.ic_launcher_background);
                progress_dialog.setIndeterminate(false);
                progress_dialog.setCancelable(true);
                progress_dialog.show();
                TrytoQueryHistory();
                break;

        }
    }

    private void SetItemImageViewDefault(ImageView v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        //lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        //lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width = 30;
        lp.height = 40;
        v.setBackgroundResource(R.color.bg_white);
        v.setLayoutParams(lp);
    }

    private void SetItemImageViewFill(ImageView v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        //lp.width = 30;
        //lp.height = 40;
        v.setBackgroundResource(R.color.transparent);
        v.setLayoutParams(lp);
    }


    private  class ChatAdapter extends BaseAdapter
    {
        private Context context = null;
        private List<ChatEntity> chatList = null;
        private LayoutInflater inflater = null;
        private int COME_MSG = 0;
        private int TO_MSG = 1;

        public ChatAdapter(Context context,List<ChatEntity> chatList)
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
        public int getItemViewType(int position) {
            // 区别两种view的类型，标注两个不同的变量来分别表示各自的类型
            ChatEntity entity = chatList.get(position);
            if (entity.isComeMsg())
            {
                return COME_MSG;
            }else{
                return TO_MSG;
            }
        }

        @Override
        public int getViewTypeCount() {
            // 这个方法默认返回1，如果希望listview的item都是一样的就返回1，我们这里有两种风格，返回2
            return 2;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatHolder chatHolder = null;
            if (convertView == null)
            {
                chatHolder = new ChatHolder();
                if (chatList.get(position).isComeMsg())
                {
                    convertView = inflater.inflate(R.layout.chat_from_item, null);
                }
                else
                {
                    convertView = inflater.inflate(R.layout.chat_to_item, null);
                }
                chatHolder.timeTextView = (TextView) convertView.findViewById(R.id.chat_item_tv_time);
                chatHolder.contentTextView = (TextView) convertView.findViewById(R.id.chat_item_tv_content);
                chatHolder.userImageView = (ImageView) convertView.findViewById(R.id.chat_item_iv_user_image);
                chatHolder.userTextView = (TextView) convertView.findViewById(R.id.chat_item_tv_name);
                chatHolder.contentImageView = (ImageView)convertView.findViewById(R.id.chat_item_iv_content);
                convertView.setTag(chatHolder);
            }
            else
            {
                chatHolder = (ChatHolder)convertView.getTag();
            }

            //set display
            final ChatEntity entity = chatList.get(position);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.chat_item_tv_content:
                            // 创建一个剪贴数据集，包含一个普通文本数据条目（需要复制的数据）
                            ClipData clipData = ClipData.newPlainText(null, ((TextView)view).getText());
                            cmb.setPrimaryClip(clipData);
                            AppConfig.PrintInfo(ChatDetailActivity.this , "已复制到剪贴板");
                            //((TextView)view).setTextIsSelectable(true);
                            break;
                        case R.id.chat_item_iv_content:
                            if(entity.file_name==null || entity.file_name.length()<=0)
                                return;
                            /*
                            Drawable d = ((ImageView)view).getDrawable();
                            if(d == null)
                                return;
                            //Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
                            //Bitmap bitmap = ((BitmapDrawable) ((ImageView) view).getDrawable()).getBitmap();
                             */
                            showFullImg(entity.file_name);
                        default:
                            break;
                    }
                }
            };

            chatHolder.timeTextView.setText(entity.chatTime);
//            chatHolder.contentTextView.setText(chatList.get(position).getContent());
            //这里将所有_xx_替换为表情
            String src_content = entity.content;
            SpannableString sp = AppConfig.String2EmotionSp(getBaseContext() , getResources() , src_content);
            chatHolder.contentTextView.setText(sp);
            chatHolder.contentTextView.setTextColor(getResources().getColor(R.color.bg_midnight_blue_lighter));
            //chatHolder.contentTextView.setOnClickListener(listener);
            chatHolder.userImageView.setImageResource(entity.userImage);
            chatHolder.userTextView.setText(entity.userName);
            chatHolder.contentImageView.setImageDrawable(null); //clear bitmap to transparent
            //Image
            if(entity.chat_type == CSProto.CHAT_MSG_TYPE_IMG) {
                chatHolder.contentTextView.setVisibility(View.GONE);
                chatHolder.contentImageView.setVisibility(View.VISIBLE);
                if(entity.file_name != null && entity.file_name.length()>0) {
                    if(entity.bmp_content != null)
                        chatHolder.contentImageView.setImageBitmap(entity.bmp_content);
                    else
                        TryLoadChatImg(entity.msg_id , entity.content , entity.file_name);
                } else {
                    chatHolder.contentImageView.setImageDrawable(getResources().getDrawable(R.drawable.shape_chat_img_default));
                }
                /*
                if(entity.bmp_content != null) {
                    chatHolder.contentImageView.setImageBitmap(entity.bmp_content);
                } else {    //load img
                    if(entity.file_name!=null && entity.file_name.length()>0)
                        TryLoadChatImg(entity.msg_id , entity.content , entity.file_name);
                }*/
            } else {
                chatHolder.contentTextView.setVisibility(View.VISIBLE);
                chatHolder.contentImageView.setVisibility(View.GONE);
            }
            chatHolder.contentImageView.setOnClickListener(listener);
            if(entity.uid == UserInfo.SYS_UID) {
                chatHolder.contentTextView.setTextColor(getResources().getColor(R.color.bg_main_head_font_body));
                chatHolder.userTextView.setText("系统");
            }
            //special
            switch ((int) entity.chat_flag) {
                /*
                case (int) ChatInfo.CHAT_FLAG_CANCELLER:
                    Log.d(log_label , "canceller msg_id:" + entity.msg_id);
                    chatHolder.contentTextView.setVisibility(View.GONE);
                    chatHolder.contentImageView.setVisibility(View.GONE);
                    chatHolder.timeTextView.setVisibility(View.GONE);
                    chatHolder.userTextView.setVisibility(View.GONE);
                    break;
                 */
                case (int) ChatInfo.CHAT_FLAG_CANCELED:
                    //Log.d(log_label , "cancelled msg_id:" + entity.msg_id);
                    chatHolder.contentTextView.setTextColor(getResources().getColor(R.color.bg_main_head_font_body));
                    break;
                default:
                    //nothing
                    break;
            }


            return convertView;
        }

        private class ChatHolder
        {
            private TextView userTextView;
            private TextView timeTextView;
            private ImageView userImageView;
            private TextView contentTextView;
            private ImageView contentImageView;
        }

    }


    private void showFullImg(Bitmap bmp_img) {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;
        //Bitmap bmp_img;

        if(bmp_img == null)
        {
            AppConfig.PrintInfo(ChatDetailActivity.this, "还没有图片");
            return;
        }

        //创建对话框
        img_dialog = new AlertDialog.Builder(ChatDetailActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(ChatDetailActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);
        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }


    private void showFullImg(String file_name) {
        AlertDialog img_dialog;
        LayoutInflater inflater;
        View content_view;
        ImageView iv_large;
        Bitmap bmp_img;


        //创建对话框
        img_dialog = new AlertDialog.Builder(ChatDetailActivity.this).create();

        //加载布局并获得相关widget
        inflater = LayoutInflater.from(ChatDetailActivity.this);
        content_view = inflater.inflate(R.layout.dialog_photo_entry, null);
        iv_large = (ImageView) content_view.findViewById(R.id.iv_large_dig_photo_entry);

        //load from local
        bmp_img = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_CHAT , grp_id , file_name);
        if(bmp_img == null)
        {
            AppConfig.PrintInfo(ChatDetailActivity.this, "还没有图片");
            return;
        }

        iv_large.setImageBitmap(bmp_img);

        //显示dialog
        img_dialog.setView(content_view);
        img_dialog.show();
    }

    public void onEmojiClick(View v) {
        if(dialog == null)
            return;

        LinearLayout ll_face_panel = (LinearLayout)dialog.findViewById(R.id.ll_send_dialog_face_panel);
        if(ll_face_panel.getVisibility() == View.VISIBLE)
            ll_face_panel.setVisibility(View.GONE);
        else
            ll_face_panel.setVisibility(View.VISIBLE);
    }

    public void onImgClick(View v) {
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(ChatDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok);
        bt_ok.setText("图库");
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel);
        bt_cancel.setText("清除");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //create
                dialog.dismiss();
                AppConfig.PrintInfo(getBaseContext(), "选择图片");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT,null);
//				intent.setType("image/*");
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try
                {
                    startActivityForResult(Intent.createChooser(intent, "请选择一个要上传的文件"), AppConfig.FILE_SELECT_CODE);
                }
                catch (android.content.ActivityNotFoundException ex)
                {
                    // Potentially direct the user to the Market with a Dialog
                }
            }
        });

        //cancel
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
                dialog.dismiss();
                if(bmp_img == null)
                {
                    AppConfig.PrintInfo(ChatDetailActivity.this, "还没有选择图片");
                    return;
                }

                //reset
                bmp_img = null;

                //show text
                DialogDisplayText();
            }
        });
    }


    public void onImgClickBak(View v)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("选择图片来源");

        builder.setPositiveButton("图库", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                AppConfig.PrintInfo(getBaseContext(), "选择图片");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT,null);
//				intent.setType("image/*");
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try
                {
                    startActivityForResult(Intent.createChooser(intent, "请选择一个要上传的文件"), AppConfig.FILE_SELECT_CODE);
                }
                catch (android.content.ActivityNotFoundException ex)
                {
                    // Potentially direct the user to the Market with a Dialog
                }
            }
        });

        builder.setNegativeButton("相机", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                Intent intent = new Intent();

                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(intent, AppConfig.USE_CAM_CODE);
            }
        });

        //踩
        builder.setNeutralButton("清除", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {

                if(bmp_img == null)
                {
                    AppConfig.PrintInfo(ChatDetailActivity.this, "还没有选择图片");
                    return;
                }

                //reset
                bmp_img = null;

                //show text
                DialogDisplayText();
            }
        });

        builder.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        Bitmap bmp_get = null;
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode != RESULT_OK)
            return;


        switch(requestCode)
        {
            case AppConfig.FILE_SELECT_CODE:
            case AppConfig.USE_CAM_CODE:
                Uri uri = intent.getData();
                if(uri == null)
                    break;

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
//				AppConfig.PrintInfo(this, "Src:" + is_tmp.available()/1024 + "kb");
                    AppConfig.TryNotifyImgSize(this , is_tmp.available());
                    Bitmap bmp_tmp = BitmapFactory.decodeStream(is_tmp);
                    is_tmp.close();

                    //压缩
                    bmp_get = bmp_tmp;
                    //bmp_get = AppConfig.compressImage(bmp_tmp, AppConfig.HEAD_FILE_SIZE/1024);
                    if(bmp_get == null)
                    {
                        AppConfig.PrintInfo(this, "压缩图片出错，请重新选择");
                        return;
                    }


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

        //set bitmap
        bmp_img = bmp_get;
        DialogDisplayImage();
    }

    private void DialogDisplayImage() {
        if(bmp_img == null)
            return;
        if(dialog == null)
            return;

        //handle
        TextView tv_send = (TextView)dialog.findViewById(R.id.send_dialog_content);
        tv_send.setVisibility(View.GONE);

        ImageView iv_send = (ImageView) dialog.findViewById(R.id.send_dialog_iv_content);
        iv_send.setVisibility(View.VISIBLE);
        iv_send.setImageBitmap(bmp_img);
        send_type = CSProto.CHAT_MSG_TYPE_IMG;
    }

    private void DialogDisplayText() {
        if(dialog == null)
            return;

        //handle
        TextView tv_send = (TextView)dialog.findViewById(R.id.send_dialog_content);
        tv_send.setVisibility(View.VISIBLE);

        ImageView iv_send = (ImageView) dialog.findViewById(R.id.send_dialog_iv_content);
        iv_send.setVisibility(View.GONE);
        iv_send.setBackgroundResource(R.color.bg_bak_trans);
        send_type = CSProto.CHAT_MSG_TYPE_TEXT;
    }



    public void onClickSendMsg(View view)
    {
        if(dialog != null) {
            dialog.show();
            return;
        }
        dialog = new Dialog(this, R.style.popupDialog);
        dialog.setContentView(R.layout.send_dialog);
        dialog.setCancelable(true);

        TextView tv_send_title = (TextView)dialog.findViewById(R.id.send_dialog_title);
        //tv_send_title.setText("发送信息");


        //添加表情
        for(int i=0; i<AppConfig.SCHAT_FACE_COUNT; i++)
        {
            ImageView btn_iv_face = (ImageView)dialog.findViewById(R.id.iv_send_dialog_face_001+i);
            btn_iv_face.setOnClickListener(new View.OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    EditText et_send_content = (EditText)dialog.findViewById(R.id.send_dialog_content);
                    if(et_send_content.length()>=120)
                    {
                        AppConfig.PrintInfo(getBaseContext(), "您的输入超出120字");
                        return;
                    }

                    //基准
                    ImageView iv_face_grin = (ImageView)dialog.findViewById(R.id.iv_send_dialog_face_001);


                    int face_offset = v.getId()-iv_face_grin.getId();
                    String face_str = "_"+Integer.toString(face_offset)+"_";
                    SpannableString sp = new SpannableString(face_str);
                    Drawable d = getResources().getDrawable(R.drawable.face_001+face_offset);
                    d.setBounds(0, 0, AppConfig.dip2px(getBaseContext(), AppConfig.SCHAT_FACE_SIZE),
                            AppConfig.dip2px(getBaseContext(), AppConfig.SCHAT_FACE_SIZE));
                    ImageSpan span = new ImageSpan(d, AppConfig.SCHAT_FACE_ALIGN);
                    sp.setSpan(span, 0, face_str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    et_send_content.append(sp);

                }
            });
        }

        ImageView iv_show_more = (ImageView)dialog.findViewById(R.id.iv_send_dialog_more);
        iv_show_more.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                LinearLayout ll_more_panel = (LinearLayout)dialog.findViewById(R.id.ll_send_dialog_more);
                if(ll_more_panel.getVisibility() == View.GONE)
                    ll_more_panel.setVisibility(View.VISIBLE);
                else
                    ll_more_panel.setVisibility(View.GONE);
            }
        });
        //choose image and listener
        ImageView iv_choose_img = (ImageView)dialog.findViewById(R.id.iv_send_dialog_choose_pic);
        iv_choose_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onImgClick(view);
            }
        });

        //choose emoji and listener
        ImageView iv_default_emoji = (ImageView)dialog.findViewById(R.id.iv_send_dialog_default_emoji);
        iv_default_emoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEmojiClick(view);
            }
        });

        //select send_img and listener
        ImageView iv_show_send_img = (ImageView)dialog.findViewById(R.id.send_dialog_iv_content);
        iv_show_send_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bmp_img != null)
                    showFullImg(bmp_img);
            }
        });


        //send msg listener
        Button btn_send_ok = (Button)dialog.findViewById(R.id.send_dialog_ok);
        btn_send_ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                send_chat();
            }
        });

        dialog.show();
    }

    private void send_chat() {
        do {
            //send text
            if (send_type == CSProto.CHAT_MSG_TYPE_TEXT) {
                send_chat_text();
                break;
            }

            //send img
            if(send_type == CSProto.CHAT_MSG_TYPE_IMG) {
                send_chat_img();
                //reset
                DialogDisplayText();
                break;
            }

        }while (false);
    }


    private void send_chat_img() {
        String log_label = "send_chat_img";
        if(dialog == null) {
            Log.e(log_label , "dialog null");
            AppConfig.PrintInfo(this , "发送出错");
            return;
        }

        if(bmp_img == null) {
            Log.e(log_label , "img null");
            AppConfig.PrintInfo(this , "图片为空");
            return;
        }

        //upload
        //Get Random FileAddr
        file_serv_info = AppConfig.GetFileServ(-1);
        if(file_serv_info == null) {
            Log.e(log_label , "get file serv failed!");
            AppConfig.PrintInfo(this , "系统错误");
            return;
        }

        //Send Chat Img
        String upload_query = AppConfig.FileAddr2UploadUrl(file_serv_info.Addr);
        new UpdateChatImgTask().execute(upload_query);

        //hide
        dialog.dismiss();
    }


    private void send_chat_text() {
        String log_label = "send_chat_text";
        if(dialog == null) {
            Log.e(log_label , "dialog null");
            AppConfig.PrintInfo(this , "发送出错");
            return;
        }

        EditText et_send_content = (EditText)dialog.findViewById(R.id.send_dialog_content);
        //Recver
        if(grp_id <= 0)
        {
            AppConfig.PrintInfo(getBaseContext(), "群组信息出错，不能发送！");
            return;
        }
        //Sender
        if(!AppConfig.IsLogin())
        {
            AppConfig.PrintInfo(getBaseContext(), "对不起，您还没有登录，不能发送！");
            return;
        }
        //Content
        String content = et_send_content.getText().toString();
        if(content==null || content.length()<=0)
        {
            AppConfig.PrintInfo(getBaseContext(), "发送内容不能为空");
            return;
        }
        if(content.length()>=120)
        {
            AppConfig.PrintInfo(getBaseContext(), "您的输入超出120字");
            return;
        }

        //***关闭当前dialog
        dialog.dismiss();
        et_send_content.setText("");

        //开启发送DIALOG
        //Send
        String query = CSProto.CSSendChatReq(CSProto.CHAT_MSG_TYPE_TEXT , grp_id , content);
        AppConfig.SendMsg(query);
    }


    //flag == 0:older; 1:new
    private int LoadChatItem(long grp_id , int flag) {
        int ret = 0;
        if(AppConfig.db == null) {
            AppConfig.PrintInfo(this , "加载聊天失败");
            return ret;
        }
        if(!AppConfig.TryLockBool(in_loading)) {
            AppConfig.PrintInfo(this , "加载错误");
            return ret;
        }
        if(progress_dialog != null)
            progress_dialog.cancel();

        //dispatch
        try {
            switch (flag) {
                case LOAD_FLAG_CREATE:
                case LOAD_FLAG_HIS:
                    ret = LoadOlderItem(grp_id , flag);
                    break;
                case LOAD_FLAG_NEW:
                    ret = LoadNewItem(grp_id);
                    break;
                default:
                    break;
            }

            AppConfig.UnLockBool(in_loading);

        }catch (SQLException e) {
            Log.e(log_label , "LoadChatItem failed!");
            AppConfig.PrintInfo(this , "查询失败");
            e.printStackTrace();
            AppConfig.UnLockBool(in_loading);
        }
        return ret;
    }

    private int LoadOlderItem(long grp_id , int flag) throws SQLException {
        String log_label = "LoadOlderItem";
        long oldest_msg_id = UserInfo.GetGrpOldetMsgId(grp_id);
        long latest_msg_id = UserInfo.GetGrpLatestMsgId(grp_id);
        Log.d(log_label , "grp_id:" + grp_id + " flag:" + flag + " oldest:" + oldest_msg_id + " latest:" + latest_msg_id);
        int item_count = 0;
        long last_display_ts = 0;
        long snd_ts = 0;
        long query_start_msg_id = 0;

        long query_start = oldest_msg_id;
        if(flag==LOAD_FLAG_CREATE && (oldest_msg_id==latest_msg_id)) {
            query_start++; //when load by create curr read window size zero,oldest not loaded,should +1
        }
        if(latest_msg_id == 0) { //local empty!
            //local is zero should query from server
            Log.d(log_label , "local chat is empty! should fetch from server!");
            return 0;
        }

        String tab_name = DBHelper.grp_id_2_tab_name(grp_id);
        String sql =  "SELECT * FROM " + tab_name + " WHERE grp_id=" + grp_id + " and msg_id<" + query_start + " ORDER BY msg_id DESC limit 30"; //latest 40 chat
        Log.d(log_label , "flag:" + flag + " sql:" + sql);
        Cursor c = AppConfig.db.rawQuery(sql, null);
        if(c == null) {
            return 0;
        }
        while (c.moveToNext())
        {
            ChatEntity chat = new ChatEntity();

            //SET ENTITY
            int is_from = c.getInt(c.getColumnIndex("is_from"));
            if(is_from == 1)
                chat.setComeMsg(true);
            else
                chat.setComeMsg(false);
            long msg_id = c.getLong(c.getColumnIndex("msg_id"));
            chat.msg_id = msg_id;
            if(msg_id < oldest_msg_id) {
                oldest_msg_id = msg_id;
            }
            if(query_start_msg_id == 0) {
                query_start_msg_id = msg_id;
            }


            snd_ts = c.getLong(c.getColumnIndex("snd_time"));
            if(AppConfig.ChatTimeInSpan(snd_ts , last_display_ts) == false) {
                chat.chatTime = AppConfig.ConverUnixTime2MinStr(snd_ts);
                last_display_ts = snd_ts;
            }
            String enc_content = c.getString(c.getColumnIndex("chat_content"));
            String dec_content = DBHelper.DecryptChatContent(enc_content , AppConfig.user_local_des_key);
            if(TextUtils.isEmpty(dec_content)) {
                Log.e(log_label , "decrypt content failed! msg_id:" + msg_id);
                continue;
            }

            chat.content = dec_content;
            chat.userName = c.getString(c.getColumnIndex("snd_name"));
            chat.uid = c.getLong(c.getColumnIndex("snd_uid"));
            chat.chat_flag = c.getLong(c.getColumnIndex("flag"));
            chat.chat_type = c.getInt(c.getColumnIndex("chat_type"));
            //get file_name
            if(chat.chat_type == CSProto.CHAT_MSG_TYPE_IMG && chat.content.length()>0) {
                chat.file_name = AppConfig.Url2RealFileName(chat.content);
            }
            if(chat.chat_type > AppConfig.CURRENT_SUPPORT_CHAT_TYPE) {
                chat.content = "当前版本不支持该消息";
            }
            //Log.d(log_label , "msg_id:" + msg_id + " content:" + chat.content + " flag:" + chat.chat_flag);
            //Insert
            if(chat.chat_flag != ChatInfo.CHAT_FLAG_CANCELLER && chat.chat_flag != ChatInfo.CHAT_FLAG_DEL)
                chatList.add(0 , chat);
        }
        //modify
        item_count = c.getCount();
        c.close();

        //update some
        if(item_count > 0)
        {
            chatAdapter.notifyDataSetChanged();
            UserInfo.SetGrpOldetMsgId(grp_id , oldest_msg_id);
        }
        if (flag == LOAD_FLAG_CREATE) {
            chatListView.setSelection(chatList.size() - 1); //最后一行
        } else
            chatListView.setSelection(0);	//跳到第一行
        Log.d(log_label , "oldest_msg_id:" + oldest_msg_id);

        //check lost
        if(flag==LOAD_FLAG_CREATE && ((query_start_msg_id-oldest_msg_id + 1) != item_count)) { //should start-oldest + 1 == item_count
            Log.i(log_label , "some message lost! start:" + query_start_msg_id + " end:" + oldest_msg_id + " count:" + item_count);
            String query = CSProto.CSChatHistoryReq(grp_id , query_start_msg_id);
            AppConfig.SendMsg(query);
        }

        return item_count;
    }

    private void ResetCanceledMsg(long msg_id) {
        String log_label = "ResetCanceledMsg";
        //iter current list
        ChatEntity entity;
        for(int i=0; i<chatList.size(); i++) {
            entity = chatList.get(i);
            if(entity.msg_id == msg_id) {
                Log.d(log_label , "match msg_id:" + msg_id);
                entity.chat_type = CSProto.CHAT_MSG_TYPE_TEXT;
                entity.chat_flag = ChatInfo.CHAT_FLAG_CANCELED;
                entity.content = DBHelper.canceled_content;
                break;
            }
        }
    }

    private int LoadNewItem(long grp_id) throws SQLException {
        String log_label = "LoadOlderItem";
        UserChatGroup grp_info = UserInfo.getChatGrp(grp_id);
        long latest_msg_id = grp_info.local_last_msg_id;
        String latest_msg = "";
        long latest_ts = grp_info.local_last_ts;
        int chat_type = grp_info.local_last_chat_type;

        int item_count = 0;
        long last_display_ts = 0;
        long snd_ts = 0;

        boolean has_cancel_msg = false;
        String tab_name = DBHelper.grp_id_2_tab_name(grp_id);
        String sql =  "SELECT * FROM " + tab_name + " WHERE grp_id=" + grp_id + " AND msg_id>" + latest_msg_id
                + " ORDER BY msg_id ASC"; //new msg id
        Log.d(log_label , "sql:" + sql);
        int src_pos = chatList.size() - 1;
        if(src_pos < 0)
            src_pos = 0;
        Cursor c = AppConfig.db.rawQuery(sql, null);
        while (c.moveToNext())
        {
            ChatEntity chat = new ChatEntity();

            //SET ENTITY
            int is_from = c.getInt(c.getColumnIndex("is_from"));
            if(is_from == 1)
                chat.setComeMsg(true);
            else
                chat.setComeMsg(false);
            long msg_id = c.getLong(c.getColumnIndex("msg_id"));
            chat.chat_flag = c.getLong(c.getColumnIndex("flag"));
            chat.chat_type = c.getInt(c.getColumnIndex("chat_type"));
            snd_ts = c.getLong(c.getColumnIndex("snd_time"));
            chat.msg_id = msg_id;
            //chat.content = c.getString(c.getColumnIndex("chat_content"));
            String enc_content = c.getString(c.getColumnIndex("chat_content"));
            String dec_content = DBHelper.DecryptChatContent(enc_content , AppConfig.user_local_des_key);
            if(TextUtils.isEmpty(dec_content)) {
                Log.e(log_label , "decrypt content failed! msg_id:" + msg_id);
                dec_content = "...";
            }

            chat.content = dec_content;
            if(chat.chat_type > AppConfig.CURRENT_SUPPORT_CHAT_TYPE) {
                chat.content = "当前版本不支持该消息";
            }
            if(msg_id > latest_msg_id) {
                latest_msg_id = msg_id;
                latest_msg = chat.content;
                if(chat.chat_flag != ChatInfo.CHAT_FLAG_NORMAL) {
                    latest_msg = "...";
                }
                latest_ts = snd_ts;
                chat_type = chat.chat_type;
            }

            if(AppConfig.ChatTimeInSpan(snd_ts , last_display_ts) == false) {
                chat.chatTime = AppConfig.ConverUnixTime2MinStr(snd_ts);
                last_display_ts = snd_ts;
            }

            chat.userName = c.getString(c.getColumnIndex("snd_name"));
            chat.uid = c.getLong(c.getColumnIndex("snd_uid"));
            //get file_name
            if(chat.chat_type == CSProto.CHAT_MSG_TYPE_IMG && chat.content.length()>0) {
                chat.file_name = AppConfig.Url2RealFileName(chat.content);
            }

            //special
            if(chat.chat_flag == ChatInfo.CHAT_FLAG_CANCELLER && chat.content.length()>0) {
                long target_msg_id = Long.parseLong(chat.content);
                ResetCanceledMsg(target_msg_id);
            }

            //Insert
            if(chat.chat_flag != ChatInfo.CHAT_FLAG_CANCELLER)
                chatList.add(chat);
        }
        item_count = c.getCount();
        c.close();

        //modify
        if(item_count > 0)
        {
            chatAdapter.notifyDataSetChanged();
            //UserInfo.UpdateGrpLatestMsg(grp_id , latest_msg_id , latest_msg);
            grp_info.local_last_msg_id = latest_msg_id;
            grp_info.local_last_ts = latest_ts;
            grp_info.last_msg = latest_msg;
            grp_info.local_last_chat_type = chat_type;
            if(latest_msg_id >= grp_info.serv_last_msg_id) {
                grp_info.serv_last_msg_id = latest_msg_id;
            }
        }
        chatListView.setSelection(src_pos);	//src pos
        Log.d(log_label , "latest_msg_id:" + latest_msg_id);
        return item_count;
    }

    private void onLongClickChat(final int pos) {
        ChatEntity entity = chatList.get(pos);
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatDetailActivity.this);
        LayoutInflater inflater = LayoutInflater.from(ChatDetailActivity.this);
        View root_v = inflater.inflate(R.layout.normal_dialog , null);
        TextView tv_title = (TextView)root_v.findViewById(R.id.tv_normal_dialog_title);
        tv_title.setText("请选择");
        Button bt_ok = (Button)root_v.findViewById(R.id.bt_normal_dialog_ok); //del
        bt_ok.setText("删除");
        Button bt_mid = (Button)root_v.findViewById(R.id.bt_normal_dialog_mid); //recall
        if(AppConfig.UserUid == entity.uid && entity.chat_flag == ChatInfo.CHAT_FLAG_NORMAL) {
            bt_mid.setText("撤回");
            bt_mid.setVisibility(View.VISIBLE);
        }
        Button bt_cancel = (Button)root_v.findViewById(R.id.bt_normal_dialog_cancel); //copy
        bt_cancel.setText("复制");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setContentView(root_v);


        //ok
        bt_ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                //del chat
                dialog.dismiss();
                DelLocalChat(pos);
            }
        });

        //cancel
        bt_mid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //cancel chat
                dialog.dismiss();
                CancelMsg(pos);
            }
        });

        //copy
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //copy
                dialog.dismiss();
                CopyLocalChat(pos);
            }
        });

        /*
        //弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatDetailActivity.this);
        ChatEntity entity = chatList.get(pos);
        //builder.setIcon(R.drawable.group_main);
        builder.setTitle("请选择");


        //赞
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton)
            {
                //quit grup
                DelLocalChat(pos);
            }
        });

        //踩
        builder.setNegativeButton("复制", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                //nothing
                CopyLocalChat(pos);
            }
        });

        //cancel
        if(AppConfig.UserUid == entity.uid && entity.chat_flag == ChatInfo.CHAT_FLAG_NORMAL) {
            builder.setNeutralButton("撤回", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    CancelMsg(pos);
                }
            });
        }

        builder.create().show();*/
    }

    private void CancelMsg(int pos) {
        String log_label = "CancelMsg";
        ChatEntity entity = chatList.get(pos);
        //check
        if(entity.chat_flag != ChatInfo.CHAT_FLAG_NORMAL) {
            AppConfig.PrintInfo(this , "无法撤回此类消息");
            return;
        }

        if(entity.uid != AppConfig.UserUid) {
            Log.e(log_label , "not sender");
            return;
        }

        //query
        Log.i(log_label , "cancel grp_id:" + grp_id + " msg_id:" + entity.msg_id);
        String req = CSProto.CSUpdateChatReq(grp_id , entity.msg_id);
        AppConfig.SendMsg(req);
    }


    private void DelLocalChat(final int pos) {
        ChatEntity chat = (ChatEntity) chatList.get(pos);
        if(AppConfig.db == null) {
            AppConfig.PrintInfo(this , "删除失败");
        }
        DBHelper.DelChat(grp_id , chat.msg_id);
        //remove
        chatList.remove(pos);
        chatAdapter.notifyDataSetChanged();
    }

    private void CopyLocalChat(final int pos) {
        ChatEntity chat = (ChatEntity) chatList.get(pos);
        ClipData clipData = ClipData.newPlainText(null, chat.content);
        cmb.setPrimaryClip(clipData);
        AppConfig.PrintInfo(ChatDetailActivity.this , "已复制到剪贴板");
    }


    private void onClickChat(final int pos) {
        ChatEntity chat = (ChatEntity) chatList.get(pos);
        if(chat.uid == UserInfo.SYS_UID)
            return;
        //AppConfig.PrintInfo(getBaseContext(), "uid:" + chat.uid);
        Intent intent = new Intent("UserInfo");
        intent.putExtra("uid" , Long.toString(chat.uid));
        startActivity(intent);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    private void CheckChatMsg() {
        new Thread(new Runnable() {
            private long msg_id = 0;
            private UserChatGroup grp_info = UserInfo.getChatGrp(grp_id);
            @Override
            public void run() {
                Log.d(log_label , "CheckChatMsg starts");

                //wait result
                while (check_chat) {
                    try {
                        Thread.sleep(300);

                        //get lock sould not locked
                        if(!grp_info.new_msg.compareAndSet(true , false)) {
                            continue;
                        }

                        //new msg
                        Log.d(log_label , "CheckChatMsg new msg！");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                LoadChatItem(grp_id , LOAD_FLAG_NEW);
                            }
                        });


                    } catch (InterruptedException b) {
                        b.printStackTrace();
                    }
                }
                Log.d(log_label , "CheckChatMsg exit!");
            }
        }).start();
    }

    private void TrytoQueryHistory() {
        //First Load Local
        if(LoadChatItem(grp_id , LOAD_FLAG_HIS) > 0) {
            return;
        }

        //Query From Net
        new Thread(new Runnable() {
            String log_label = "QueryHistory";
            @Override
            public void run() {
                if(AppConfig.TryLockBool(AppConfig.chat_history_lock) == false) {
                    Log.e(log_label , "fail! lock fail!");
                    return;
                }
                long oldest_msg_id = UserInfo.GetGrpOldetMsgId(grp_id);
                long latest_msg_id = UserInfo.GetGrpLatestMsgId(grp_id);
                long query_msg_id = oldest_msg_id;
                do {
                    if(latest_msg_id==0) {
                        query_msg_id = -1;
                        Log.d(log_label , "latest_msg_id==0 query from latest! query_msg_id:" + query_msg_id);
                        break;
                    }

                    Log.d(log_label , "local has data! query from oldest! query_msg_id:" + query_msg_id);
                    break;
                    /*
                    if(oldest_msg_id == latest_msg_id) {    //oldest not loaded
                        oldest_msg_id++;    //should load oldest itself
                        break;
                    }*/
                } while (false);
                String req = CSProto.CSChatHistoryReq(grp_id , query_msg_id);
                AppConfig.SendMsg(req);
                int v = 0;
                //wait rsp
                while (v<=AppConfig.REQ_TIMEOUT) {
                    try {
                        if(AppConfig.chat_history_lock.get() == false) {
                            Log.i(log_label , "TrytoQueryHistory finish");
                            handler.post(new Runnable() {

                                @Override
                                public void run()
                                {
                                    LoadChatItem(grp_id , LOAD_FLAG_HIS);
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
                if(progress_dialog != null)
                    progress_dialog.cancel();
                Log.e(log_label , "TrytoQueryHistory req timeout");
            }
        }).start();
    }

    private void TryLoadChatImg(long msg_id , String chat_url , String file_name) {
        String log_label = "TryLoadChatImg";
        //arg check
        if(msg_id<=0 || chat_url==null || chat_url.length()<=0 || file_name==null || file_name.length()<=0) {
            Log.e(log_label , "arg illegal!");
            return;
        }

        //get query
        String img_query = AppConfig.ParseServerUrl(chat_url);
        if(img_query==null || img_query.length()<=0) {
            Log.e(log_label , "parse head_url failed!");
            return;
        }
        Log.d(log_label , "img_query:" + img_query + " file_name:" + file_name);

        //query
        new LoadFileTask().execute(img_query , Long.toString(msg_id) , file_name);
    }


    private void SetMsgImg(long msg_id , Bitmap bmp_content) {
        String log_label = "SetMsgImg";
        //check
        if(msg_id<=0 || bmp_content==null) {
            Log.e(log_label , "arg null");
            return;
        }

        //get
        ChatEntity entity = null;
        for(int i=0; i<chatList.size(); i++) {
            entity = chatList.get(i);
            if(entity.msg_id == msg_id) {
                Log.d(log_label , "found msg_id:" + msg_id);
                entity.bmp_content = bmp_content;
                chatAdapter.notifyDataSetChanged();
                break;
            }
        }

        return;
    }

    //compress src bitmap to standard size
    private Bitmap AdaptChatImage(Bitmap bmp_src) {
        String log_label = "AdaptChatImage";
        if(bmp_src == null) {
            return null;
        }
        //get larger size
        float larger = bmp_src.getWidth();
        if(bmp_src.getHeight() > larger) {
            larger = bmp_src.getHeight();
        }

        if(larger <= standard_item_img_px) {
            Log.d(log_label , "larger:" + larger + " standard:" + standard_item_img_px + " no need compress!");
            return bmp_src;
        }

        //scale
        float scale = larger / standard_item_img_px;
        Log.d(log_label , "scale:" + scale + " larger:" + larger + " standard:" + standard_item_img_px);
        Bitmap bmp_dest = AppConfig.compressBitmapbySize(bmp_src , scale);

        //change to 565
        if(bmp_dest.getByteCount() <= AppConfig.CHAT_IMG_COMPRESS_SIZE)
            return bmp_dest;

        Bitmap bmp_565 = AppConfig.compressImage565(bmp_dest);
        if(bmp_565 != null)
            return bmp_565;

        return bmp_dest;
    }


    /*
     * Load from Local or Load from Net
     * HTTPSTRINGYTASK
     */
    private class LoadFileTask extends AsyncTask<String, Void, Bitmap>
    {
        private long msg_id = 0;
        private String file_name = "";
        private boolean from_local = false;
        private String log_label = "LoadFileTask";
        @Override
        protected Bitmap doInBackground(String... params)
        {
            InputStream input_stream = null;
            byte[] data = null;
            Bitmap bmp_chat = null;
            msg_id = Long.parseLong(params[1]);
            file_name = params[2];

            try
            {
                /*load from local*/
                bmp_chat = AppConfig.ReadLocalImg(AppConfig.LOCAL_IMG_CHAT , grp_id , file_name);
                if(bmp_chat != null) {
                    Log.d(log_label , "load from local! file:" + file_name);
                    from_local = true;
                    return bmp_chat;
                }

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
                    bmp_chat = BitmapFactory.decodeByteArray(data, 0, data.length);
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

            return bmp_chat;
        }

        @Override
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         * result形式:
         * [01](&time:tag:author)*
         */
        protected void onPostExecute(Bitmap bmp_chat)
        {
//			progress_dialog.cancel();
            if(bmp_chat == null)
                return;

            /*Parse BITMAP*/
            try
            {
                //Bitmap bmp_head = BitmapFactory.decodeByteArray(data, 0, data.length);
                if(bmp_chat != null)
                {
                    if(!from_local) { //from net will save to local
                        Log.i(log_label, "download img done");
                        AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_CHAT, grp_id, bmp_chat, file_name);
                    }
                    //convert to small bitmap
                    Bitmap bmp_compressed = AdaptChatImage(bmp_chat);
                    if(bmp_compressed != null)
                        SetMsgImg(msg_id , bmp_compressed);
                    else
                        SetMsgImg(msg_id , bmp_chat);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }


    /*
     * Task
     */
    private class UpdateChatImgTask extends AsyncTask<String, Void, String>
    {
        String log_label = "UpdateChatImgTask";
        InputStream input_stream = null;
        @Override
        protected String doInBackground(String... params)
        {
            String result = null;
            if(file_serv_info == null) {
                Log.e(log_label , "file_serv_info null!");
                return null;
            }
            if(bmp_img==null) {
                Log.e(log_label , "bmp_img null!");
                return null;
            }

            //InputStream img_is = AppConfig.Bitmap2IS(bmp_img , 70);
            InputStream img_is = null;
            if(bmp_img.getByteCount() > AppConfig.MAX_IMG_SIZE) {
                int scale = AppConfig.MAX_IMG_SIZE * 100 / bmp_img.getByteCount();
                Log.i(log_label , "will compress scale:" + scale);
                img_is = AppConfig.Bitmap2IS(bmp_img, scale);
            }
            else
                img_is = AppConfig.Bitmap2IS(bmp_img , 100);
            if(img_is == null) {
                Log.e(log_label , "img_is null!");
                return null;
            }

            try
            {
                /*open connection*/
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("url_type", AppConfig.URL_TYPE_CHAT);
                map.put("uid", AppConfig.UserUid);
                map.put("grp_id", grp_id);
                map.put("tmp_id", 111);
                map.put("token", file_serv_info.Token);

                ArrayList<InputStream> is_list = new ArrayList<InputStream>();
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
                    AppConfig.PrintInfo(getBaseContext(), "发送成功");
                }
                else
                {
                    AppConfig.PrintInfo(getBaseContext(), "发送失败");
                }
                return;

            }
            catch(JSONException e)
            {
                e.printStackTrace();
            }


        }

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
        new LoadGroupHeadTask().execute(head_query , head_file_name);
    }


    /*
     * HTTPTASK
     */
    private class LoadGroupHeadTask extends AsyncTask<String, Void, Bitmap>
    {
        private String file_name = "";
        private boolean from_local = false;
        private String log_label = "LoadGroupHeadTask";
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
                        Log.i(log_label, "download img done");
                        AppConfig.saveLocalImg(AppConfig.LOCAL_IMG_GRP_HEAD, grp_id, bmp_img_loaded, file_name);
                    }
                    //iv is round
                    Bitmap bmp_compress = AdaptChatImage(bmp_img_loaded);
                    Bitmap bmp_tmp = AppConfig.toRoundHeadBitmap(bmp_compress);
                    iv_chat_grp.setImageBitmap(bmp_tmp);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }


}


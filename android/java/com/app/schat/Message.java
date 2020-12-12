package com.app.schat;

import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

class MessageItem {
    public int msg_id;
    public int msg_type;
    public String author;
    public String content;
    public long snd_ts;
    public long uid;
    public long grp_id;
    public String grp_name;
    public int read;
    public String extra_str;

    public MessageItem() {
        this.author = "";
        this.content = "";
        this.grp_name = "";
        this.extra_str = "";
        this.read = 0;
    }
}

class MessageCache {
    public int msg_id;
    public AtomicBoolean lock;
    public ArrayList<MessageItem> msg_list;

    public MessageCache() {
        this.lock = new AtomicBoolean(false);
        this.msg_list = new ArrayList<>();
        this.msg_id = 10086;
    }

}


public class Message {
    public static final String MESSAGE_SYS_AUTHOR = "系统";
    public static final int MESSAGE_TYPE_APPLY_GROUP = 1; //apply into group
    public static final int MESSAGE_TYPE_ENTER_GRPUP = 2; //member enter group
    public static final int MESSAGE_TYPE_LEAVE_GROUP = 3; //member leave group
    public static final int MESSAGE_TYPE_EXIT_GROUP = 4;  //exit group
    public static final int MESSAGE_TYPE_KICK_GROUP =  5; //kicked out from group

    public static boolean UnReadedMsg() {
        String log_label = "UnReadedMsg";

        //query from db
        String tab_name = DBHelper.MESSAGE_TB_NAME;
        String sql =  "SELECT count(*) FROM " + tab_name + " WHERE read=0"; //read = 0
        Log.d(log_label , "sql:" + sql);
        Cursor c;
        try {
            c = AppConfig.db.rawQuery(sql, null);
        }catch (SQLException e) {
            Log.e(log_label , "sql error! sql:" + sql);
            e.printStackTrace();
            return false;
        }
        if(c == null) {
            return false;
        }
        c.moveToFirst();
        int total_count = c.getInt(0);
        c.close();

        //return
        Log.d(log_label , "unreaded:" + total_count);
        if(total_count > 0)
            return true;
        return false;
    }

    public static ArrayList<MessageItem> fetch_msg_item() {
        String log_label = "fetch_msg_item";
        ArrayList<MessageItem> dst_list = new ArrayList<>();
        if(AppConfig.db == null) {
            Log.i(log_label , "db not ready!");
            return dst_list;
        }
        //query from db
        String tab_name = DBHelper.MESSAGE_TB_NAME;
        String sql =  "SELECT * FROM " + tab_name + " ORDER BY msg_id DESC limit 40"; //latest 40 chat
        Log.d(log_label , "sql:" + sql);
        Cursor c;
        try {
            c = AppConfig.db.rawQuery(sql, null);
        }catch (SQLException e) {
            Log.e(log_label , "sql error! sql:" + sql);
            e.printStackTrace();
            return dst_list;
        }
        if(c == null) {
            return dst_list;
        }
        while (c.moveToNext())
        {
            MessageItem item = new MessageItem();

            //FILL INFO
            item.msg_id = c.getInt(c.getColumnIndex("msg_id"));
            item.msg_type = c.getInt(c.getColumnIndex("msg_type"));
            item.author = c.getString(c.getColumnIndex("author"));
            item.content = c.getString(c.getColumnIndex("content"));
            item.snd_ts = c.getLong(c.getColumnIndex("snd_ts"));
            item.uid = c.getLong(c.getColumnIndex("uid"));
            item.grp_id = c.getLong(c.getColumnIndex("grp_id"));
            item.grp_name = c.getString(c.getColumnIndex("grp_name"));
            item.read = c.getInt(c.getColumnIndex("read"));
            item.extra_str = c.getString(c.getColumnIndex("extra_str"));

            //Log.d(log_label , "msg_id:" + item.msg_id + " content:" + item.content + " read:" + item.read);
            //Insert
            //chatList.add(chat);
            dst_list.add(0 , item); //asc
        }
        c.close();
        return dst_list;
    }


    public static boolean add_msg_item(MessageItem item) {
        String log_label = "add_msg_item";
        if(item == null) {
            Log.d(log_label , "arg nil!");
            return false;
        }
        if(AppConfig.db == null) {
            Log.i(log_label , "db not ready!");
            return false;
        }

        //store
        String tb_name = DBHelper.MESSAGE_TB_NAME;
        String sql = "insert into " + tb_name + "(msg_type, author, content, snd_ts, uid, grp_id, grp_name,read,extra_str) " +
                "values(" + item.msg_type + ",'" + item.author + "','" + item.content + "'," + item.snd_ts + "," + item.uid + "," +
                item.grp_id + ",'" + item.grp_name + "'," + item.read + ",'" + item.extra_str + "')";
        Log.d(log_label, "sql:" + sql);
        try {
            AppConfig.db.execSQL(sql);
        }catch (SQLException e) {
            Log.e(log_label , "error! sql:" + sql);
            e.printStackTrace();
        }
        return true;
    }


    public static boolean del_msg_item(int msg_id) {
        String log_label = "del_msg_item";

        //del
        String sql = "DELETE FROM " + DBHelper.MESSAGE_TB_NAME + " WHERE msg_id=" + msg_id;
        try {
            AppConfig.db.execSQL(sql);
        }catch (SQLException e) {
            Log.e(log_label , "error! sql:" + sql);
            e.printStackTrace();
        }

        return true;
    }

    public static boolean read_msg_item(int msg_id) {
        String log_label = "read_msg_item";
        if(msg_id < 0) {
            Log.d(log_label , "arg illegal!");
            return false;
        }

        //update
        String sql = "UPDATE " + DBHelper.MESSAGE_TB_NAME + " SET read=1 WHERE msg_id=" + msg_id;
        try {
            AppConfig.db.execSQL(sql);
        }catch (SQLException e) {
            Log.e(log_label , "error! sql:" + sql);
            e.printStackTrace();
        }
        return true;
    }



}

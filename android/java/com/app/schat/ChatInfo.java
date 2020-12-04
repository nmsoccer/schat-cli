package com.app.schat;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class GroupGroundItem {
    public long grp_id;
    public String grp_name;
    public int mem_count;
    public String desc;
    public String head_url;

    public GroupGroundItem() {
        this.grp_name = "";
        this.desc = "";
        this.head_url = "";
    }

}

class GroupSnapCache {
    //public AtomicBoolean lock;
    public ConcurrentHashMap<Long , GroupGroundItem> item_map;

    public GroupSnapCache() {
        //this.lock = new AtomicBoolean(false);
        this.item_map = new ConcurrentHashMap<>();
    }
}


class GroundGroup {
    public AtomicBoolean in_proc;
    public int fetch_count;
    public HashMap<Long , GroupGroundItem> item_map;

    public GroundGroup() {
        this.in_proc = new AtomicBoolean(false);
        this.fetch_count = 0;
        this.item_map = new HashMap<Long , GroupGroundItem>();
    }
}


class ChatMsg {
    public int chat_type;
    public long msg_id;
    public long grp_id;
    public long snd_uid;
    public String snd_name;
    public long snd_ts;
    public String sn_content;
    public long chat_flag;
}

class RecvChatNew {
    public AtomicBoolean new_chat;

    public RecvChatNew() {
        this.new_chat = new AtomicBoolean(false);
    }
}

class ChatGroup {
    public long  grp_id;
    public String  grp_name;
    public long  master;
    public long latest_msg_id;
    public long create_ts;
    public int mem_count;
    public ArrayList<Long> members;
    public boolean visible;
    public String desc;
    public String head_url;

    public ChatGroup() {
        this.grp_name = "";
        this.visible = false;
        this.desc = "";
        this.members = new ArrayList<>();
    }
}

class ChatGroupCache {
    public AtomicBoolean lock;
    public HashMap<Long , ChatGroup> grp_map;

    public ChatGroupCache() {
        this.grp_map = new HashMap<>();
        this.lock = new AtomicBoolean(false);
    }
}

public class ChatInfo {
    public static final int UPT_CHAT_DEL = 0 ;//del chat
    public static final int UPT_CHAT_CANCEL = 1; //cancel chat

    public static final long CHAT_FLAG_NORMAL = 0 ;//normal chat
    public static final long CHAT_FLAG_DEL = 1; //deleted
    public static final long CHAT_FLAG_CANCELED = 2; //canceled chat
    public static final long CHAT_FLAG_CANCELLER = 3; //cancel master chat


    public static boolean AddChatGroup(ChatGroup grp_info) {
        String log_label = "AddChatGroup";
        ChatGroupCache g_cache = AppConfig.chat_group_cache;
        if(g_cache == null) {
            return false;
        }

        //lock
        if(AppConfig.TryLockBool(g_cache.lock) == false) {
            Log.e(log_label , "lock failed!");
            return false;
        }

        g_cache.grp_map.put(grp_info.grp_id , grp_info);
        AppConfig.UnLockBool(g_cache.lock);
        Log.i(log_label , "done! grp_id:" + grp_info.grp_id);
        return true;
    }



    public static ChatGroup GetChatGroup(long grp_id) {
        String log_label = "GetChatGroup";
        ChatGroupCache g_cache = AppConfig.chat_group_cache;
        if(g_cache == null) {
            return null;
        }

        //lock
        if(AppConfig.TryLockBool(g_cache.lock) == false) {
            Log.e(log_label , "lock failed!");
            return null;
        }

        ChatGroup grp_info = g_cache.grp_map.get(grp_id);
        AppConfig.UnLockBool(g_cache.lock);
        return grp_info;
    }

    public static void AddGroupMember(long grp_id , long member_uid) {
        String log_label = "AddGroupMember";
        ChatGroup grp_info = GetChatGroup(grp_id);
        if(grp_info == null) {
            Log.d(log_label , "grp_info nil! grp_id:" + grp_id);
            return;
        }

        //check exist
        boolean exist = false;
        ArrayList<Long> members = grp_info.members;
        for(int i=0; i<members.size(); i++) {
            if(members.get(i) == member_uid) {
                exist = true;
                break;
            }
        }
        if(exist) {
            Log.d(log_label , "is exist! grp_id:" + grp_id + " mem_uid:" + member_uid);
            return;
        }

        grp_info.members.add(member_uid);
        grp_info.mem_count++;
        Log.d(log_label , "add member! grp_id:" + grp_id + " mem_uid:" + member_uid + " count:" + grp_info.mem_count);
    }

    public static void DelGroupMember(long grp_id , long member_uid) {
        String log_label = "DelGroupMember";
        ChatGroup grp_info = GetChatGroup(grp_id);
        if(grp_info == null) {
            Log.d(log_label , "grp_info nil! grp_id:" + grp_id);
            return;
        }

        //check exist
        int pos = -1;
        ArrayList<Long> members = grp_info.members;
        for(int i=0; i<members.size(); i++) {
            if(members.get(i) == member_uid) {
                pos = i;
                break;
            }
        }
        if(pos < 0) {
            Log.d(log_label , "not exist! grp_id:" + grp_id + " mem_uid:" + member_uid);
            return;
        }

        grp_info.members.remove(pos);
        grp_info.mem_count--;
        Log.d(log_label , "add member! grp_id:" + grp_id + " mem_uid:" + member_uid + " count:" + grp_info.mem_count);
    }

    public static void AddGroupSnap(ArrayList<GroupGroundItem> item_list) {
        String log_label = "AddGroupSnapList";
        if(AppConfig.grp_snap_cache == null) {
            Log.e(log_label , "cache nil!");
            return;
        }
        if(item_list==null || item_list.size()<=0) {
            return;
        }

        GroupSnapCache cache = AppConfig.grp_snap_cache;

        //handle
        ConcurrentHashMap<Long , GroupGroundItem> item_map = cache.item_map;
        GroupGroundItem item;
        for(int i=0; i<item_list.size(); i++) {
            item = item_list.get(i);
            item_map.put(item.grp_id , item);
        }
        return;
    }

    public static void AddGroupSnap(GroupGroundItem item) {
        String log_label = "AddGroupSnap";
        if(AppConfig.grp_snap_cache == null) {
            Log.e(log_label , "cache nil!");
            return;
        }
        if(item==null) {
            return;
        }

        GroupSnapCache cache = AppConfig.grp_snap_cache;

        //handle
        ConcurrentHashMap<Long , GroupGroundItem> item_map = cache.item_map;
        item_map.put(item.grp_id , item);
        return;
    }


    public static ArrayList<GroupGroundItem> GetGroupSnapList() {
        String log_label = "GetGroupSnapList";
        ArrayList<GroupGroundItem> item_list = new ArrayList<>();
        if(AppConfig.grp_snap_cache == null) {
            Log.e(log_label , "cache nil!");
            return item_list;
        }

        GroupSnapCache cache = AppConfig.grp_snap_cache;

        //handle
        ConcurrentHashMap<Long , GroupGroundItem> item_map = cache.item_map;
        for(GroupGroundItem item : item_map.values()) {
            item_list.add(item);
        }
        return item_list;
    }

    public static GroupGroundItem GetGroupSnap(long grp_id) {
        String log_label = "GetGroupSnap";
        if(AppConfig.grp_snap_cache == null) {
            Log.e(log_label , "cache nil!");
            return null;
        }

        GroupSnapCache cache = AppConfig.grp_snap_cache;

        //handle
        ConcurrentHashMap<Long , GroupGroundItem> item_map = cache.item_map;
        GroupGroundItem item = item_map.get(grp_id);
        return item;
    }

    public static void DelGroupSnap(long grp_id) {
        String log_label = "DelGroupSnap";
        if(AppConfig.grp_snap_cache == null) {
            Log.e(log_label , "cache nil!");
            return;
        }

        GroupSnapCache cache = AppConfig.grp_snap_cache;

        //handle
        ConcurrentHashMap<Long , GroupGroundItem> item_map = cache.item_map;
        if(item_map.containsKey(grp_id)) {
            item_map.remove(grp_id);
            Log.i(log_label , "remove grp_id:" + grp_id);
        } else {
            Log.i(log_label , "not exist grp_id:" + grp_id);
        }
        return;
    }





}

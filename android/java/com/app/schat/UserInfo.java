package com.app.schat;
//CS-PROTO refer https://github.com/nmsoccer/schat/proto/cs/user_info.go

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class UserBasic {
    public long uid;
    public String name;
    public String addr;
    public int sex;
    public int level;
    public String head_url;
    public String head_file_name;

    public UserBasic() {
        this.name = "";
        this.addr = "";
        this.head_url = "";
        this.head_file_name = "";
    }
}

/*
type UserChatGroup struct {
	GroupId int64
	GroupName string
	LastMsgId int64 //last readed
	EnterTs   int64 //enter ts
}

type UserChatInfo struct {
	AllGroup int32
	AllGroups map[int64] *UserChatGroup
	MasterGroup int32
	MasterGroups map[int64] bool
}
 */

class UserChatGroup {
    public long serv_last_msg_id; //server last_msg_id
    public long local_last_msg_id; //local last_msg_id
    public long oldest_msg_id; //curr oldest msg id
    public int local_last_chat_type;
    public long grp_id;
    public String grp_name;
    public long enter_ts;
    public String last_msg;
    public AtomicBoolean new_msg;
    public long local_last_ts;


    public UserChatGroup() {
        this.grp_id = 0;
        this.local_last_msg_id = 0;
        this.enter_ts = 0;
        this.grp_name = "";
        this.last_msg = "";
        this.oldest_msg_id = 0; //not inited
        this.serv_last_msg_id = 0;
        this.new_msg = new AtomicBoolean(false);
        this.local_last_chat_type = CSProto.CHAT_MSG_TYPE_TEXT;
    }

}

class UserChatInfo {
    public int all_group;
    public ConcurrentHashMap<Long, UserChatGroup> all_groups;
    public int master_group;
    public ConcurrentHashMap<Long, Boolean> master_groups;

    public UserChatInfo() {
        this.all_group = 0;
        this.all_groups = new ConcurrentHashMap<>();
        this.master_group = 0;
        this.master_groups = new ConcurrentHashMap<>();
    }

}


class UserDetail {
    public int exp;
    public UserChatInfo chat_info;
    public String desc;
    public String des_key;

    public UserDetail() {
        this.chat_info = new UserChatInfo();
        this.desc = "";
    }
}

class UserProfile {
    public long  uid;
    public String name;
    public String addr;
    public short sex;
    public int level;
    public String head_url;
    public String head_file_name;
    public String user_desc;

    public UserProfile() {
        this.name = this.addr = this.head_url = this.head_file_name = "";
        this.user_desc = "";
    }

}

class UserProfileCache {
    //public AtomicBoolean lock;
    public ConcurrentHashMap<Long , UserProfile> user_map;

    public UserProfileCache() {
        //this.lock = new AtomicBoolean(false);
        this.user_map = new ConcurrentHashMap<>();
    }
}


public class UserInfo {
    public static final int SYS_UID = 0;
    public String account_name;
    public UserBasic basic;
    public UserDetail detail;
    public AtomicBoolean lock;
    public static int MAX_CREATE_GROUP_COUNT = 10; //default

    public UserInfo() {
        this.account_name = "";
        this.basic = new UserBasic();
        this.detail = new UserDetail();
        this.lock = new AtomicBoolean(false);
    }



    //static functions
    public static int MasterGroupCount() {
        if(AppConfig.user_info == null) {
            return -1;
        }

        return AppConfig.user_info.detail.chat_info.master_group;
    }


    public static UserChatGroup getChatGrp(long grp_id) {
        String log_label = "getChatGrp";
        if(AppConfig.user_info == null) {
            return null;
        }
        UserInfo user_info = AppConfig.user_info;


        //handle
        UserChatGroup grp_info = null;
        do {
            UserChatInfo chat_info = user_info.detail.chat_info;
            if (chat_info == null) {
                break;
            }
            if (chat_info.all_group <= 0) {
                break;
            }
            grp_info = chat_info.all_groups.get(grp_id);
            break;
        } while (false);

        return grp_info;
    }

    public static boolean IsInGroup(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return false;
        }
        return true;
    }

    public static void EnterGroup(long grp_id , String grp_name , boolean master) {
        String log_label = "EnterGroup";
        //enter
        if(AppConfig.user_info == null) {
            Log.e(log_label , "user nil");
            return;
        }
        UserInfo user_info = AppConfig.user_info;

          //chat info
        UserChatInfo chat_info = user_info.detail.chat_info;
        if(chat_info == null) {
            Log.e(log_label , "chat info nil");
            return;
        }

          //all groups
        ConcurrentHashMap<Long, UserChatGroup> all_groups = chat_info.all_groups;
        if(all_groups == null) {
            Log.e(log_label , "all_groups nil");
            return;
        }

        do {
            //create grp dir
            AppConfig.CreateGroupFileDir(grp_id);

            //grp
            UserChatGroup grp_info = all_groups.get(grp_id);
            if (grp_info == null) {
                grp_info = new UserChatGroup();
                grp_info.grp_name = grp_name;
                grp_info.grp_id = grp_id;
                all_groups.put(grp_id, grp_info);
                chat_info.all_group++;
                Log.i(log_label, "EnterGroup all_group! grp_id:" + grp_id + " all_group:" + chat_info.all_group);
            }

            //master
            if (!master) {
                break;
            }
            ConcurrentHashMap<Long, Boolean> master_groups = chat_info.master_groups;
            if (master_groups == null) {
                Log.e(log_label, "EnterGroup master_groups nil");
                break;
            }
            if (master_groups.get(grp_id) == null) {
                master_groups.put(grp_id, true);
                chat_info.master_group++;
                Log.i(log_label, "EnterGroup master_group! grp_id:" + grp_id + " master_group:" + chat_info.master_group);
            }
            break;

        } while (false);
        return;
    }

    public static void LeaveGroup(long grp_id) {
        String log_label = "LeaveGroup";
        //enter
        if(AppConfig.user_info == null) {
            Log.e(log_label , " user nil");
            return;
        }
        UserInfo user_info = AppConfig.user_info;

        //chat info
        UserChatInfo chat_info = user_info.detail.chat_info;
        if(chat_info == null) {
            Log.e(log_label , " chat info nil");
            return;
        }

        //all groups
        ConcurrentHashMap<Long, UserChatGroup> all_groups = chat_info.all_groups;
        ConcurrentHashMap<Long , Boolean> master_grups = chat_info.master_groups;
        if(all_groups == null || master_grups==null) {
            Log.e(log_label , " groups nil");
            return;
        }

        do {
            //remove
            if (all_groups.containsKey(grp_id)) {
                all_groups.remove(grp_id);
                chat_info.all_group--;
                Log.d(log_label, " remove from all group. grp_id:" + grp_id + " count:" + chat_info.all_group);
            }

            if (master_grups.containsKey(grp_id)) {
                master_grups.remove(grp_id);
                chat_info.master_group--;
                Log.d(log_label, " remove from master group. grp_id:" + grp_id + " count:" + chat_info.master_group);
            }
            break;
        } while (false);
        return;
    }

    /*
    public static void UpdateGrpLatestMsg(long grp_id , long msg_id , String msg) {
        String log_label = "UpdateGrpLatestMsg";
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return;
        }
        if(grp_info.local_last_msg_id <= msg_id) {
            grp_info.local_last_msg_id = msg_id;
            if(msg == null) {
                msg = "";
            }
            grp_info.last_msg = msg;
            Log.d(log_label , "id:" + msg_id + " grp_id:" + grp_id + " msg:" + msg);
        }
        if(grp_info.local_last_msg_id >= grp_info.serv_last_msg_id) {
            grp_info.serv_last_msg_id = grp_info.local_last_msg_id;
        }
    }*/

    public static void SetGrpServerLatestId(long grp_id , long msg_id) {
        String log_label = "SetGrpServerLatestId";
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return;
        }
        if(grp_info.serv_last_msg_id <= msg_id) {
            Log.d(log_label , "server_msgid from:" + grp_info.serv_last_msg_id + " to:" + msg_id);
            grp_info.serv_last_msg_id = msg_id;
        }
    }

    public static void setGrpNewMsgStat(long grp_id , boolean stat) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return;
        }
        grp_info.new_msg.set(stat);
    }

    public static boolean getGrpNewMsgStat(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return false;
        }
        return grp_info.new_msg.get();
    }


    public static String GetGrpLatestMsg(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return "";
        }
        return grp_info.last_msg;
    }
    public static long GetGrpLatestMsgId(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return 0;
        }
        return grp_info.local_last_msg_id;
    }


    public static long GetGrpServerLatestMsgId(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return 0;
        }
        return grp_info.serv_last_msg_id;
    }

    public static long GetGrpOldetMsgId(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return 0;
        }
        return grp_info.oldest_msg_id;
    }
    public static boolean SetGrpOldetMsgId(long grp_id , long oldest_msg_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return false;
        }
        if(grp_info.oldest_msg_id == 0 || grp_info.oldest_msg_id >= oldest_msg_id) {
            grp_info.oldest_msg_id = oldest_msg_id;
            return true;
        }
        return false;
    }
    public static boolean ResetGrpOldestMsgId(long grp_id) {
        UserChatGroup grp_info = getChatGrp(grp_id);
        if(grp_info == null) {
            return false;
        }
        grp_info.oldest_msg_id = grp_info.local_last_msg_id;
        return false;
    }

    public static boolean SetUserProfiles(ArrayList<UserProfile> profiles) {
        String log_label = "SetUserProfiles";
        UserProfileCache u_cache = AppConfig.user_profile_cache;
        if(u_cache == null) {
            Log.e(log_label , "cache nil");
            return false;
        }

        //add
        UserProfile profile;
        long uid;
        for(int i=0; i<profiles.size(); i++) {
            profile = profiles.get(i);
            uid = profile.uid;

            //convert head_file_name
            if(profile.head_url!=null && profile.head_url.length()>0) {
                profile.head_file_name = AppConfig.Url2RealFileName(profile.head_url);
            }

            u_cache.user_map.put(uid , profile);
            Log.d(log_label , "add uid:" + uid);
        }

        return true;
    }

    public static HashMap<Long , UserProfile> GetUserProfiles(ArrayList<Long> uids) {
        String log_label = "GetUserProfiles";
        UserProfileCache u_cache = AppConfig.user_profile_cache;
        if(u_cache == null) {
            Log.e(log_label , "cache nil");
            return null;
        }

        //add
        HashMap<Long , UserProfile> maps = new HashMap<>();
        UserProfile profile;
        long uid;
        for(int i=0; i<uids.size(); i++) {
            uid = uids.get(i);
            profile = u_cache.user_map.get(uid);
            if(profile != null) {
                maps.put(uid, profile);
                Log.d(log_label, "get uid:" + uid);
            }
        }
        return maps;
    }

}

package com.app.schat;

import android.database.SQLException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class CSProto {
    //CS-PROTO refer https://github.com/nmsoccer/schat/proto/cs/api.go
    public final static int CS_PROTO_PING_REQ   = 1;
    public static final int CS_PROTO_PING_RSP   = 2;
    public static int CS_PROTO_LOGIN_REQ  = 3;
    public static final int CS_PROTO_LOGIN_RSP  = 4;
    public static final int CS_PROTO_LOGOUT_REQ = 5;
    public static int CS_PROTO_LOGOUT_RSP = 6;
    public static final int CS_PROTO_REG_REQ    = 7;
    public static final int CS_PROTO_REG_RSP    = 8;
    public static int CS_PROTO_CREATE_GRP_REQ = 9;
    public static final int CS_PROTO_CREATE_GRP_RSP  = 10;
    public static int CS_PROTO_APPLY_GRP_REQ = 11;
    public static final int CS_PROTO_APPLY_GRP_RSP = 12;
    public static final int CS_PROTO_APPLY_GRP_NOTIFY = 13;
    public static int CS_PROTO_APPLY_GRP_AUDIT  = 14;
    public static int CS_PROTO_SEND_CHAT_REQ = 15;
    public static final int CS_PROTO_SEND_CHAT_RSP = 16;
    public static final int CS_PROTO_SYNC_CHAT_LIST = 17;
    public static int CS_PROTO_EXIT_GROUP_REQ = 18;
    public static final int CS_PROTO_EXIT_GROUP_RSP = 19;
    public static int CS_PROTO_CHAT_HISTORY_REQ = 20;
    public final static int CS_PROTO_COMMON_NOTIFY    = 21;
    public static int CS_PROTO_KICK_GROUP_REQ  = 22;
    public static int CS_PROTO_QUERY_GROUP_REQ = 23;
    public static final int CS_PROTO_SYNC_GROUP_INFO = 24;
    public static int CS_PROTO_FETCH_USER_PROFILE_REQ = 25;
    public static final int CS_PROTO_FETCH_USER_PROFILE_RSP = 26;
    public static final int CS_PROTO_CHG_GROUP_ATTR_REQ = 27;
    public static final int CS_PROTO_CHG_GROUP_ATTR_RSP = 28;
    public final  static int CS_PROTO_GROUP_GROUND_REQ = 29;
    public final  static int CS_PROTO_GROUP_GROUND_RSP = 30;
    public static int CS_PROTO_COMMON_QUERY = 31;
    public static int CS_PROTO_UPDATE_USER_REQ = 32;
    public static final int CS_PROTO_UPDATE_USER_RSP = 33;
    public static int CS_PROTO_UPDATE_CHAT_REQ = 34;
    public static final int CS_PROTO_UPDATE_CHAT_RSP = 35;
    public static int CS_PROTO_END = 36;

    //COMMON NOTIFY
    public static final int COMMON_NOTIFY_T_FILE_ADDR = 1;
    public static final int COMMON_NOTIFY_T_ADD_MEM   = 2;
    public static final int COMMON_NOTIFY_T_DEL_MEM   = 3;
    public static final int COMMON_NOTIFY_T_HEAD_URL  = 4;
    public static final int COMMON_NOTIFY_T_ENTER_GROUP = 5;
    public static final int COMMON_NOTIFY_T_SERVER_SETTING = 6;

    //COMMON QUERY
    public static final int COMMON_QUERY_OWN_GRP_SNAP = 0;
    public static final int COMMON_QUERY_SET_CLI_HEART = 1;

    //COMMON RESULT
    public final static int COMMON_RESULT_SUCCESS = 0;
    public final static int COMMON_RESULT_FAILED = 1;

    //APPLY GROUP
    public static final int APPLY_GRP_CLEAR = 0; //default success
    public static final int APPLY_GRP_DONE  = 0; //apply done
    public static final int APPLY_GRP_ALLOW = 1;
    public static final int APPLY_GRP_DENY  = 2;
    public static final int APPLY_GRP_NONE  = 3; // not exist
    public static final int APPLY_GRP_PASS  = 4;  //pass error
    public static final int APPLY_GRP_EXIST = 5;  //already in
    public static final int APPLY_GRP_ERR   = 6;  //sys err

    //SYNC GROUP FIELD
    public static final int SYNC_GROUP_FIELD_ALL = 1;
    public static final int SYNC_GROUP_FIELD_SNAP = 2;

    //GROUP ATTR
    public static final int GROUP_ATTR_VISIBLE = 0;
    public static final int GROUP_ATTR_INVISIBLE = 1;
    public static final int GROUP_ATTR_DESC = 2;
    public static final int GROUP_ATTR_GRP_NAME = 3;
    public static final int GROUP_ATTR_GRP_HEAD = 4;


    //LOGIN RESULT refer https://github.com/nmsoccer/schat/proto/ss/ss.proto:USER_LOGIN_RET
    public final static int LOGIN_EMPTY = 1;
    public final static int LOGIN_PASS = 2;
    public final static int LOGIN_ERR = 3;
    public final static int LOGIN_MULTI_ON = 4; //multi online

    //REG RESULT
    public static final int REG_SAME_NAME = 1;  //dup name
    public static final int REG_SYS_ERR = 2;    //sys error

    //CREATE GROUP RESULT
    public static final int CREATE_GRP_RESET = -1;
    public static final int CREATE_GRP_DUP_NAME = 1; //dup name
    public static final int CREATE_GRP_DB_ERR = 2;
    public static final int CREATE_GRP_MAX_NUM = 3;
    public static final int CREATE_GRP_RET_FAIL = 4;

    //CHAT MSG TYPE
    public static final int CHAT_MSG_TYPE_TEXT = 0;
    public static final int CHAT_MSG_TYPE_IMG = 1;
    public static final int CHAT_MSG_TYPE_MP4 = 2;
    public static final int CHAT_MSG_TYPE_VOICE = 3;

    //SYNC CHAT TYPE
    public static final int SYNC_CHAT_TYPE_NORMAL = 0;
    public static final int SYNC_CHAT_TYPE_HISTORY = 1;


    private static String log_cs = "cs_proto";
    //API
    public static void CsProto(String resp) {
        try {
            JSONTokener parser = new JSONTokener(resp);
            //1.get root
            JSONObject root = (JSONObject) parser.nextValue();

            //2.proto
            int proto = (int)root.getInt("proto");
            JSONObject sub = (JSONObject)root.get("sub");

            //dispatch
            switch(proto) {
                case CS_PROTO_PING_RSP:
                    CsPingRsp(sub);
                    break;
                case CS_PROTO_LOGIN_RSP:
                    CsLoginRsp(sub);
                    break;
                case CS_PROTO_REG_RSP:
                    CsRegRsp(sub);
                    break;
                case CS_PROTO_COMMON_NOTIFY:
                    CsCommNotifyRsp(sub);
                    break;
                case CS_PROTO_GROUP_GROUND_RSP:
                    CSGroupGroundRsp(sub);
                    break;
                case CS_PROTO_SEND_CHAT_RSP:
                    CSSendChatRsp(sub);
                    break;
                case CS_PROTO_SYNC_CHAT_LIST:
                    CSSyncChatList(sub);
                    break;
                case CS_PROTO_APPLY_GRP_RSP:
                    CsApplyGroupRsp(sub);
                    break;
                case CS_PROTO_SYNC_GROUP_INFO:
                    CSSyncGroupInfo(sub);
                    break;
                case CS_PROTO_FETCH_USER_PROFILE_RSP:
                    CSFetchUserProfileRsp(sub);
                    break;
                case CS_PROTO_APPLY_GRP_NOTIFY:
                    CSApplyGroupNotify(sub);
                    break;
                case CS_PROTO_EXIT_GROUP_RSP:
                    CSExitGroupRsp(sub);
                    break;
                case CS_PROTO_CHG_GROUP_ATTR_RSP:
                    CSChgGroupAttrRsp(sub);
                    break;
                case CS_PROTO_CREATE_GRP_RSP:
                    CSCreateGroupRsp(sub);
                    break;
                case CS_PROTO_UPDATE_USER_RSP:
                    CSUpdateUserRsp(sub);
                    break;
                default:
                    Log.e(log_cs , "unhandle proto: " + proto);
                    break;
            }

        }catch (JSONException e) {
            Log.e(log_cs , "json failed!");
            e.printStackTrace();
        }finally {
            AppConfig.common_op_lock.set(false);//must reset lock;
        }

    }

    //generate login req
    public static String CsLoginReq(String UserName , String Pass , String version) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_LOGIN_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("name" , UserName);
            sub.put("pass" , Pass);
            sub.put("version" , version);

            //all
            obj.put("sub" , sub);
            return obj.toString();
        }catch (JSONException e) {
            Log.e(log_cs , "create login req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate reg req
    public static String CsRegReq(String UserName , String Pass , String NickName , int sex , String addr) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_REG_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("name" , UserName);
            sub.put("pass" , Pass);
            sub.put("role_name" , NickName);
            sub.put("sex" , sex);
            sub.put("addr" , addr);
            sub.put("version" , AppConfig.version);

            //all
            obj.put("sub" , sub);
            return obj.toString();
        }catch (JSONException e) {
            Log.e(log_cs , "create reg req failed!");
            e.printStackTrace();
        }

        return "";
    }


    //generate send chat req
    public static String CSSendChatReq(int chat_type , long grp_id , String content , long tmp_id) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_SEND_CHAT_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("temp_id" , tmp_id);
            sub.put("chat_type" , chat_type);
            sub.put("grp_id" , grp_id);
            sub.put("content" , content);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create group_groud req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate group req
    public static String CSGroupGroundReq(int StartIndex) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_GROUP_GROUND_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("start" , StartIndex);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create group_groud req failed!");
            e.printStackTrace();
        }

        return "";
    }


    //generate apply req
    public static String CSGroupApplyReq(long grp_id , String grp_name , String pass , String msg) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_APPLY_GRP_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("grp_id" , grp_id);
            sub.put("grp_name" , grp_name);
            //sub.put("pass" , pass);
            sub.put("msg" , msg);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create apply_group req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate logout req
    public static String CSLogoutReq() {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_LOGOUT_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("uid" , AppConfig.UserUid);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSLogoutReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate query group req
    public static String CSQueryGroupReq(long grp_id) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_QUERY_GROUP_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("grp_id" , grp_id);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSQueryGroupReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate chat history req
    //[oldest-xx , oldest)
    public static String CSChatHistoryReq(long grp_id , long oldest_msg_id , int count) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_CHAT_HISTORY_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("grp_id" , grp_id);
            sub.put("now_msg_id" , oldest_msg_id);
            sub.put("count" , count);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSChatHistoryReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate fetch user profile req
    public static String CSFetchUserProfileReq(ArrayList<Long> uids) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_FETCH_USER_PROFILE_REQ);

            //sub
            JSONObject sub = new JSONObject();
            JSONArray target_list = new JSONArray();
            for(int i=0; i<uids.size(); i++) {
                target_list.put(uids.get(i));
            }
            sub.put("target_list" , target_list);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSFetchUserProfileReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate group audit req
    public static String CSApplyGroupAudit(int audit , long apply_uid , long grp_id , String grp_name) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_APPLY_GRP_AUDIT);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("audit" , audit);
            sub.put("apply_uid" , apply_uid);
            sub.put("grp_id" , grp_id);
            sub.put("grp_name" , grp_name);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSApplyGroupAudit req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate quit group req
    public static String CSExitGroupReq(long grp_id) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_EXIT_GROUP_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("grp_id" , grp_id);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSExitGroupReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate kick group req
    public static String CSKickGroupReq(long grp_id , long target_uid) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_KICK_GROUP_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("grp_id" , grp_id);
            sub.put("kick_uid" , target_uid);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSKickGroupReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate chg group attr req
    public static String CSChgGroupAttrReq(int attr_id , long grp_id , long int_v , String str_v) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_CHG_GROUP_ATTR_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("attr" , attr_id);
            sub.put("grp_id" , grp_id);
            sub.put("int_v" , int_v);
            sub.put("str_v" , str_v);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSChgGroupAttrReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate create group attr req
    public static String CSCreateGroupReq(String grp_name , String pass , String desc) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_CREATE_GRP_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("name" , grp_name);
            sub.put("pass" , pass);
            sub.put("desc" , desc);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSCreateGroupReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate update user info req
    public static String CSUpdateUserReq(String role_name , String pass , String desc , String addr) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_UPDATE_USER_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("role_name" , role_name);
            sub.put("addr" , addr);
            sub.put("pass" , pass);
            sub.put("desc" , desc);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSUpdateUserReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate update chat req
    public static String CSUpdateChatReq(long grp_id , long msg_id) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_UPDATE_CHAT_REQ);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("upt_type" , ChatInfo.UPT_CHAT_CANCEL);
            sub.put("grp_id" , grp_id);
            sub.put("msg_id" , msg_id);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSUpdateChatReq req failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate invite req
    public static String CSInviteGroupReq(long target_uid , long grp_id , String grp_name) {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_APPLY_GRP_AUDIT);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("audit" , 1);
            sub.put("apply_uid" , target_uid);
            sub.put("grp_id" , grp_id);
            sub.put("grp_name" , grp_name);
            sub.put("flag" , 1);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSInviteGroupReq failed!");
            e.printStackTrace();
        }

        return "";
    }

    //generate heart req
    public static String CSHeartReq() {
        try {
            //root
            JSONObject obj = new JSONObject();
            obj.put("proto", CS_PROTO_COMMON_QUERY);

            //sub
            JSONObject sub = new JSONObject();
            sub.put("type" , COMMON_QUERY_SET_CLI_HEART);

            //all
            obj.put("sub" , sub);
            return obj.toString();

        }catch (JSONException e) {
            Log.e(log_cs , "create CSHeartReq failed!");
            e.printStackTrace();
        }

        return "";
    }


    /*--------------------Sub Proto*-----------------*/
    //cs ping rsp
    private static void CsPingRsp(JSONObject sub) throws JSONException {
        long ts = sub.getLong("ts");
        Log.i(log_cs , "proto:ping rsp ts:" + ts);
    }

    //cs login rsp
    private static void CsLoginRsp(JSONObject sub) throws JSONException {
        String log_label = "CsLoginRsp";
        Log.i(log_label , "proto:login rsp");
        do {
            //result
            int result = sub.getInt("result");
            AppConfig.login_result = result;
            if (result != COMMON_RESULT_SUCCESS) {
                Log.e(log_label, "login failed! ret:" + result);
                break;
            }

            Log.i(log_label , "login result success!");
            //IsLogin
            if(AppConfig.IsLogin()) {
                Log.i(log_label , "already login!");
                return;
            }

            //Level0 UserInfo
            UserInfo user_info = new UserInfo();
            user_info.account_name = sub.getString("name");

            //Level1 Basic
            JSONObject o_basic = sub.getJSONObject("basic");
            UserBasic basic_info = new UserBasic();
            basic_info.uid = o_basic.getLong("uid");
            basic_info.name = o_basic.getString("name");
            basic_info.addr = o_basic.getString("addr");
            basic_info.sex = o_basic.getInt("sex");
            basic_info.level = o_basic.getInt("level");
            basic_info.head_url = o_basic.getString("head_url");

            //Level1 detail
            JSONObject o_detail = sub.getJSONObject("user_detail");
            UserDetail detail_info = new UserDetail();
            detail_info.exp = o_detail.getInt("exp");
            detail_info.desc = o_detail.getString("desc");
            detail_info.des_key = o_detail.getString("c_des_key");
            AppConfig.user_local_des_key = detail_info.des_key;

            //Level2 detail.chat_info
            JSONObject o_chat = o_detail.getJSONObject("chat_info");
            UserChatInfo chat_info = new UserChatInfo();
            if(o_chat != null) {
                //Level3 chat_info.all_groups
                int all_group = o_chat.getInt("all_group");
                if(all_group > 0) {
                    JSONObject o_all_groups = o_chat.getJSONObject("all_groups");
                    ConcurrentHashMap<Long, UserChatGroup> all_groups = new ConcurrentHashMap<>();

                    //iter grp_id
                    Iterator<String> it = o_all_groups.keys();
                    while(it.hasNext()) {
                        String key_grp_id = it.next();
                        //Log.i(log_cs , "login:chat_group:" + key_grp_id);
                        JSONObject o_chat_grp = o_all_groups.getJSONObject(key_grp_id);
                        UserChatGroup chat_grp = new UserChatGroup();
                            //fill group info
                        chat_grp.grp_id = o_chat_grp.getLong("grp_id");
                        chat_grp.grp_name = o_chat_grp.getString("grp_name");
                        chat_grp.serv_last_msg_id = o_chat_grp.getLong("last_msg");
                        chat_grp.enter_ts = o_chat_grp.getLong("enter_ts");
                        chat_grp.local_last_msg_id = 0;
                        chat_grp.oldest_msg_id = chat_grp.local_last_msg_id; //msg id range

                        //fill in map
                        all_groups.put(chat_grp.grp_id , chat_grp);
                    }

                    chat_info.all_groups = all_groups;
                }
                chat_info.all_group = all_group;

                //Level3 chat_info.master_groups
                int master_group = o_chat.getInt("master_group");
                if(master_group > 0) {
                    JSONObject o_master_groups = o_chat.getJSONObject("master_groups");
                    ConcurrentHashMap<Long, Boolean> master_groups = new ConcurrentHashMap<>();
                    //iter grp_id
                    Iterator<String> it = o_master_groups.keys();
                    while(it.hasNext()) {
                        String key_grp_id = it.next();
                        //Log.i(log_cs , "login:chat_group:" + key_grp_id);

                        //fill in map
                        master_groups.put(Long.parseLong(key_grp_id) , true);
                    }
                    chat_info.master_groups = master_groups;
                }
                chat_info.master_group = master_group;
            }

            //Level1 detail end
            if(chat_info != null)
                detail_info.chat_info = chat_info;

            //usere info end
            if(basic_info != null)
                user_info.basic = basic_info;
            if(detail_info != null)
                user_info.detail = detail_info;

            AppConfig.user_info = user_info;

            //open db here
            String db_name = AppConfig.ServerSpace + "_" + user_info.account_name + "_" + DBHelper.DATABASE_NAME;	//与SPACE 和 用户名绑定的库
            DBHelper db_helper = new DBHelper(AppConfig.main_act,db_name,null, DBHelper.DATABASE_VERSION);
            AppConfig.db = db_helper.getWritableDatabase();
            if(AppConfig.db == null)
            {
                AppConfig.PrintInfo(AppConfig.main_act, "系统故障，将无法保存您的聊天记录");
            }
            else
            {
                //AppConfig.PrintInfo(getBaseContext(), "Open DB Success!\n");
                //调取聊天记录中所有姓名
                Log.i(log_label , "open db success! db_name:" + db_name);
            }
            break;
        } while (false);


        AppConfig.common_op_lock.set(false);
    }

    //cs common noitfy rsp
    private static void CsCommNotifyRsp(JSONObject sub) throws JSONException {
        int type = sub.getInt("type");
        long grp_id = sub.getLong("grp_id");
        long int_v = sub.getLong("intv");
        String str_v = sub.getString("strv");
        Log.i(log_cs , "proto:common notify type:" + type + " grp_id:"+grp_id + " int_v:"+int_v +
                "str_v:"+str_v);

        switch(type) {
            case COMMON_NOTIFY_T_FILE_ADDR:
                CommonNotifyFileAddr(sub);
                break;
            case COMMON_NOTIFY_T_ADD_MEM:
                CommonNotifyAddMember(sub);
                break;
            case COMMON_NOTIFY_T_DEL_MEM:
                CommonNotifyDelMember(sub);
                break;
            case COMMON_NOTIFY_T_HEAD_URL:
                CommonNotifyHeadUrl(sub);
                break;
            case COMMON_NOTIFY_T_ENTER_GROUP:
                CommonNotifyEnterGroup(sub);
                break;
            case COMMON_NOTIFY_T_SERVER_SETTING:
                CommonNotifyServerSetting(sub);
                break;
            default:
                Log.e(log_cs , "unkown notify type:" + type);
                break;
        }
    }

    //file server info
    //like:"strs":["1|737698165572|110.29.135.45:22341","2|265724929390|111.29.135.45:22342"]
    private static void CommonNotifyFileAddr(JSONObject sub) throws JSONException {
        JSONArray o_addrs = sub.getJSONArray("strs");
        if(o_addrs==null || o_addrs.length()<=0) {
            Log.i(log_cs , "no file addr!");
            return;
        }
        //file server
        ConcurrentHashMap<Integer , FileServInfo> file_servers = new ConcurrentHashMap<>();
            //parse
        for(int i=0; i<o_addrs.length(); i++) {
            String str = o_addrs.get(i).toString();
            String[] strs = str.split("\\|");
                //check length
            if(strs.length != 3) {
                Log.e(log_cs , "parse file addr failed! length illegal! length:" + strs.length + " raw:" + str);
                continue;
            }

                //fill
            FileServInfo serv_info = new FileServInfo();
            serv_info.Index = Integer.parseInt(strs[0]);
            serv_info.Token = strs[1];
            serv_info.Addr = strs[2];
            Log.i(log_cs , "file_serv index:"+serv_info.Index + " token:"+serv_info.Token + "addr:"+serv_info.Addr);
                //in map
            file_servers.put(serv_info.Index , serv_info);
        }

        AppConfig.miscInfo.set_file_map(file_servers);
        Log.i(log_cs , "CommonNotifyFileAddr finish!");
    }

    //user enter in group
    //like:"sub":{"type":2,"grp_id":5016,"intv":10006,"strv":"av1","strs":null}
    private static void CommonNotifyAddMember(JSONObject sub) throws JSONException {
        long grp_id = sub.getLong("grp_id");
        long uid = sub.getLong("intv");
        String grp_name = sub.getString("strv");

        //add message item
        //set info
        MessageItem msg_item = new MessageItem();
        msg_item.author = Message.MESSAGE_SYS_AUTHOR;
        msg_item.uid = uid;
        msg_item.grp_id = grp_id;
        msg_item.grp_name = grp_name;
        msg_item.snd_ts = AppConfig.CurrentUnixTime();
        String head = "用户:" + uid + " ";
        if(uid == AppConfig.UserUid) {
            head = "您";
        }
        msg_item.content = head + "加入了群组[" + msg_item.grp_name + "]";
        msg_item.msg_type = Message.MESSAGE_TYPE_ENTER_GRPUP;

        //add
        Message.add_msg_item(msg_item);

        //add group member
        ChatInfo.AddGroupMember(msg_item.grp_id , uid);
    }

    //other user enter in group
    //like:"sub":{"type":3,"grp_id":5016,"intv":10006,"strv":"av1","strs":null}
    private static void CommonNotifyDelMember(JSONObject sub) throws JSONException {
        long grp_id = sub.getLong("grp_id");
        long uid = sub.getLong("intv");
        String grp_name = sub.getString("strv");

        //add message item
        //set info
        MessageItem msg_item = new MessageItem();
        msg_item.author = Message.MESSAGE_SYS_AUTHOR;
        msg_item.uid = uid;
        msg_item.grp_id = grp_id;
        msg_item.grp_name = grp_name;
        msg_item.snd_ts = AppConfig.CurrentUnixTime();
        String head = "用户:" + uid + " ";
        if(uid == AppConfig.UserUid) {
            head = "您";
        }
        msg_item.content = head + "退出了群组[" + msg_item.grp_name + "]";
        msg_item.msg_type = Message.MESSAGE_TYPE_LEAVE_GROUP;

        //add msg
        Message.add_msg_item(msg_item);

        //del group member
        ChatInfo.DelGroupMember(msg_item.grp_id , uid);
    }

    //modify head url
    //like:"sub":{"type":4,"grp_id":0,"intv":0,"strv":"2:2:20:10004_24f772b8e24d830bb2ece21baa05dd61_.jpg","strs":null}
    private static void CommonNotifyHeadUrl(JSONObject sub) throws JSONException {
        String new_head_url = sub.getString("strv");
        if(AppConfig.user_info == null) {
            return;
        }
        AppConfig.user_info.basic.head_url = new_head_url;
        AppConfig.user_info.basic.head_file_name = AppConfig.Url2RealFileName(new_head_url);
    }

    //user enter in group
    //like:"sub":{"type":5,"grp_id":5016,"intv":0,"strv":"xxoo","strs":null}
    private static void CommonNotifyEnterGroup(JSONObject sub) throws JSONException {
        long grp_id = sub.getLong("grp_id");
        String grp_name = sub.getString("strv");
        UserInfo.EnterGroup(grp_id , grp_name , false);
    }

    //server setting
    //like:"sub":{"type":6,"grp_id":0,"intv":0,"strv":"{\"chat_config_table\":{\"count\":2,\"res\":[{\"name\":\"max_create_group\",\"value\":\"10\"},{\"name\":\"max_reconnect_times\",\"value\":\"5\"}]}}","strs":null}
    private static void CommonNotifyServerSetting(JSONObject sub) throws  JSONException {
        String log_label = "NotifyServerSetting";
        if(AppConfig.ServerSettingRecved) {
            Log.d(log_label , "already recved!");
            return;
        }
        String setting_str = sub.getString("strv");
        if(TextUtils.isEmpty(setting_str)) {
            Log.e(log_label , "setting info null");
            return;
        }

        //parse
        JSONTokener parser = new JSONTokener(setting_str);
        //1.get root
        JSONObject root = (JSONObject) parser.nextValue();


        //table
        JSONObject o_config_table = root.getJSONObject("chat_config_table");
        if(o_config_table == null) {
            Log.e(log_label , "o_config_table null");
            return;
        }

        //count
        int count = o_config_table.getInt("count");
        Log.d(log_label , "res count:" + count);

        //res
        JSONArray o_res = o_config_table.getJSONArray("res");
        if(o_res == null) {
            Log.e(log_label , "o_res null");
            return;
        }

        JSONObject o_cfg_item;
        String name;
        String value;
        int v;
        for(int i=0; i<o_res.length(); i++) {
            o_cfg_item = o_res.getJSONObject(i);
            if(o_cfg_item == null) {
                Log.e(log_label , "item nil at index:" + i);
                continue;
            }

            name = o_cfg_item.getString("name");
            value = o_cfg_item.getString("value");
            if(TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) {
                Log.e(log_label , "item info illegal! content:" + o_cfg_item.toString());
                continue;
            }
            //Log.d(log_label , "name:" + name + " v:" + value);

            //handle
            switch (name) {
                case "max_create_group":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "max_create_group " + UserInfo.MAX_CREATE_GROUP_COUNT + " --> " + v);
                    if(v > 0)
                        UserInfo.MAX_CREATE_GROUP_COUNT = v;
                    break;
                case "max_reconnect_times":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "max_reconnect_times " + AppConfig.MAX_DIS_RECONN_TIMES + " --> " + v);
                    if(v > 0)
                        AppConfig.MAX_DIS_RECONN_TIMES = v;
                    break;
                case "heart_frequent":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "heart_frequent " + AppConfig.HEART_BEAT_FREQUENT + " --> " + v);
                    if(v > 0)
                        AppConfig.HEART_BEAT_FREQUENT = v;
                    break;
                case "req_timeout":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "req_timeout " + AppConfig.REQ_TIMEOUT + " --> " + v);
                    if(v > 0) {
                        AppConfig.REQ_TIMEOUT = v;
                    }
                    break;
                case "upload_file_size":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "upload_file_size " + AppConfig.UPLOAD_NORMAL_FILE_SIZE + " --> " + v);
                    if(v > 0)
                        AppConfig.UPLOAD_NORMAL_FILE_SIZE = v;
                    break;
                case "upload_img_size":
                    v = Integer.parseInt(value);
                    Log.d(log_label , "upload_img_size " + AppConfig.MAX_IMG_SIZE + " --> " + v);
                    if(v > 0)
                        AppConfig.MAX_IMG_SIZE = v;
                    break;
                default:
                    Log.e(log_label , "unkown option:" + name);
                    break;
            }


        }

    }


    //cs ground group rsp
    private static void CSGroupGroundRsp(JSONObject sub) throws JSONException {
        int count = sub.getInt("count");
        Log.i(log_cs , "proto:CSGroupGroundRsp count:" + count);

        ArrayList<GroupGroundItem> item_list = new ArrayList<>();
        long grp_id = 0;
        String grp_name = "";
        do {
            //empty
            if(count<=0) {
                break;
            }
            //list
            JSONArray o_item_list = sub.getJSONArray("item_list");
            if(o_item_list == null) {
                break;
            }

            //parse
            for (int i = 0; i < o_item_list.length(); i++) {
                JSONObject o_item = o_item_list.getJSONObject(i);
                GroupGroundItem item = new GroupGroundItem();
                item.grp_id = o_item.getLong("grp_id");
                item.grp_name = o_item.getString("grp_name");
                item.mem_count = o_item.getInt("mem_count");
                item.desc = o_item.getString("desc");
                item.head_url = o_item.getString("head_url");

                item_list.add(item);
            }

        }while (false);

        //finish
        if(count>0 || item_list.size()>0) {
            ChatInfo.AddGroupSnap(item_list);
        }
    }

    //chat rsp
    private static void CSSendChatRsp(JSONObject sub) throws JSONException {
        String log_label = "CsSendChatRsp";
        long temp_id = sub.getLong("temp_id");
        int result = sub.getInt("result");
        if(result != CSProto.COMMON_RESULT_SUCCESS) {
            Log.e(log_label , "send failed! temp_id:" + temp_id);
            return;
        }

        //get group
        JSONObject o_msg = sub.getJSONObject("chat_msg");
        if(o_msg == null) {
            Log.e(log_label , "chat msg nil! temp_id:" + temp_id);
            return;
        }

        long grp_id = o_msg.getLong("grp_id");
        UserChatGroup u_grp = UserInfo.getChatGrp(grp_id);
        if(u_grp == null) {
            Log.e(log_label , "group info null! grp_id:" + grp_id + " temp_id:" + temp_id);
            return;
        }

        u_grp.recved_tmp_list.add(temp_id);
        Log.d(log_label , "finish! grp_id:" + grp_id + " temp_id:" + temp_id);
    }

    //cs sync chat list
    private static void CSSyncChatList(JSONObject sub) throws JSONException {
        int sync_type = sub.getInt("sync_type");
        switch (sync_type) {
            case SYNC_CHAT_TYPE_NORMAL:
                SyncChatListNoraml(sub);
                break;
            case SYNC_CHAT_TYPE_HISTORY:
                SyncChatListHistory(sub);
                break;
            default:
                Log.e(log_cs , "illegal sync_type:" + sync_type);
                break;
        }
    }

    private static void SyncChatListNoraml(JSONObject sub) throws JSONException {
        String log_label = "SyncChatListNoraml";
        long grp_id = sub.getLong("grp_id");
        long curr_max_msg_id = 0;

        int count = sub.getInt("count");
        Log.d(log_label , "grp_id:" + grp_id + " count:" + count);
        if(count <= 0) {
            return;
        }

        String tb_name = DBHelper.grp_id_2_tab_name(grp_id);
        //parse list
        JSONArray o_list = sub.getJSONArray("chat_list");
        ChatMsg msg = new ChatMsg();
        for(int i=0; i<count; i++) {
            JSONObject o_msg = o_list.getJSONObject(i);
            //parse chat
            msg.chat_type = o_msg.getInt("chat_type");
            msg.msg_id = o_msg.getLong("msg_id");
            msg.grp_id = o_msg.getLong("grp_id");
            msg.snd_uid = o_msg.getLong("sender_uid");
            msg.snd_name = o_msg.getString("sender");
            msg.snd_ts = o_msg.getLong("send_ts");
            msg.sn_content = o_msg.getString("content");
            msg.chat_flag = o_msg.getLong("flag");
            if(msg.msg_id > curr_max_msg_id) {
                curr_max_msg_id = msg.msg_id;
            }
            Log.d(log_label , "<" + i + "> msg_id:" + msg.msg_id + " snd_uid:" +
                    msg.snd_uid + " content:" + msg.sn_content);

            //insert db
            if(AppConfig.db == null) {
                Log.i(log_label , "sql not ready! msg_id:" + msg.msg_id);
                continue;
            }
            int is_from = 1; //from chat
            if(msg.snd_uid == AppConfig.UserUid) {
                is_from = 0; //to chat
            }
            //String enc_content = DBHelper.sqliteEscape(msg.sn_content);
            String enc_content = DBHelper.EncryptChatContent(msg.sn_content , AppConfig.user_local_des_key);
            if(TextUtils.isEmpty(enc_content)) {
                Log.e(log_label , "enc content failed!");
                return;
            }

            String sql = "insert into " + tb_name + "(msg_id, is_from, chat_type, grp_id, snd_uid, snd_name, snd_time, flag , chat_content) " +
                    "values(" + msg.msg_id + "," + is_from + "," + msg.chat_type + "," + msg.grp_id + "," + msg.snd_uid + ",'" + msg.snd_name + "'," +
                    msg.snd_ts + "," + msg.chat_flag + ",'" + enc_content + "')";
            Log.d(log_label , "sql:" + sql);
            try {
                AppConfig.db.execSQL(sql);
            }catch (SQLException e) {
                Log.e(log_label , "error! sql:" + sql);
                e.printStackTrace();
            }

            //check special
            //canceller msg
            if(msg.chat_flag == ChatInfo.CHAT_FLAG_CANCELLER && msg.sn_content.length()>0) {
                Log.i(log_label , "canceller msg! cancel msg_id:" + msg.sn_content);
                long canceled_msg_id = Long.parseLong(msg.sn_content);
                DBHelper.CancelChat(grp_id , canceled_msg_id);
            }

        }
        UserChatGroup u_grp = UserInfo.getChatGrp(grp_id);
        if(u_grp == null) {
            Log.e(log_label , "grp info nil!");
            return;
        }
        u_grp.new_msg.set(true);
        if(curr_max_msg_id > u_grp.serv_last_msg_id) { //update server max
            u_grp.serv_last_msg_id = curr_max_msg_id;
        }
        //UserInfo.setGrpNewMsgStat(grp_id , true);
        //
        /*
        if(curr_max_msg_id > 0) {
            UserInfo.SetGrpServerLatestId(grp_id , curr_max_msg_id);
        }*/
    }

    private static void SyncChatListHistory(JSONObject sub) throws JSONException {
        String log_label = "SyncChatListHistory";
        long grp_id = sub.getLong("grp_id");
        int count = sub.getInt("count");
        Log.d(log_label , "grp_id:" + grp_id + " count:" + count);
        if(count <= 0) {
            AppConfig.chat_history_lock.set(false);
            return;
        }
        //check db
        if(AppConfig.db == null) {
            Log.e(log_label , "db nil! grp_id:" + grp_id);
            AppConfig.chat_history_lock.set(false);
            return;
        }

        String tb_name = DBHelper.grp_id_2_tab_name(grp_id);
        //parse list
        JSONArray o_list = sub.getJSONArray("chat_list");
        ChatMsg msg = new ChatMsg();
        long max_msg_id = 0;

        for (int i = 0; i < count; i++) {
            JSONObject o_msg = o_list.getJSONObject(i);
            //parse chat
            msg.chat_type = o_msg.getInt("chat_type");
            msg.msg_id = o_msg.getLong("msg_id");
            msg.grp_id = o_msg.getLong("grp_id");
            msg.snd_uid = o_msg.getLong("sender_uid");
            msg.snd_name = o_msg.getString("sender");
            msg.snd_ts = o_msg.getLong("send_ts");
            msg.sn_content = o_msg.getString("content");
            msg.chat_flag = o_msg.getLong("flag");
            Log.d(log_label, "<" + i + "> msg_id:" + msg.msg_id + " snd_uid:" +
                    msg.snd_uid + " content:" + msg.sn_content);
            int is_from = 1; //from chat
            if (msg.snd_uid == AppConfig.UserUid) {
                is_from = 0; //to chat
            }
            if(msg.msg_id > max_msg_id) {
                max_msg_id = msg.msg_id;
            }
            //update
            if(msg.chat_flag == ChatInfo.CHAT_FLAG_CANCELED)
                    msg.sn_content = DBHelper.canceled_content;

            //String enc_content = DBHelper.sqliteEscape(msg.sn_content);
            String enc_content = DBHelper.EncryptChatContent(msg.sn_content , AppConfig.user_local_des_key);
            if(TextUtils.isEmpty(enc_content)) {
                Log.e(log_label , "enc content failed!");
                return;
            }

            String sql = "insert into " + tb_name + "(msg_id, is_from, chat_type, grp_id, snd_uid, snd_name, snd_time, flag , chat_content) " +
                    "values(" + msg.msg_id + "," + is_from + "," + msg.chat_type + "," + msg.grp_id + "," + msg.snd_uid + ",'" + msg.snd_name + "'," +
                    msg.snd_ts + "," + msg.chat_flag + ",'" + enc_content + "')";
            Log.d(log_label, "sql:" + sql);
            try {
                AppConfig.db.execSQL(sql);
            }catch (SQLException e) {
                Log.e(log_cs , "SyncChatListHistory error! sql:" + sql);
                e.printStackTrace();
            }
        }
        UserChatGroup u_grp = UserInfo.getChatGrp(grp_id);
        //may be init local lastest_msg_id
        if(u_grp != null && u_grp.local_last_msg_id == 0) {
            u_grp.local_last_msg_id = max_msg_id;
            u_grp.oldest_msg_id = max_msg_id; //reset
            Log.d(log_label , "set local last_msg_id:" + max_msg_id);
        }
        if(u_grp != null && u_grp.serv_last_msg_id < max_msg_id) {
            u_grp.serv_last_msg_id = max_msg_id;
            Log.d(log_label , "set server last_msg_id:" + max_msg_id);
        }


        AppConfig.chat_history_lock.set(false);
    }

    //cs reg rsp
    private static void CsRegRsp(JSONObject sub) throws JSONException {
        //result
        int result = sub.getInt("result");
        AppConfig.reg_result = result;
        AppConfig.common_op_lock.set(false);

        Log.i(log_cs, "proto:reg rsp result:" + result);
        if (result != COMMON_RESULT_SUCCESS) {
            Log.e(log_cs, "reg failed! ret:" + result);
        }
    }

    //cs apply group rsp
    private static void CsApplyGroupRsp(JSONObject sub) throws JSONException {
        //result
        int result = sub.getInt("result");
        AppConfig.apply_result = result;

        long grp_id = sub.getLong("grp_id");
        String grp_name = sub.getString("grp_name");
        int flag = sub.getInt("flag");
        Log.i(log_cs, "proto:apply group rsp result:" + result + " grp_id:" + grp_id + " grp_name:" + grp_name + " flag:"+flag);

        //some result handle
        switch (result) {
            case APPLY_GRP_ALLOW:
                //something
                //UserInfo.EnterGroup(grp_id , grp_name , false);
                if(result == 1) { //be invited into group
                    //add message item
                    MessageItem msg_item = new MessageItem();
                    msg_item.author = Message.MESSAGE_SYS_AUTHOR;
                    msg_item.uid = AppConfig.UserUid;
                    msg_item.grp_id = grp_id;
                    msg_item.grp_name = grp_name;
                    msg_item.snd_ts = AppConfig.CurrentUnixTime();
                    msg_item.content = "您被邀请加入了群组[" + msg_item.grp_name + "]";
                    msg_item.msg_type = Message.MESSAGE_TYPE_ENTER_GRPUP;

                    //add msg
                    Message.add_msg_item(msg_item);
                }
                break;
            case APPLY_GRP_DENY:
                //something
                break;
            default:    //will sync
                AppConfig.common_op_lock.set(false);
                break;
        }

    }



    //cs sync group rsp
    private static void CSSyncGroupInfo(JSONObject sub) throws JSONException {
        //field
        int field = sub.getInt("field");
        long grp_id = sub.getLong("grp_id");

        //switch
        switch (field) {
            case SYNC_GROUP_FIELD_ALL:
                SyncGroupInfoAll(grp_id , sub);
                break;
            case SYNC_GROUP_FIELD_SNAP:
                SyncGroupInfoSnap(grp_id , sub);
                break;
            default:
                Log.e(log_cs , "CSSyncGroupInfo illegal field:" + field);
                break;
        }

    }


    private static void SyncGroupInfoAll(long grp_id , JSONObject sub) throws JSONException {
        JSONObject o_grp_info = sub.getJSONObject("grp_info");
        if(o_grp_info == null) {
            Log.e(log_cs , "SyncGroupInfoAll grp_info null! grp_id:" + grp_id);
            return;
        }

        //grp_info
        ChatGroup grp_info = new ChatGroup();
        grp_info.grp_id = grp_id;
        grp_info.grp_name = o_grp_info.getString("grp_name");
        grp_info.master = o_grp_info.getLong("master");
        grp_info.create_ts = o_grp_info.getLong("create");
        grp_info.mem_count = o_grp_info.getInt("mem_count");
        grp_info.desc = o_grp_info.getString("desc");
        grp_info.head_url = o_grp_info.getString("head_url");
        if(o_grp_info.getInt("visible") > 0) {
            grp_info.visible = true;
        }
        if(grp_info.mem_count>0 && o_grp_info.has("members")) {
            JSONObject o_members = o_grp_info.getJSONObject("members");
            ArrayList<Long> members = grp_info.members;


            //iter grp_id
            Iterator<String> it = o_members.keys();
            long mem_uid = 0;
            String key_uid = "";
            while(it.hasNext()) {
                key_uid = it.next();
                mem_uid = Long.parseLong(key_uid);
                Log.d(log_cs , "SyncGroupInfoAll:mem_uid:" + mem_uid);
                //fill
                members.add(mem_uid);
            }
        }

        //insert
        ChatInfo.AddChatGroup(grp_info);

        //try fetch uids
        ArrayList<Long> uids = new ArrayList<>();
        uids.add(grp_info.master);
        for(int i=0; i<grp_info.members.size(); i++) {
            uids.add(grp_info.members.get(i));
        }
        TryFetchUserProfiles(uids);
    }

    private static void SyncGroupInfoSnap(long grp_id , JSONObject sub) throws JSONException {
        JSONObject o_grp_snap = sub.getJSONObject("grp_snap");
        if(o_grp_snap == null) {
            Log.e(log_cs , "SyncGroupInfoSnap grp_snap null! grp_id:" + grp_id);
            return;
        }

        //grp_info
        GroupGroundItem grp_snap = new GroupGroundItem();
        grp_snap.grp_id = grp_id;
        grp_snap.grp_name = o_grp_snap.getString("grp_name");
        grp_snap.mem_count = o_grp_snap.getInt("mem_count");
        grp_snap.desc = o_grp_snap.getString("desc");
        grp_snap.head_url = o_grp_snap.getString("head_url");


        //try fetch uids
        ArrayList<GroupGroundItem> items = new ArrayList<>();
        items.add(grp_snap);
        ChatInfo.AddGroupSnap(items);
    }

    private static void TryFetchUserProfiles(ArrayList<Long> uids) {
        ArrayList<Long> net_uids;
        do {
            //from local cache
            HashMap<Long, UserProfile> map = UserInfo.GetUserProfiles(uids);
            if (map == null) {
                Log.e(log_cs , "TryFetchUserProfiles map nil!");
                net_uids = uids;
                break;
            }

            net_uids = new ArrayList<>();
            long uid;
            //not exist in local
            for(int i=0; i<uids.size(); i++) {
                uid = uids.get(i);
                if(!map.containsKey(uid)) {
                    Log.d(log_cs , "TryFetchUserProfiles net uid:" + uid);
                    net_uids.add(uid);
                }
            }
            break;
        }while (false);

        //query server
        if(net_uids.size() <= 0) {
            return;
        }

        Log.d(log_cs , "TryFetchUserProfiles query from server!");
        String req = CSFetchUserProfileReq(net_uids);
        AppConfig.SendMsg(req);
    }

    //cs fetch profiles rsp
    private static void CSFetchUserProfileRsp(JSONObject sub) throws JSONException {
        String log_func = "CSFetchUserProfileRsp";
        JSONObject o_profiles = sub.getJSONObject("profiles");
        if(o_profiles == null) {
            Log.e(log_cs , log_func + " empty profiles");
            return;
        }

        //iter
        //iter grp_id
        ArrayList<UserProfile> profiles = new ArrayList<>();
        Iterator<String> it = o_profiles.keys();
        while(it.hasNext()) {
            String key_uid = it.next();
            Log.d(log_cs , log_func + " uid:" + key_uid);
            JSONObject o_user_profile = o_profiles.getJSONObject(key_uid);
            UserProfile user_profile = new UserProfile();

            //fill info
            user_profile.uid = o_user_profile.getLong("uid");
            user_profile.name = o_user_profile.getString("name");
            user_profile.addr = o_user_profile.getString("addr");
            user_profile.level = o_user_profile.getInt("level");
            user_profile.sex = (short)o_user_profile.getInt("sex");
            user_profile.head_url = o_user_profile.getString("head_url");
            user_profile.user_desc = o_user_profile.getString("desc");

            //fill in map
            profiles.add(user_profile);
            //save to db

        }

        //add to cache
        if(profiles.size() <= 0) {
            return;
        }
        UserInfo.SetUserProfiles(profiles);
    }

    //cs fetch profiles rsp
    private static void CSApplyGroupNotify(JSONObject sub) throws JSONException {
        String log_func = "CSApplyGroupNotify";

        //set info
        MessageItem msg_item = new MessageItem();
        msg_item.uid = sub.getLong("apply_uid");
        msg_item.author = sub.getString("apply_name");
        msg_item.snd_ts = AppConfig.CurrentUnixTime();
        String apply_info = sub.getString("apply_msg");
        msg_item.grp_id = sub.getLong("grp_id");
        msg_item.grp_name = sub.getString("grp_name");
        msg_item.content = "申请加入群组[" + msg_item.grp_name + "]" + " 附加信息:" + apply_info;
        msg_item.msg_type = Message.MESSAGE_TYPE_APPLY_GROUP;

        //add
        Message.add_msg_item(msg_item);
    }

    //cs exit group rsp
    private static void CSExitGroupRsp(JSONObject sub) throws JSONException {
        String log_func = "CSExitGroupRsp";

        int result = sub.getInt("result");

        //set info
        MessageItem msg_item = new MessageItem();
        msg_item.author = Message.MESSAGE_SYS_AUTHOR;
        msg_item.snd_ts = AppConfig.CurrentUnixTime();
        msg_item.grp_id = sub.getLong("grp_id");
        msg_item.grp_name = sub.getString("grp_name");
        msg_item.msg_type = Message.MESSAGE_TYPE_LEAVE_GROUP;

        int del_group = sub.getInt("del_group");
        int by_kick = sub.getInt("by_kick");
        //construct content
        String content = "您";
        if(del_group == 0) {
            content += "退出群组";
        } else {
            content += "解散群组";
        }

        content += "[" + msg_item.grp_name + "]";

        //result
        if(result == CSProto.COMMON_RESULT_SUCCESS) {
            //nothing
            if(msg_item.grp_id > 0) {
                UserInfo.LeaveGroup(msg_item.grp_id);
                if(del_group == 1) {
                    ChatInfo.DelGroupSnap(msg_item.grp_id);
                }
            }
        } else {
            content += "失败";
        }

        //kick?
        if(by_kick == 1) {
            content += " （被踢出）";
            msg_item.msg_type = Message.MESSAGE_TYPE_KICK_GROUP;
        }
        msg_item.content = content;

        //add
        Message.add_msg_item(msg_item);
    }

    //cs chg group attr rsp
    private static void CSChgGroupAttrRsp(JSONObject sub) throws JSONException {
        String log_label = "CSChgGroupAttrRsp";

        //set info
        int result = sub.getInt("result");
        int attr_id = sub.getInt("attr");
        long grp_id = sub.getLong("grp_id");
        long int_v = sub.getLong("int_v");
        String str_v = sub.getString("str_v");

        if(result != CSProto.COMMON_RESULT_SUCCESS) {
            Log.e(log_label , "chg attr:" + attr_id + " failed!");
            return;
        }
        //get grp_info
        ChatGroup grp_info = ChatInfo.GetChatGroup(grp_id);
        if (grp_info == null) {
            Log.e(log_label , "get group info failed! grp_id:" + grp_id);
            return;
        }

        //switch
        switch (attr_id) {
            case CSProto.GROUP_ATTR_VISIBLE:
                grp_info.visible = true;
                Log.d(log_label , "set visible! grp_id:" + grp_id);
                break;
            case CSProto.GROUP_ATTR_INVISIBLE:
                grp_info.visible = false;
                Log.d(log_label , "set invisible! grp_id:" + grp_id);
                break;
            case CSProto.GROUP_ATTR_DESC:
                grp_info.desc = str_v;
                Log.d(log_label , "set desc! grp_id:" + grp_id + " desc:" + str_v);
                break;
            case CSProto.GROUP_ATTR_GRP_NAME:
                Log.d(log_label , "set grp_name! grp_id:" + grp_id + " old:" + grp_info.grp_name + " new:" + str_v);
                grp_info.grp_name = str_v;

                //grp_tiem
                GroupGroundItem item = ChatInfo.GetGroupSnap(grp_id);
                if(item != null)
                    item.grp_name = str_v;
                //user group
                UserChatGroup u_grp = UserInfo.getChatGrp(grp_id);
                if(u_grp != null)
                    u_grp.grp_name = str_v;
                //AppConfig.change_grp_name = true;
                break;
            case CSProto.GROUP_ATTR_GRP_HEAD:
                grp_info.head_url = str_v;
                Log.d(log_label , "set head_url! grp_id:" + grp_id + " head_url:" + str_v);
                //also set profile if used
                GroupGroundItem profile = ChatInfo.GetGroupSnap(grp_id);
                if(profile != null)
                    profile.head_url = grp_info.head_url;
                    ChatInfo.AddGroupSnap(profile);
                break;
            default:
                Log.e(log_label , "unkown attr_id:" + attr_id);
                break;
        }


    }

    //cs create group rsp
    private static void CSCreateGroupRsp(JSONObject sub) throws JSONException {
        String log_label = "CSCreateGroupRsp";
        //result
        int result = sub.getInt("result");

        //if fail
        Log.i(log_label, "rsp result:" + result);
        if (result != COMMON_RESULT_SUCCESS) {
            Log.e(log_cs, "failed! ret:" + result);
            AppConfig.create_group_result = result;
            return;
        }

        //fill info
        GroupGroundItem g_item = new GroupGroundItem();
        g_item.grp_name = sub.getString("name");
        g_item.grp_id = sub.getLong("grp_id");
        g_item.mem_count = sub.getInt("member_count");
        g_item.desc = sub.getString("desc");
        Log.i(log_label , "grp_id:" + g_item.grp_id + " name:" + g_item.grp_name + " desc:" + g_item.desc);

        //add
        ChatInfo.AddGroupSnap(g_item);
        UserInfo.EnterGroup(g_item.grp_id , g_item.grp_name , true);
        AppConfig.CreateGroupFileDir(g_item.grp_id);
        //set result
        AppConfig.create_group_result = result;
    }


    //cs update user rsp
    private static void CSUpdateUserRsp(JSONObject sub) throws JSONException {
        String log_label = "CSUpdateUserRsp";
        //result
        int result = sub.getInt("result");

        //if fail
        Log.i(log_label, "rsp result:" + result);
        if (result != COMMON_RESULT_SUCCESS) {
            Log.e(log_label, "failed! ret:" + result);
            AppConfig.update_user_self = result;
            return;
        }

        //fill info
        String role_name = sub.getString("role_name");
        String addr = sub.getString("addr");
        String desc = sub.getString("desc");
        String pass = sub.getString("pass");
        Log.i(log_label , "role_name:" + role_name + " addr:" + addr + " desc:" + desc + " pass" + pass);

        //change
        UserInfo user_info = AppConfig.user_info;
        if(role_name.length() > 0)
            user_info.basic.name = role_name;
        if(addr.length() > 0)
            user_info.basic.addr = addr;
        if(desc.length() > 0)
            user_info.detail.desc = desc;

        //set result
        AppConfig.update_user_self = result;
    }


}

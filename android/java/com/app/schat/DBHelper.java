package com.app.schat;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper
{
	public static final String DATABASE_NAME = "schat.db";
	public static final int DATABASE_VERSION = 1;
	public static final String CHAT_TB_NAME = "tbchat";
	public static final String USER_TB_NAME = "tbuser";
	public static final String MESSAGE_TB_NAME = "tbmessage";


	public static int MAX_CHAT_GROUP_TAB = 19; //hash group to diff tab
	public static int MAX_USER_INFO_TAB = 27; //hash uid to diff tab

	public static String grp_id_2_tab_name(long grp_id) {
		long v = grp_id % MAX_CHAT_GROUP_TAB;
		return CHAT_TB_NAME + "_" + v;
	}
	public static String uid_2_tab_name(long uid) {
		long v = uid % MAX_USER_INFO_TAB;
		return USER_TB_NAME + "_" + v;
	}


	public DBHelper(Context context, String name, CursorFactory factory, int version)
	{
		super(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		//在数据库被创建时执行
		//chat
		for(int i=0; i<MAX_CHAT_GROUP_TAB; i++) {
			String tab_name = CHAT_TB_NAME + "_" + i;
			db.execSQL("CREATE TABLE IF NOT EXISTS " + tab_name +
					"(msg_id INTEGER , is_from INTEGER, chat_type INTEGER , grp_id INTEGER , snd_uid INTEGER , " +
					"snd_name VARCHAR, snd_time INTEGER, chat_content TEXT , flag INTEGER , primary key (msg_id,grp_id))");
		}
		//user profile
		/*
		for(int i=0; i<MAX_USER_INFO_TAB; i++) {
			String tab_name = AppConfig.USER_TB_NAME + "_" + i;
			db.execSQL("CREATE TABLE IF NOT EXISTS " + tab_name +
					"(uid INTEGER PRIMARY KEY , name VARCHAR, addr VARCHAR , sex INTEGER , level INTEGER , " +
					"head_url VARCHAR)");
		}
		 */
		//message
		String tab_name = MESSAGE_TB_NAME;
		db.execSQL("CREATE TABLE IF NOT EXISTS " + tab_name +
				"(msg_id INTEGER PRIMARY KEY AUTOINCREMENT , msg_type INTEGER, author VARCHAR , content VARCHAR , snd_ts INTEGER , " +
				"uid INTEGER, grp_id INTEGER, grp_name VARCHAR , read INTEGER , extra_str VARCHAR)");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int old_version, int new_version)
	{
		// TODO Auto-generated method stub

	}

	public static void del_all_tables() {
		if(AppConfig.db == null) {
			return;
		}
		for(int i=0; i<MAX_CHAT_GROUP_TAB; i++) {
			String tab_name = CHAT_TB_NAME + "_" + i;
			AppConfig.db.execSQL("drop table " + tab_name);
		}
	}

	public static String EncryptChatContent(String src_content , String des_key) {
		String log_label = "EncryptChatContent";
		if(TextUtils.isEmpty(des_key)) {
			Log.e(log_label , "des key empty!");
			return "";
		}

		String base64_content = null;
		//encrypt
		try {
			byte[] enc_data;
			enc_data = MyEncrypt.EncryptDesECB(src_content.getBytes(), des_key.getBytes());
			//base64
			base64_content = Base64.encodeToString(enc_data, Base64.DEFAULT);
		}catch (Exception e) {
			Log.e(log_label , "encrypt data" + src_content + "failed!");
			e.printStackTrace();
			return "";
		}

		//Log.d(log_label , "success! src:" + src_content + " dst:" + base64_content + " des_key:" + des_key);
		return base64_content;
	}

	public static String DecryptChatContent(String src_content , String des_key) {
		String log_label = "DecryptChatContent";
		if(TextUtils.isEmpty(des_key)) {
			Log.e(log_label , "des key empty!");
			return "";
		}

		byte[] dec_data = null;
		//decode
		try{
			byte[] decode_data;
			decode_data = Base64.decode(src_content , Base64.DEFAULT);

			//decrypt
			dec_data = MyEncrypt.DecryptDesECB(decode_data , des_key.getBytes());
		}catch (IllegalArgumentException e) {
			e.printStackTrace();
			return "";
		}catch (Exception e) {
			Log.e(log_label , "decrypt data" + src_content + "failed!");
			e.printStackTrace();
			return "";
		}

		if(dec_data == null)
			return "";

		//result
		String result = new String(dec_data);
		//Log.d(log_label , "success! src:" + src_content + " dst:" + result + " des_key:" + des_key);
		return result;
	}


	public static String sqliteEscape(String keyWord){
		//keyWord = keyWord.replace("/", "//");
		keyWord = keyWord.replace("'", "''");
		//keyWord = keyWord.replace("[", "/[");
		//keyWord = keyWord.replace("]", "/]");
		//keyWord = keyWord.replace("%", "/%");
		//keyWord = keyWord.replace("&","/&");
		//keyWord = keyWord.replace("_", "/_");
		//keyWord = keyWord.replace("(", "/(");
		//keyWord = keyWord.replace(")", "/)");
		return keyWord;
	}

	public static String canceled_content = "已撤回消息";
	public static void CancelChat(long grp_id , long msg_id) {
		String log_label = "CancelChat";
		if(AppConfig.db == null){
			Log.e(log_label , "db not nil");
			return;
		}

		//sql
		String tb_name = grp_id_2_tab_name(grp_id);
		String sql = "update " + tb_name + " set chat_content='" + canceled_content + "', flag=" + ChatInfo.CHAT_FLAG_CANCELED + ", chat_type=" +
				CSProto.CHAT_MSG_TYPE_TEXT + " WHERE grp_id=" +	grp_id + " AND msg_id=" + msg_id + " ";
		try {
			AppConfig.db.execSQL(sql);
			Log.i(log_label , "sql:" + sql);
		}catch (SQLException e) {
			Log.e(log_label , "exe failed! sql:" + sql);
			e.printStackTrace();
		}

	}

	public static void DelChat(long grp_id , long msg_id) {
		String log_label = "DelChat";
		if(AppConfig.db == null){
			Log.e(log_label , "db not nil");
			return;
		}

		//sql
		String tb_name = grp_id_2_tab_name(grp_id);
		String sql = "update " + tb_name + " set flag=" + ChatInfo.CHAT_FLAG_DEL + " WHERE grp_id=" +
				grp_id + " AND msg_id=" + msg_id + " ";
		try {
			AppConfig.db.execSQL(sql);
			Log.i(log_label , "sql:" + sql);
		}catch (SQLException e) {
			Log.e(log_label , "exe failed! sql:" + sql);
			e.printStackTrace();
		}

	}


}

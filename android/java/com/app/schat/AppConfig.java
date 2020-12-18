package com.app.schat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class AppConfig {
    public static String ServerSpace = "schat";
    public static String version = "";
    public static int CURRENT_SUPPORT_CHAT_TYPE = CSProto.CHAT_MSG_TYPE_VOICE;
    public static int REQ_TIMEOUT = 7;
    
    public static final String PGPREFS = "schat_share";	/*共享数据*/
    public static final int PGPREFS_MOD = Activity.MODE_PRIVATE;
    public static boolean ServerSettingRecved = false;
    public static Properties prop = null;

    private static String passwd_key_alias = "ssccaatt_ppaass";
    public static String user_local_des_key = "";
    private static KeyStoreUtil key_store_util = new KeyStoreUtil(); //key store

    public static int LOGIN_SERVER_STICKY = 600; //within period login last logout server.
    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat min_format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static int MAX_AUDIO_SECONDS = 30;
    //IMAGE
    public static int UPLOAD_NORMAL_FILE_SIZE = (10 * 1024 * 1024);	/*上传文件最大尺寸*/
    public static int MAX_IMG_SIZE= (10 * 1024 * 1024); //default 最大10M
    //public static final int HEAD_FILE_SIZE = (40 * 1024);
    public static void TryNotifyImgSize(Context context , int file_size) {
        if(file_size >= AppConfig.MAX_IMG_SIZE) {
            AppConfig.PrintInfo(context , "图片大小超过" + AppConfig.MAX_IMG_SIZE/1024/1024 + "M , 上传时将进行压缩");
        }
    }
    public static final int CHAT_IMG_STANDARD_SIZE = 150; //dp
    public static final int CHAT_IMG_COMPRESS_SIZE = (40 * 1024);
    public static final int USER_HEAD_STANDARD_SIZE = 60; //dp


    //CHAT SERV
    public static String ChatServHost = ""; //
    public static int    ChatServPort = 0; //17908;
    public static boolean ZlibOn = true; //zlib

    // KEY WORDS
    //public static final String KEY_USER_NAME = "user";	/*本次登录的用户名*/
    public static final String KEY_VALIDATE_TIME = "validate_time";	/*成功验证的时间*/
    public static final String KEY_SAVED_USER = "saved_name";	/*保存的用户名*/
    public static final String KEY_SAVED_PASS = "saved_pass";	/*保存的密码*/
    public static final String KEY_SERVER_SPACE = "server_space"; /*保存的服务器名*/
    public static final String KEY_DIR_ADDR = "server_dir_addr"; /*保存的服务器dir地址*/
    public static final String KEY_CONN_SERV_IP = "conn_serv_ip"; //上一次登陆的conn_serv
    public static final String KEY_CONN_SERV_PORT = "conn_serv_port";
    public static final String KEY_LAST_EXIT = "last_logout"; //上一次退出时间
    public static final String KEY_REQ_TIMEOUT = "req_timeout"; //请求超时时限

    //摘要
    public static String pub_key_sha2 = ""; //public key sha256 hash code
    //---------
    /**
     * SHA256hash
     * @param str
     * @return
     */
    public static String String2SHA256StrJava(String str){
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }

    /**
     * SHA256hash
     * @param content
     * @return
     */
    public static String Bytes2SHA256StrJava(byte[] content){
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(content);
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }


    /**
     * 将byte转为16进制
     * @param bytes
     * @return
     */
    private static String byte2Hex(byte[] bytes){
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i=0;i<bytes.length;i++){
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length()==1){
                //1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    //LOGIN
    public static String UserName;
    public static long UserUid;
    public static final int USER_NAME_LEN = 32;
    public static final int PASSWORD_LEN = 12;
    public static int login_result = -1;
    public static UserInfo user_info;
    //public static boolean change_grp_name = false;

    public static boolean IsLogin() {
        if(UserName!=null && UserName.length()>0) {
            return true;
        }
        return false;
    }

    public static int HEART_BEAT_FREQUENT =  10; //hearbeat second to server
    public static int MAX_DIS_RECONN_TIMES = 3;
    private static int DisReConnTimes = 0;
    public static Activity main_act = null;
    //断线重连
    public static void DisConnReConn() {
        String log_label = "DisConnReConn";
        if(IsLogin() == false) {//must already login before
            Log.i(log_label , "not login before yet!");
            return;
        }
        if(DisReConnTimes >= MAX_DIS_RECONN_TIMES) {
            Log.e(log_label , "reconn:" + DisReConnTimes + " times overflow!");
            return;
        }

        DisReConnTimes++;
        SharedPreferences shared_data = main_act.getSharedPreferences(AppConfig.PGPREFS, AppConfig.PGPREFS_MOD);
        String enc_pass = shared_data.getString(KEY_SAVED_PASS , "");
        String real_pass = AppConfig.DecryptPasswd(UserName , enc_pass);
        if(TextUtils.isEmpty(real_pass)) {
            Log.e(log_label , "pass error!");
            return;
        }

        //Send Login
        String req = CSProto.CsLoginReq(UserName , real_pass , version);
        SendMsg(req);
        Log.d(log_label , "dis_reconn times:" + DisReConnTimes);
    }

    private static String GetPassAlias(String user_name) {
        return AppConfig.ServerSpace + "_" + user_name + "||" + passwd_key_alias;
    }

    public static String DecryptPasswd(String user_name , String encrypted_pass) {
        String log_label = "DecryptPasswd";
        //check alias
        String key_alias = GetPassAlias(user_name);
        if(!key_store_util.AliasExist(key_alias)) {
            Log.e(log_label , "alias not exist!");
            return "";
        }

        if(TextUtils.isEmpty(encrypted_pass)) {
            Log.e(log_label , "encrypted_pass is empty!");
            return "";
        }


        //base64 decoded
        byte[] b64_decoded = Base64.decode(encrypted_pass , Base64.DEFAULT);
        if(b64_decoded == null) {
            Log.e(log_label , "base64 decoded fail! src:" + encrypted_pass);
            return "";
        }

        //decrypt
        byte[] result = key_store_util.Decrypt(b64_decoded , key_alias);
        if(result == null) {
            Log.e(log_label , "decrypt failed! src:" + encrypted_pass);
            return "";
        }

        Log.d(log_label , "success! alias:" + key_alias);
        return new String(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String EncryptPasswd(Context context , String user_name , String raw_pass) {
        String log_label = "EncryptPasswd";
        String key_alias = GetPassAlias(user_name);
        if(TextUtils.isEmpty(key_alias)) {
            Log.e(log_label , "key_alias is empty!");
            return "";
        }
        //key_store_util.DeleteKey(key_alias);
        //check alias
        if(!key_store_util.AliasExist(key_alias)) {
            Log.i(log_label , "alias not exist, will create it");

            KeyPair kp = key_store_util.GenerateKey(context , key_alias);
            if(kp == null) {
                Log.e(log_label , "generate alias failed!");
                return "";
            }
        }

        if(TextUtils.isEmpty(raw_pass)) {
            Log.e(log_label , "raw pass is empty!");
            return "";
        }

        //decrypt
        byte[] result = key_store_util.Encrypt(raw_pass.getBytes() , key_alias);
        if(result == null) {
            Log.e(log_label , "decrypt failed! from:" + raw_pass);
            return "";
        }

        String base64_encrypted = Base64.encodeToString(result, Base64.DEFAULT);
        Log.d(log_label , "success! alias:" + key_alias + " result:" + base64_encrypted);
        return base64_encrypted;
    }


    //FLAG
    public static final char SEX_MALE = 1;
    public static final char SEX_FEMALE = 2;
    public static int reg_result = -1;
    public static int create_group_result = -1;
    public static int update_user_self = -1;

    //APPLY
    public static int apply_result = -1;

    //Lock
    public static boolean TryLockBool(AtomicBoolean lock) {
        int v = 0;
        try {
            while(v <= REQ_TIMEOUT) {
                if(lock.compareAndSet(false , true)) {
                    return true; //locked
                }
                Thread.sleep(300);
                v++;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void UnLockBool(AtomicBoolean lock) {
        lock.set(false);
    }


    //Cache
    public static ChatGroupCache chat_group_cache = new ChatGroupCache();
    public static UserProfileCache user_profile_cache = new UserProfileCache();
    public static MessageCache message_cache = new MessageCache();

    //CHAT
    public static int CHAT_TIME_DISPLAY_SPAN = (5 * 60); //5min
    public static boolean ChatTimeInSpan(long ts1 , long ts2) {
        long diff = Math.abs(ts1 - ts2);
        if(diff < CHAT_TIME_DISPLAY_SPAN) {
            return true;
        }
        return false;
    }
    public static final int SCHAT_FACE_SIZE = 30;	//30dp
    public static final int SCHAT_FACE_COUNT = 16;	//16个表情
    public static final int SCHAT_FACE_ALIGN = ImageSpan.ALIGN_BOTTOM;
    public static SpannableString String2EmotionSp(Context context , Resources res , String src_content) {
        //表情
        SpannableString sp = new SpannableString(src_content);

        int start;
        int end;
        int curr_head = 0;
        String face_name;
        while(true)
        {
            //寻找所有_xx_的标记
            start = src_content.indexOf('_', curr_head);
            if(start<0)
                break;
            curr_head = start+1;

            end = src_content.indexOf('_', curr_head);
            if(end<0)
                break;

            //__
            if(start+1 >= end)
                continue;

            //获取中间值
            face_name = src_content.substring(start+1, end);
            if(face_name==null || face_name.length()<=0 || face_name.length()>6)
                continue;
            int face_offset;
            try
            {
                face_offset = Integer.parseInt(face_name);
            }
            catch (NumberFormatException e)
            {
                // TODO: handle exception
                continue;
            }

            if(face_offset<0 || face_offset>=AppConfig.SCHAT_FACE_COUNT)
                continue;

            //设置表情
            Drawable my_face = res.getDrawable(R.drawable.face_001+face_offset);
            my_face.setBounds(0, 0, dip2px(context, AppConfig.SCHAT_FACE_SIZE),
                    dip2px(context, AppConfig.SCHAT_FACE_SIZE));
            ImageSpan span = new ImageSpan(my_face, AppConfig.SCHAT_FACE_ALIGN);
            sp.setSpan(span, start, end+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sp;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    //GroundGroup
    public static GroundGroup groupd_groups = new GroundGroup();
    public static GroupSnapCache grp_snap_cache = new GroupSnapCache();
    //ChatMsg
    //public static ConcurrentHashMap<Long , RecvChatNew> recv_chat_new_cache = new ConcurrentHashMap<>();
    public static AtomicBoolean chat_history_lock = new AtomicBoolean(false);//history is sync
    /** REQUEST CODE**/
    public static final int FILE_SELECT_CODE=1;	//选择图片
    public static final int FILE_CROP_CODE = 2;	//裁剪图片
    public static final int USE_CAM_CODE = 3;	//使用相机
    public static final int FILE_SELECT_VIDEO = 4; //选择视频文件

    //MiscInfo
    public static MiscInfo miscInfo = new MiscInfo();

    //SQLITE
    public static SQLiteDatabase db = null;

    //DIR
    public static final String DATA_DIR = "schat";	/*外部数据目录*/
    public static String ABSOLUTE_DATA_DIR_PATH = null;	/*绝对路径*/
    public static String USER_HEAD_DIR_PATH = null;	//用户头像目录
    public static String CHAT_MAIN_DIR_PATH = null; //聊天主目录
    public static String GROUP_HEAD_DIR_PATH = null; //群组头像主目录
    public static String TEMP_MISC_DIR_PATH = null;  //临时文件目录
    public static int GROUP_FILE_DIR_MAX = 23;

    public static String GrpId2FileDir(long grp_id) {
        return "g_" + (grp_id % GROUP_FILE_DIR_MAX);
    }

    public static void CreateGroupFileDir(long grp_id) {
        String log_label = "CreateGroupFileDir";
        //group dir
        String g_dir_name = GrpId2FileDir(grp_id);
        String dir_path = AppConfig.CHAT_MAIN_DIR_PATH + "/" + g_dir_name;
        File f_dir = new File(dir_path);
        if(!f_dir.exists())
        {
            f_dir.mkdirs();
            Log.i(log_label , "create group dir:" + dir_path);
        }
        Log.d(log_label , "grp_id:" + grp_id + " group dir:" + dir_path);
    }


    public static void DelTempMiscFile(String file_name) {
        String log_label = "DelTempMiscFile";
        //check
        if(TextUtils.isEmpty(file_name)) {
            Log.e(log_label , "file name empty!");
            return;
        }

        String file_path = AppConfig.TEMP_MISC_DIR_PATH + "/" + file_name;
        File local_file = new File(file_path);
        if(local_file.exists() == false) {
            Log.d(log_label , "file not exist! file_path:" + file_path);
            return;
        }

        //del
        Log.d(log_label , "del finish! file:" + file_path);
        local_file.delete();
    }

    public static byte[] ReadTempMiscFile(String file_name) {
        String log_label = "ReadTempMiscFile";

        //check
        if(TextUtils.isEmpty(file_name)) {
            Log.e(log_label , "file name empty!");
            return null;
        }

        String file_path = AppConfig.TEMP_MISC_DIR_PATH + "/" + file_name;
        //exist?
        File local_file = new File(file_path);
        if(local_file.exists() == false) {
            Log.d(log_label , "file not exist! file_path:" + file_path);
            return null;
        }

        //load
        try
        {
            InputStream fips = new FileInputStream(file_path);
            byte[] data = ReadInput2Bytes(fips);

            if(data == null)
                return null;
            return  data;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    public static String RecordAudioFile = "";
    public static String GetTempMiscFilePath(String file_name) {
        if(TextUtils.isEmpty(file_name))
            return null;
        return AppConfig.TEMP_MISC_DIR_PATH + "/" + file_name;
    }

    public static boolean SaveTempMiscFile(String file_name , byte[] data) {
        String log_label = "SaveTempMiscFile";
        if(TextUtils.isEmpty(file_name)) {
            Log.e(log_label , "file name empty!");
            return false;
        }

        //path
        String file_path = AppConfig.TEMP_MISC_DIR_PATH + "/" + file_name;
        File local_file = new File(file_path);
        if(local_file.exists()) {
            Log.d(log_label , "file exist! file_path:" + file_path);
            return true;
        }

        //real save
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(local_file));
            bos.write(data);
            bos.flush();
            bos.close();
            Log.i(log_label, "save " + file_name + " success");
            return true;
        }
        catch (Exception e)
        {
            Log.i(log_label, "save " + file_name + " failed");
            e.printStackTrace();
            return false;
        }
    }


    /*
    public static String EncLocalStr(String str , String enc_key) {
        enc_key.to
        MyEncrypt.EncryptAesCBC()
    }

     */


    public static boolean checkNet(Context context)
    {
        //获得手机所有连接管理对象（包括对wi-fi等连接的管理）
        try
        {
            ConnectivityManager connectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivity != null)
            {
                //获得网络连接管理的对象
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if(info != null && info.isConnected())
                {
                    //判断当前网络是否已连接
                    if(info.getState() == NetworkInfo.State.CONNECTED);
                    return true;
                }
            }
        }
        catch (Exception e)
        {

        }
        return false;
    }


    //SYNC FLAG
    public static AtomicBoolean common_op_lock = new AtomicBoolean(false);


    public static void PrintInfo(Context context , String str) {
        Toast.makeText(context , str , Toast.LENGTH_SHORT).show();
    }


    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static byte[] getRandomBytes(int length){
        Random random=new Random();
        byte[] bts = new byte[length];
        random.nextBytes(bts);
        return bts;
    }


    //check name
    public static int CheckName(Context context , String str , int length)
    {
        int i;
        char all_number = 1;

        if(str.length()<=0 || str.length()>length)
        {
            AppConfig.PrintInfo(context, "名长度不能大于" + length);
            return -1;
        }

        /*特殊字符*/
        if(str.indexOf(' ') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有空格");
            return -1;
        }

        if(str.indexOf('\'') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有单引号");
            return -1;
        }

        if(str.indexOf('\n') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有回车");
            return -1;
        }

        if(str.indexOf('\"') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有双引号");
            return -1;
        }

        if(str.indexOf('/') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有斜杠");
            return -1;
        }

        if(str.indexOf('\\') >= 0)
        {
            AppConfig.PrintInfo(context, "名中不能有斜杠");
            return -1;
        }

        /*全数字*/
        if(str.matches("\\d*"))
        {
            AppConfig.PrintInfo(context, "名不能全为数字");
            return -1;
        }

        return 0;
    }

    //check result 0: success -1: failed
    public static int CheckPassWord(Context context , String str , int min_pass_len , int max_pass_len)
    {
        String log_label = "CheckPassWord";
        if(str.length()<min_pass_len || str.length()>max_pass_len)
        {
            AppConfig.PrintInfo(context, "密码长度为" + min_pass_len + " ~ " + max_pass_len);
            Log.e(log_label , "pass:" + str + " len:" + str.length());
            return -1;
        }

        /*只能是字符或者数字*/
        String pattern = "[a-zA-Z0-9]+";
        if(!str.matches(pattern))
        {
            AppConfig.PrintInfo(context, "密码只能是字符或者数字");
            return -1;
        }

        return 0;
    }

    //time
    public static long CurrentUnixTime() {
        long timeStamp = System.currentTimeMillis() / 1000;
        return timeStamp;
    }
    public static long CurrentUnixTimeMilli() {
        long timeStamp = System.currentTimeMillis();
        return timeStamp;
    }
    public static String ConverUnixTime2Str(long UnixTime)
    {
        String result;
        Date date = new Date(UnixTime * 1000);

        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));	/*GMT+8为北京时间，转化成本地*/
        result = format.format(date);
        return result;
    }
    //print by minute
    public static String ConverUnixTime2MinStr(long UnixTime)
    {
        String result;
        Date date = new Date(UnixTime * 1000);

        min_format.setTimeZone(TimeZone.getTimeZone("GMT+8"));	/*GMT+8为北京时间，转化成本地*/
        result = min_format.format(date);
        return result;
    }

    public static void SendMsg(final String cmd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatClient.SendMsg(cmd);
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    //query storage permission after 6.0
    private  static final int REQUEST_EXTERNAL_STORAGE = 1;
    private  static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }

        permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /*
     * 质量压缩Bitmap
     * 内存不会变 但会改变对应的字节数组大小
     * level:目标KB
     */
    public static Bitmap compressImage(Bitmap image , int level)
    {
        String log_label = "compressImage";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中

        if(baos.toByteArray().length/1024 < level)	//小于level则不用压缩了
        {
            return image;
        }
        Log.d(log_label , "before size:" + (image.getByteCount()/1024) + "K");
        int options = 100;
        while ( baos.toByteArray().length/1024>level) {  //循环判断如果压缩后图片是否大于levelkb,大于继续压缩
            Log.d(log_label , "options:" + options + " size:" + baos.toByteArray().length/1024);
            if(options <=1)
                break;
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }

        byte[] bytes = baos.toByteArray();
        Log.d(log_label, "final options:" + options + " size:" + bytes.length/1024);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Log.d("compress" , "after size:" + (bitmap.getByteCount()/1024) + "K");
        //回收之前的image内存
        if(image!=null && !image.isRecycled())
        {
            try
            {
                image.recycle();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    //scale:will shrink to 1/scale
    public static Bitmap compressBitmapbySize(Bitmap src , float scale) {
        String log_label = "compressBitmapbySize";
        Log.d(log_label, "before size:" + (src.getByteCount() / 1024)
                + "K w:" + src.getWidth() + " h:" + src.getHeight());

        //<=40K no need
        if(src.getByteCount()/1024 <= 40) {
            return src;
        }
        //matrix
        Matrix matrix = new Matrix();
        float dst_scale = 1/scale;
        //dst_scale = Math.round(dst_scale * 10) / 10;
        Log.d(log_label , "scale:" + dst_scale);
        matrix.setScale(dst_scale, dst_scale);

        //compress
        Bitmap bm = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        Log.d(log_label, "after size:" + (bm.getByteCount() / 1024)
                + "K w:" + bm.getWidth() + " h:" + bm.getHeight() + " scale:" + dst_scale);
        return bm;
    }

    public static Bitmap compressImage565(Bitmap bmp_src) {
        String log_label = "compressImage565";
        Log.d(log_label, "before bmp size:" + bmp_src.getByteCount()/1024 + "k h:" +
                bmp_src.getHeight() + " w:" + bmp_src.getWidth());
        //create
        Bitmap bmp_dst = Bitmap.createBitmap(bmp_src.getWidth(), bmp_src.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp_dst);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bmp_src, 0, 0, paint);
        Log.d(log_label, "after bmp size:" + bmp_dst.getByteCount()/1024 + "k h:" +
                bmp_dst.getHeight() + " w:" + bmp_dst.getWidth());
        return bmp_dst;
    }


    //compress to 565
    public static Bitmap compressImage565_bak(Bitmap bmp_src) {
        String log_label = "compressImage565";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        //to stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp_src.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        byte[] bytes = baos.toByteArray();
        Log.d(log_label, "before bmp size:" + bmp_src.getByteCount()/1024 + "k bytes:" + bytes.length/1024 + "k" + " h:" +
                bmp_src.getHeight() + " w:" + bmp_src.getWidth());


        Bitmap bmp_dst = BitmapFactory.decodeByteArray(bytes , 0 , bytes.length , options);
        //Bitmap bitmap = BitmapFactory.decodeFile(+ "/compresstest/test.png", options);
        if(bmp_dst != null) {
            Log.d(log_label, "after bmp size:" + bmp_src.getByteCount()/1024 + "k bytes:" + bytes.length/1024 + "k" + " h:" +
                    bmp_src.getHeight() + " w:" + bmp_src.getWidth());
        }

        return bmp_dst;
    }

    //degree 压缩率 比如60 表示压缩到60%
    public static InputStream Bitmap2IS(Bitmap bm , int degree)
    {
        String log_label = "Bitmap2IS";
        if(bm == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        Log.d(log_label , "before bmp size:" + bm.getByteCount()/1024 + "k" + " bytes:" + baos.toByteArray().length/1024 + "k");
        baos.reset();

        bm.compress(Bitmap.CompressFormat.JPEG, degree, baos);
        Log.d(log_label , "after bmp size:" + bm.getByteCount()/1024 + "k" + " bytes:" + baos.toByteArray().length/1024 + "k");
        InputStream sbs = new ByteArrayInputStream(baos.toByteArray());
        return sbs;
    }

    /*
     * 得到图片字节流 数组大小
     * */
    public static byte[] ReadInput2Bytes(InputStream is) throws Exception
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        if(is == null)
        {
            return null;
        }

        byte[] buffer = new byte[1024*100];
        int len = 0;

        while((len=is.read(buffer)) > 0)
        {

            outStream.write(buffer, 0, len);
        }

        outStream.close();
        is.close();
        return outStream.toByteArray();
    }

    public static Bitmap ZoomHeadBitmap(Bitmap bitmapOrg)
    {
        if(bitmapOrg == null)
            return null;
        //获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();

        int newWidth;
        int newHeight;
        //定义预转换成的图片的宽度和高度

        if(width > height)
        {
            newWidth = 250;
            newHeight = 250*height/width; //按比例缩放
        }
        else
        {
            newHeight = 250;
            newWidth = 250*width/height;
        }


        //计算缩放率，新尺寸除原始尺寸
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();

        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);

        // 创建新的图片
        Bitmap bitmapDest = Bitmap.createBitmap(bitmapOrg, 0, 0,
                width, height, matrix, true);

        //回收
        /*
        if(bitmapOrg != null && !bitmapOrg.isRecycled())
        {
            // 回收并且置为null
        	bitmapOrg.recycle();
        	bitmapOrg = null;

        }
        System.gc();*/
        return bitmapDest;
    }

    public static Bitmap toRoundHeadBitmap(Bitmap bitmap_src)
    {
        Bitmap bitmap = null;
        if(bitmap_src == null)
            return null;

        bitmap = ZoomHeadBitmap(bitmap_src);
        if(bitmap == null)
            return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left,top,right,bottom,dst_left,dst_top,dst_right,dst_bottom;
        if (width <= height)
        {
            roundPx = width / 2;
            top = 0;
            bottom = width;
            left = 0;
            right = width;
            height = width;
            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        }
        else
        {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;
            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }

        Bitmap output = Bitmap.createBitmap(width,
                height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src = new Rect((int)left, (int)top, (int)right, (int)bottom);
        final Rect dst = new Rect((int)dst_left, (int)dst_top, (int)dst_right, (int)dst_bottom);
        final RectF rectF = new RectF(dst);

        paint.setAntiAlias(true);

        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);	//画个圆角矩形，其内部是圆形

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));	//设置图形交错时的显示，后来的显示
        canvas.drawBitmap(bitmap, src, dst, paint);

        return output;
//		return AppConfig.ZoomBitmap(bitmap);
    }


    public static final int LOCAL_IMG_HEAD = 1;
    public static final int LOCAL_IMG_CHAT = 2;
    public static final int LOCAL_IMG_GRP_HEAD = 3;
    /*
     * 读取本地图片
     */
    public static Bitmap ReadLocalImg(int local_type , long grp_id , String file_name)
    {
        String log_label = "ReadLocalImg";
        Bitmap bit_map = null;
        byte[] data = null;
        String file_path = null;

        //check type
        switch (local_type) {
            case LOCAL_IMG_HEAD:
                if(AppConfig.USER_HEAD_DIR_PATH == null) {
                    Log.e(log_label , "head dir path null!");
                    break;
                }
                file_path = AppConfig.USER_HEAD_DIR_PATH + "/" + file_name;
                break;
            case LOCAL_IMG_CHAT:
                if(AppConfig.CHAT_MAIN_DIR_PATH == null) {
                    Log.e(log_label , "chat main dir path null!");
                    break;
                }
                file_path = AppConfig.CHAT_MAIN_DIR_PATH + "/" + AppConfig.GrpId2FileDir(grp_id) + "/" + file_name;
                break;
            case LOCAL_IMG_GRP_HEAD:
                if(AppConfig.GROUP_HEAD_DIR_PATH == null) {
                    Log.e(log_label , "grp_head dir path null!");
                    break;
                }
                file_path = AppConfig.GROUP_HEAD_DIR_PATH + "/" + file_name;
                break;
            default:
                Log.e(log_label , "illegal local_type:" + local_type);
                break;
        }

        //check
        if(file_path==null || file_path.length()<=0) {
            Log.e(log_label , "file path illegal!");
            return null;
        }

        //exist?
        File local_file = new File(file_path);
        if(local_file.exists() == false) {
            Log.d(log_label , "file not exist! file_path:" + file_path);
            return null;
        }

        //load
        try
        {
            InputStream fips = new FileInputStream(file_path);
            data = ReadInput2Bytes(fips);

            if(data == null)
                return null;
            bit_map = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return bit_map;
    }

    public static void ClearAllLocalDir() {
        String log_label = "ClearAllLocalDir";
        //clear head
        if(AppConfig.USER_HEAD_DIR_PATH != null) {
            Log.i(log_label , "will clear dir:" + AppConfig.USER_HEAD_DIR_PATH);
            File head_dir = new File(AppConfig.USER_HEAD_DIR_PATH);
            DeleteFile(head_dir);
        }

        //clear chat
        if(AppConfig.CHAT_MAIN_DIR_PATH != null) {
            Log.i(log_label , "will clear dir:" + AppConfig.CHAT_MAIN_DIR_PATH);
            File chat_dir = new File(AppConfig.CHAT_MAIN_DIR_PATH);
            DeleteFile(chat_dir);
        }

        //group head
        if(AppConfig.GROUP_HEAD_DIR_PATH != null) {
            Log.i(log_label , "will clear dir:" + AppConfig.GROUP_HEAD_DIR_PATH);
            File head_dir = new File(AppConfig.GROUP_HEAD_DIR_PATH);
            DeleteFile(head_dir);
        }

        Log.d(log_label , "finish!");
    }


    public static void DeleteFile(File file){
        String log_label = "DeleteFile";
        if(file.isFile()){//判断是否为文件，是，则删除
            Log.i(log_label , "del_file:" + file.getAbsoluteFile().toString());//打印路径
            file.delete();
            return;
        }
        //不为文件，则为文件夹
        String[] childFilePath = file.list();//获取文件夹下所有文件相对路径
        for (String path:childFilePath){
            File childFile= new File(file.getAbsoluteFile()+"/"+path);
            DeleteFile(childFile);//递归，对每个都进行判断
        }
        Log.i(log_label , "is_dir:" + file.getAbsoluteFile().toString());
        //file.delete();
    }

    public static String VideoSnapName(String file_name) {
        return file_name + "_snap";
    }

    public static String FormatTimeStr(long video_time_second) {
        String result = "";
        if(video_time_second >= 3600) {
            result = String.format("%02d:%02d'" , video_time_second/3600 , video_time_second%3600);
            return result;
        }

        if(video_time_second >= 60) {
            result = String.format("%02d':%02d''" , video_time_second/60 , video_time_second%60);
            return result;
        }

        result = String.format("%02d''" , video_time_second);
        return result;
    }


    /**
     * 得到amr的时长
     *
     * @param file_path
     * @return amr文件时间长度
     * @throws IOException
     */
    public static int getAmrDuration(String file_path) throws IOException {
        if(TextUtils.isEmpty(file_path))
            return -1;
        long duration = -1;
        int[] packedSize = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0,
                0, 0 };
        File file = new File(file_path);
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            // 文件的长度
            long length = file.length();
            // 设置初始位置
            int pos = 6;
            // 初始帧数
            int frameCount = 0;
            int packedPos = -1;
            // 初始数据值
            byte[] datas = new byte[1];
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(datas, 0, 1) != 1) {
                    duration = length > 0 ? ((length - 6) / 650) : 0;
                    break;
                }
                packedPos = (datas[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }
            // 帧数*20
            duration += frameCount * 20;
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return (int)((duration/1000)+1);
    }

    //get normal file path; or return ""
    public static String getNormalChatFilePath(long grp_id , String file_name) {
        String log_label = "existNormalChatFile";
        String file_path = null;

        //get dir
        if(AppConfig.CHAT_MAIN_DIR_PATH == null) {
            Log.e(log_label , "chat main dir path null!");
            return "";
        }
        file_path = AppConfig.CHAT_MAIN_DIR_PATH + "/" + AppConfig.GrpId2FileDir(grp_id) + "/" + file_name;
        //check
        if(file_path==null || file_path.length()<=0) {
            Log.e(log_label , "file path illegal! path:" + file_path);
            return "";
        }

        //check
        File local_file = new File(file_path);
        if(local_file.exists()) {
            //Log.d(log_label , "file exist! file_path:" + file_path);
            return file_path;
        }

        Log.i(log_label , "file not exist! path:" + file_path);
        return "";
    }


    //save normal chat file exclude image
    public static boolean saveNormalChatFile(long grp_id , byte[] data , String file_name) {
        String log_label = "saveNormalChatFile";
        String file_path = null;

        if(data == null) {
            Log.e(log_label , "data null!");
            return false;
        }

        //get dir
        if(AppConfig.CHAT_MAIN_DIR_PATH == null) {
            Log.e(log_label , "chat main dir path null!");
            return false;
        }
        file_path = AppConfig.CHAT_MAIN_DIR_PATH + "/" + AppConfig.GrpId2FileDir(grp_id) + "/" + file_name;

        //check
        if(file_path==null || file_path.length()<=0) {
            Log.e(log_label , "file path illegal!");
            return false;
        }

        //save
        //File head_file = new File(AppConfig.USER_HEAD_DIR_PATH + "/" + fileName + ".jpg");
        File local_file = new File(file_path);
        if(local_file.exists()) {
            Log.d(log_label , "file exist! file_path:" + file_path);
            return true;
        }

        //real save
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(local_file));
            bos.write(data);
            bos.flush();
            bos.close();
            Log.i(log_label, "save " + file_name + " success" + " path:" + file_path);
        }
        catch (Exception e)
        {
            Log.i(log_label, "save " + file_name + " failed");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 保存文件
     * @param bm
     * @param file_name
     * @throws
     */
    public static boolean saveLocalImg(int local_type , long grp_id , Bitmap bm, String file_name)
    {
        String log_label = "saveLocalImg";
        String file_path = null;

        if(bm == null) {
            Log.e(log_label , "bitmap null!");
            return false;
        }

        //check type
        switch (local_type) {
            case LOCAL_IMG_HEAD:
                if(AppConfig.USER_HEAD_DIR_PATH == null) {
                    Log.e(log_label , "head dir path null!");
                    break;
                }
                file_path = AppConfig.USER_HEAD_DIR_PATH + "/" + file_name;
                break;
            case LOCAL_IMG_CHAT:
                if(AppConfig.CHAT_MAIN_DIR_PATH == null) {
                    Log.e(log_label , "chat main dir path null!");
                    break;
                }
                file_path = AppConfig.CHAT_MAIN_DIR_PATH + "/" + AppConfig.GrpId2FileDir(grp_id) + "/" + file_name;
                break;
            case LOCAL_IMG_GRP_HEAD:
                if(AppConfig.GROUP_HEAD_DIR_PATH == null) {
                    Log.e(log_label , "group_head dir path null!");
                    break;
                }
                file_path = AppConfig.GROUP_HEAD_DIR_PATH + "/" + file_name;
                break;
            default:
                Log.e(log_label , "illegal local_type:" + local_type);
                break;
        }

        //check
        if(file_path==null || file_path.length()<=0) {
            Log.e(log_label , "file path illegal!");
            return false;
        }


        //save
        //File head_file = new File(AppConfig.USER_HEAD_DIR_PATH + "/" + fileName + ".jpg");
        File local_file = new File(file_path);
        if(local_file.exists()) {
            Log.d(log_label , "file exist! file_path:" + file_path);
            return true;
        }

        //real save
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(local_file));
            bm.compress(Bitmap.CompressFormat.JPEG, 60, bos);
            bos.flush();
            bos.close();
            Log.i(log_label, "save " + file_name + " success");
        }
        catch (Exception e)
        {
            Log.i(log_label, "save " + file_name + " failed");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /* refer server
    CHAT_FILE URL> 1:index:grp_id:file_name  | FILE_DIR> CHAT_PARENT_PATH/GROUP_ID/YYYYMM/ | FILE_NAME> YYYYMM_MD5.TYPE
    HEAD_FILE URL> 2:index:sub_dir:file_name | FILE_DIR> HEAD_PARENT_PATH/SUB_DIR/UID/ | FILE_NAME> UID_MD5.TYPE
    GROUP_FILE URL> 3:index:sub_dir:file_name | FILE_DIR> GROUP_HEAD_PARENT_PATH/SUB_DIR/GRPID/ | FILE_NAME> GRPID_MD5.TYPE
     */
    public static final int URL_TYPE_CHAT = 1; //prefix: static
    public static final int URL_TYPE_HEAD = 2; //prefix: head
    public static final int URL_TYPE_GROUP_HEAD = 3; //prefix: g_head

    //get file_name from url
    //1:2:5016:202011_61dab386f4b4a9a53a970866040c5fdc_.jpg --> 61dab386f4b4a9a53a970866040c5fdc_.jpg
    public static String Url2RealFileName(String server_url) {
        String log_label = "Url2RealFileName";
        //check arg
        if(server_url==null || server_url.length()<=0) {
            Log.e(log_label , "url illegal!");
            return "";
        }

        //parse
        String[] strs = server_url.split(":");
        if(strs==null || strs.length!=4) {
            Log.e(log_label , "url split failed! server_url:" + server_url);
            return "";
        }

        //splice file
        String file_name = strs[3];
        String[] real_strs = file_name.split("_");
        if(real_strs==null || real_strs.length!=3) {
            Log.e(log_label , "file_name split failed! file_name:" + file_name);
            return "";
        }

        String real_name = real_strs[1]+real_strs[2];
        Log.d(log_label , "real_name:" + real_name + " url:" + server_url);
        return real_name;
    }



    //src: "chat_url":"1:2:5016:202011_61dab386f4b4a9a53a970866040c5fdc_.jpg"
    //src: "head_url":"2:2:20:10004_f7d02aeae4c2e4faec47195cf7667608_.jpeg"
    //src: "group_head_url":"3:1:24:5026_e9095dc1eb1480aa045f0cc8048f2837_.jpeg"
    //dst: http://file_addr/static/5016/202011/202011_61dab386f4b4a9a53a970866040c5fdc_.jpg
    //dst: http://file_addr/head/20/10004/10004_f7d02aeae4c2e4faec47195cf7667608_.jpeg?token=xxx
    //dst: http://file_addr/g_head/24/5026/5026_e9095dc1eb1480aa045f0cc8048f2837_.jpeg
    //results[0]== file_name if success
    public static String ParseServerUrl(String server_url) {
        String log_label = "ParseServerUrl";
        //check arg
        if(server_url==null || server_url.length()<=0) {
            Log.e(log_label , "url illegal!");
            return "";
        }

        //parse
        String[] strs = server_url.split(":");
        if(strs==null || strs.length!=4) {
            Log.e(log_label , "url split failed! server_url:" + server_url);
            return "";
        }

        //fill
        int url_type = Integer.parseInt(strs[0]);
        int serv_index = Integer.parseInt(strs[1]);
        long extra_id = Long.parseLong(strs[2]);
        String file_name = strs[3];
        //Log.d(log_label , "url_type:" + url_type + " index:" + serv_index + " extra_id:" + extra_id + " file:" + file_name);

        //file serv info
        FileServInfo file_info = AppConfig.miscInfo.get_file_info(serv_index);
        if(file_info == null) {
            Log.e(log_label , "serv_index illegal! index:" + serv_index);
            return "";
        }

        String query_path = "";
        //url_type to query_path
        switch (url_type) {
            case URL_TYPE_CHAT:
                query_path = ParseChatUrl(extra_id , file_name);
                break;
            case URL_TYPE_HEAD:
                query_path += ParseHeadUrl(extra_id , file_name);
                break;
            case URL_TYPE_GROUP_HEAD:
                query_path += ParseGroupHeadUrl(extra_id , file_name);
                break;
            default:
                Log.e(log_label , "illegal url_type:" + url_type);
                break;
        }

        if(query_path==null || query_path.length()<=0) {
            Log.e(log_label , "generate query_path failed!");
            return "";
        }

        //generate full query
        String full_query = "https://" + file_info.Addr + "/" + query_path + "?token=" + file_info.Token +"&uid=" + AppConfig.UserUid;
        //Log.d(log_label , "full_query:" + full_query);
        return full_query;
    }

    //dst: static/5016/202011/202011_61dab386f4b4a9a53a970866040c5fdc_.jpg
    private static String ParseChatUrl(long extra_id , String file_name) {
        String log_label = "ParseChatUrl";
        //basic
        String query_path = "static/" + extra_id + "/";

        //parse file_name
        String[] strs = file_name.split("_");
        if(strs == null || strs.length!=3) {
            Log.e(log_label , "file name split failed! file_name:" + file_name + " split:" + strs.length);
            return "";
        }

        //get uid
        String date = strs[0];
        query_path = query_path + date + "/" + file_name;
        Log.d(log_label , "query_path:" + query_path);
        return query_path;
    }


    //dst: head/20/10004/10004_f7d02aeae4c2e4faec47195cf7667608_.jpeg
    private static String ParseHeadUrl(long extra_id , String file_name) {
        String log_label = "ParseHeadUrl";
        //basic
        String query_path = "head/" + extra_id + "/";

        //parse file_name
        String[] strs = file_name.split("_");
        if(strs == null || strs.length!=3) {
            Log.e(log_label , "file name split failed! file_name:" + file_name + " split:" + strs.length);
            return "";
        }

        //get uid
        String uid = strs[0];
        query_path = query_path + uid + "/" + file_name;
        Log.d(log_label , "query_path:" + query_path);
        return query_path;
    }


    //dst: g_head/24/5026/5026_e9095dc1eb1480aa045f0cc8048f2837_.jpeg
    private static String ParseGroupHeadUrl(long extra_id , String file_name) {
        String log_label = "ParseGroupHeadUrl";
        //basic
        String query_path = "g_head/" + extra_id + "/";

        //parse file_name
        String[] strs = file_name.split("_");
        if(strs == null || strs.length!=3) {
            Log.e(log_label , "file name split failed! file_name:" + file_name + " split:" + strs.length);
            return "";
        }

        //get grp_id
        String grp_id = strs[0];
        query_path = query_path + grp_id + "/" + file_name;
        Log.d(log_label , "query_path:" + query_path);
        return query_path;
    }


    //get file_serv by index. if index<=0 get a random one
    public static FileServInfo GetFileServ(int index) {
        String log_label = "RandFileServ";
        //check
        if(AppConfig.miscInfo == null) {
            Log.e(log_label , "misc info null");
            return null;
        }
        return AppConfig.miscInfo.get_file_info(index);
    }



    public static String FileAddr2UploadUrl(String addr) {
        return "https://" + addr + "/upload/";
    }

    public static String ReadInputStream(InputStream input_stream)
    {
        int buff_size = 2000;
        String str = "";
        int read_count;
        char[] read_buff = new char[buff_size];

        /*read*/
        InputStreamReader input_reader = new InputStreamReader(input_stream);
        try
        {
            while((read_count = input_reader.read(read_buff)) > 0)
            {

                String read_str = String.copyValueOf(read_buff, 0, read_count);
                str += read_str;
                read_buff = new char[buff_size];
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return str;
        }

        return str;
    }

    public static final int FILE_TIME_OUT = 10 * 10000000; // 超时时间
    public static final String CHARSET = "utf-8"; // 设置编码
    /*
     * 上传多个文件及text
     */
    public static InputStream uploadFile(String target_url , HashMap<String, Object>map , ArrayList<InputStream> img_list)
    {
        InputStream in = null;

        String BOUNDARY = UUID.randomUUID().toString(); // 边界标识 随机生成
        String PREFIX = "--", LINE_END = "\r\n";
        String CONTENT_TYPE = "multipart/form-data"; // 内容类型


        /***Arg Check*/
        if(map==null || img_list==null)
            return null;

        /***Get Info*/
        /***Upload*/
        try
        {
            URL url = new URL(target_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(FILE_TIME_OUT);
            conn.setConnectTimeout(FILE_TIME_OUT);
            conn.setDoInput(true); // 允许输入流
            conn.setDoOutput(true); // 允许输出流
            conn.setUseCaches(false); // 不允许使用缓存
            conn.setRequestMethod("POST"); // 请求方式
            conn.setRequestProperty("Charset", CHARSET); // 设置编码
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);

            /**
             * 文件输入流
             */
            OutputStream outputSteam = conn.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputSteam);


            StringBuffer sb = new StringBuffer();
            /*
             * 基本信息
             */
            Iterator<String> iter = map.keySet().iterator();
            while(iter.hasNext())
            {
                String key;
                String value;

                key = iter.next();
                if(map.get(key) == null)	//为空继续
                    continue;
                value = map.get(key).toString();

                //头
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINE_END);
                //内容
                sb.append("Content-Disposition: form-data; name=\"" + key + "\""+ LINE_END + LINE_END);
//                sb.append(URLEncoder.encode(value, "UTF-8")); 这里不使用编码，因为是multipart/form-data类型
                sb.append(value);
                sb.append(LINE_END);
            }
            dos.write(sb.toString().getBytes());

            /*
             * 发送FILE
             */
            int real_index = 0;
            //only support 1 file
            for(int i=0; i<img_list.size(); i++)
            {
                InputStream file_is = img_list.get(i);
                sb = new StringBuffer();

                if (file_is != null)
                {
                    //写入请求头
                    sb.append(PREFIX);
                    sb.append(BOUNDARY);
                    sb.append(LINE_END);

                    //String up_load_name = "UploadFile" + real_index;
                    String up_load_name = "upload_file";

                    sb.append("Content-Disposition: form-data; name=\"" + up_load_name +"\";" +
                            "filename=\"img"+ real_index + "\"" + LINE_END);
                    sb.append("Content-Type: application/octet-stream" + LINE_END + LINE_END);
                    dos.write(sb.toString().getBytes());

                    byte[] bytes = new byte[1024*10];
                    int len = 0;
                    while ((len = file_is.read(bytes)) != -1)
                    {
                        dos.write(bytes, 0, len);
                    }
                    file_is.close();
                    dos.write(LINE_END.getBytes());
                    real_index++;

                }
            }

            //最后的尾部
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END)
                    .getBytes();
            dos.write(end_data);
            dos.flush();	//使用chunked编码，不用设置content-length
            dos.close();


            /**
             * 获取响应码 200=成功 当响应成功，获取响应的流
             */
            int res = conn.getResponseCode();
            if(res == HttpURLConnection.HTTP_OK)
            {
                in = conn.getInputStream();
            }

        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return in;
    }

    private static class MyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)

                throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            // TODO Auto-generated method stub
            return null;
        }

    }
    //private static MyTrustManager my_truster = new MyTrustManager();


    private static class MyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // TODO Auto-generated method stub
            return true;
        }

    }
    //private static MyHostnameVerifier my_host_verifer = new MyHostnameVerifier();

    public static boolean HttpsCertNoCheck() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new MyTrustManager()},
                    new SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection
                    .setDefaultHostnameVerifier(new MyHostnameVerifier());
            return true;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return false;
        }

    }

    //Self Cert

    public static boolean HttpCertSelf(Context context , String bks_file , String bks_pass) {
        if(TextUtils.isEmpty(bks_file) || TextUtils.isEmpty(bks_pass))
            return false;

        try {
            TrustManagerFactory trustManagerFactory = null;
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // 从资源文件中读取你自己创建的那个包含证书的 keystore 文件

            //InputStream in = context.getResources().openRawResource(R.raw.key); //这个参数改成你的 keystore 文件名
            InputStream in = context.getAssets().open(bks_file);
            // 用 keystore 的密码跟证书初始化 trusted
            trusted.load(in, bks_pass.toCharArray());
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
            trustManagerFactory.init(trusted);

            //set SSLContext
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection
                    .setDefaultHostnameVerifier(new MyHostnameVerifier());

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (CertificateException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return  false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return  false;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return  false;
        }
        return true;
    }



    public static InputStream openHttpsConnGet(String https_url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        InputStream in = null;
        int response = -1;

        //not need valid cert
        /*
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] { my_truster },
                new SecureRandom());
        HttpsURLConnection
                .setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection
                .setDefaultHostnameVerifier(my_host_verifer);

         */


        URL url = new URL(https_url);
        URLConnection conn = url.openConnection();

        if(!(conn instanceof HttpURLConnection))
        {
            throw new IOException("Not a Http Connection");
        }

        try
        {
            HttpURLConnection http_conn = (HttpURLConnection)conn;
            http_conn.setAllowUserInteraction(false);
            http_conn.setInstanceFollowRedirects(true);
            http_conn.setRequestMethod("GET");
            http_conn.setConnectTimeout(5000);
            http_conn.setReadTimeout(10000);
            http_conn.setRequestProperty("Charset", "UTF-8");
            http_conn.connect();


            response = http_conn.getResponseCode();
            if(response == HttpURLConnection.HTTP_OK)
            {
                in = http_conn.getInputStream();
            }

        }
        catch(Exception e)
        {
            throw new IOException("error connecting");
        }

        return in;
    }

    public static InputStream openHttpConnGet(String http_url) throws IOException
    {
        InputStream in = null;
        int response = -1;

        URL url = new URL(http_url);
        URLConnection conn = url.openConnection();

        if(!(conn instanceof HttpURLConnection))
        {
            throw new IOException("Not a Http Connection");
        }

        try
        {
            HttpURLConnection http_conn = (HttpURLConnection)conn;
            http_conn.setAllowUserInteraction(false);
            http_conn.setInstanceFollowRedirects(true);
            http_conn.setRequestMethod("GET");
            http_conn.setRequestProperty("Charset", "UTF-8");
            http_conn.connect();


            response = http_conn.getResponseCode();
            if(response == HttpURLConnection.HTTP_OK)
            {
                in = http_conn.getInputStream();
            }

        }
        catch(Exception e)
        {
            throw new IOException("error connecting");
        }

        return in;
    }

}

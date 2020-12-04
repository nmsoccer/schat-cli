package com.app.schat;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ChatClient {
    //PKG_LEN
    private static int PKG_MAX_SIZE = 20 * 1024;
    private static int DES_KEY_LEN = 8;
    private static int AES_KEY_LEN = 16;

    //LOG LABEL
    private static String log_conn = "chat_conn";
    private static String log_send = "chat_send";
    private static String log_recv = "chat_recv";
    private static String log_close = "chat_close";

    private static boolean sock_close = false;
    private static boolean validate = false;
    private static int enc_type = 0;    //encrypt type 0:no 1:des-ecb 2:aes-cbc-128 3:rsa+des
    private static byte[] des_key;
    private static byte[] aes_key;

    //STAT
    private static int STAT_NONE = 0;
    private static int STAT_ING = 1;
    private static int STAT_DONE = 2;

    //SOCK KERNEL STRUCT
    private static int sock_stat = STAT_NONE;
    private static Socket sock = null;
      //for read
    private static byte[] recv_cache = new byte[PKG_MAX_SIZE];
    private static byte[] recv_buff = new byte[2 * PKG_MAX_SIZE];
    private static int recv_buff_rest = 0; //rest data in recv_buff
    private static byte[] recv_pkg_buff = new byte[PKG_MAX_SIZE];
    private static int[] pkg_attr = new int[2];
    private static int tag;
    private static int readed = 0;

      //for send
    private static int pkg_len = 0;
    private static byte[] snd_pkg_buff = new byte[PKG_MAX_SIZE];

    private static DataOutputStream out = null;
    private static DataInputStream in = null;

    /*
    Check Connection
     */
    public static boolean InConnect() {
        //Log.i("check_conn" , "stat:" + sock_stat + " sock_close:" + sock_close);
        if(sock_stat == STAT_NONE || sock_close) {
            return false;
        }
        return true;
    }

    private static boolean valid_conn(Socket client) throws IOException{
        String _func_ = "<valid_conn>";
        byte[] pkg_buff;
        int pkg_len;
        int data_len;
        int tag;
        int i;
        int ret = 0;

        pkg_buff = new byte[1024];
        //Valid
        String cmd = NetPkg.CONN_VALID_KEY;

        //pack cmd
        pkg_len = NetPkg.PackPkg(pkg_buff , cmd.getBytes() , NetPkg.PKG_OP_VALID);
        if(pkg_len <= 0){
            System.out.printf("%s pack failed! pkg_len:%d" , _func_ , pkg_len);
            return false;
        }

        //send
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        out.write(pkg_buff , (int)0 , pkg_len);
        System.out.printf(">>send cmd:%s data_len:%d pkg_len:%d success!\n" , cmd , cmd.length() , pkg_len);


        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean nego_des_key(Socket client , String des_key , String rsa_pub_key) {
        String _func_ = "<nego_des_key>";
        byte[] enc_data;
        byte[] pkg_buff;
        int pkg_len;
        int data_len;
        int tag;
        int i;
        int ret = 0;

        pkg_buff = new byte[1024];
        //encrypt des_key
        try {
            enc_data = MyEncrypt.EncryptRsa(des_key, rsa_pub_key);
        }catch (Exception e) {
            Log.e(log_recv , _func_ + " encrypt failed!");
            e.printStackTrace();
            return false;
        }

        if(enc_data == null) {
            return false;
        }

        //pack cmd
        pkg_len = NetPkg.PackPkg(pkg_buff , enc_data , NetPkg.PKG_OP_RSA_NEGO);
        if(pkg_len <= 0){
            System.out.printf("%s pack failed! pkg_len:%d" , _func_ , pkg_len);
            return false;
        }

        //send
        try {
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.write(pkg_buff, (int) 0, pkg_len);
            System.out.printf(">>send des_key:%s pkg_len:%d success!\n", des_key, pkg_len);
        }catch (IOException e) {
            Log.e(log_send , _func_ + " snd pkg failed!");
            e.printStackTrace();
            return false;
        }
        return true;
    }



    /*
    Check Connection
     */
    public static void Connect(String host , int port) {
        try {
            //close old connection
            if(sock_close) {
                CloseSock();
                sock_close = false;
            }


            if(sock_stat != STAT_NONE) {
                return;
            }

            sock_stat = STAT_ING;
            Log.i(log_conn , "host:" + host + " port:" + port + " connecting...");

            sock = new Socket(host, port);
            if (sock != null) {
                sock_stat = STAT_DONE;
                Log.i(log_conn, "connect success!");
                //init stream
                OutputStream outToServer = sock.getOutputStream();
                out = new DataOutputStream(outToServer);

                InputStream inFromServer = sock.getInputStream();
                in = new DataInputStream(inFromServer);

                //validate
                valid_conn(sock);
                return;
            } else {
                Log.i(log_conn , "conn to " + host + ":" + port + " blocked!");
                sock_stat = STAT_NONE;
            }
        }catch (IOException e) {
            e.printStackTrace();
            Log.i(log_conn , "conn to " + host + ":" + port + " failed!");
            sock_stat = STAT_NONE;
        }

    }

    /*
    Send Msg
     */
    public static void SendMsg(String msg) {
        if(sock_stat!=STAT_DONE || sock==null || out == null || sock_close || !validate) {
            Log.i(log_send , "connection not ready!");
            return;
        }

        Log.i(log_send , msg);
        //compress
        byte[] com_data = msg.getBytes();
        if(AppConfig.ZlibOn) {
            com_data = ZLibTool.compress(msg.getBytes());
        }

        //encrypt
        byte[] enc_data = EncryptSndMsg(com_data);

        //check
        if(enc_data == null) {
            //Log.e(log_send , "emtpy encrypt data");
            return;
        }

        //pack cmd
        pkg_len = NetPkg.PackPkg(snd_pkg_buff , enc_data , NetPkg.PKG_OP_NORMAL);
        if(pkg_len <= 0){
            Log.e(log_send , "pack failed! pkg_len:" + pkg_len + " msg:" + msg);
            return;
        }


        try {
            //out.write(msg.getBytes());
            out.write(snd_pkg_buff , 0 , pkg_len);
        }catch (IOException e) {
            e.printStackTrace();
            Log.e(log_send , "send:" + msg + "failed for bad connection!");
            sock_close = true;
        }
    }

    /*
    Spec Msg
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void RecvSpecMsg(byte option , byte[] pkg_data) {
        Log.i(log_recv , "spec_data:" + (new String(pkg_data)));
        switch(option)
        {
            case NetPkg.PKG_OP_ECHO:
                Log.i(log_recv , "spec_echo:" + option + "content:" + (new String(pkg_data)));
            case NetPkg.PKG_OP_VALID:
                validate = true;
                enc_type = pkg_data[0];
                Log.i(log_recv , "spec_valid, enc_type: " + enc_type);
                if(enc_type == NetPkg.NET_ENCRYPT_NONE) {
                    Log.i(log_recv , "enc_type none!");
                    break;
                }
                if(enc_type == NetPkg.NET_ENCRYPT_DES_ECB) {
                    des_key = new byte[DES_KEY_LEN]; //init des key
                    System.arraycopy(pkg_data , 1 , des_key , 0 , DES_KEY_LEN);
                    Log.i(log_recv , "enc_type use des_ecb! key:" + (new String(des_key)));
                    break;
                }
                if(enc_type == NetPkg.NET_ENCRYPT_AES_CBC_128) {
                    aes_key = new byte[AES_KEY_LEN]; //init aes key
                    System.arraycopy(pkg_data , 1 , aes_key , 0 , AES_KEY_LEN);
                    Log.i(log_recv , "enc_type use aes_cbc! key:" + (new String(aes_key)));
                    break;
                }
                if(enc_type == NetPkg.NET_ENCRYPT_RSA) {
                    byte[] rsa_pub_key = new byte[pkg_data.length - 1];
                    System.arraycopy(pkg_data , 1 , rsa_pub_key , 0 , pkg_data.length-1);
                    Log.i(log_recv , "enc_type use rsa! key:" + (new String(rsa_pub_key)));

                    //nego des key
                    validate = false; //must confirm by server
                    des_key = new byte[DES_KEY_LEN];
                    String key = AppConfig.getRandomString(DES_KEY_LEN); //des key
                    System.arraycopy(key.getBytes() , 0 , des_key , 0 , DES_KEY_LEN);
                    if(! nego_des_key(sock , key , new String(rsa_pub_key))) {
                        Log.e(log_recv , "nego_des_key failed! return false!");
                    }
                    break;
                }
                Log.e(log_recv , "unkown enc_type: " + enc_type);
                validate = false;
                break;
            case NetPkg.PKG_OP_RSA_NEGO:
                Log.i(log_recv , "spec_nego");
                if(pkg_data[0]=='o' && pkg_data[1]=='k') {
                    Log.i(log_recv , "nego des key success!");
                    validate = true;

                    //断线重连
                    AppConfig.DisConnReConn();
                } else {
                    Log.e(log_recv , "nego failed!");
                    des_key = null;
                    validate = false;
                }
                break;
            default:
                Log.i(log_recv , "unkown spec option:" + option);
                break;
        }
    }


    private static byte[] EncryptSndMsg(byte[] src_data) {
        byte[] enc_data = null;

        switch (enc_type) {
            case NetPkg.NET_ENCRYPT_NONE:
                enc_data = src_data;
                break;
            case NetPkg.NET_ENCRYPT_DES_ECB:
            case NetPkg.NET_ENCRYPT_RSA:
                if(des_key == null){
                    Log.i(log_send, "enc_type:des but key not set!");
                    break;
                }
                //encrypt
                try {
                    enc_data = MyEncrypt.EncryptDesECB(src_data, des_key);
                    //Log.i(log_send , "enc des success!");
                }catch (Exception e) {
                    Log.e(log_send , "encrypt data" + new String(src_data) + "failed!");
                    e.printStackTrace();
                }
                break;
            case NetPkg.NET_ENCRYPT_AES_CBC_128:
                if(aes_key == null){
                    Log.i(log_send, "enc_type:aes but key not set!");
                    break;
                }
                //encrypt
                try {
                    enc_data = MyEncrypt.EncryptAesCBC(src_data, aes_key);
                    //Log.i(log_send , "enc aes success!");
                }catch (Exception e) {
                    Log.e(log_send , "encrypt data" + new String(src_data) + "failed!");
                    e.printStackTrace();
                }
                break;
            default:
                Log.e(log_send , "unkown enc_type:" + enc_type);
                break;
        }

        return enc_data;
    }

    private static byte[] DecryptRecvMsg(byte[] msg) {
        switch(enc_type) {
            case NetPkg.NET_ENCRYPT_NONE:
                return msg;
            case NetPkg.NET_ENCRYPT_DES_ECB:
            case NetPkg.NET_ENCRYPT_RSA:
                if(!validate || des_key==null || des_key.length <=0) {
                    Log.e(log_recv , "des_key not set!");
                    return null;
                }
                try {
                    return MyEncrypt.DecryptDesECB(msg, des_key);
                }catch (Exception e) {
                    Log.e(log_recv , "decrpyt failed! msg:" + msg);
                    e.printStackTrace();
                    return null;
                }
            case NetPkg.NET_ENCRYPT_AES_CBC_128:
                if(!validate || aes_key==null || aes_key.length <=0) {
                    Log.e(log_recv , "aes_key not set!");
                    return null;
                }
                try {
                    return  MyEncrypt.DecryptAesCBC(msg, aes_key);
                }catch (Exception e) {
                    Log.e(log_recv , "decrpyt failed! msg:" + msg);
                    e.printStackTrace();
                    return null;
                }
            default:
                Log.e(log_recv , "unkown enc_type:" + enc_type);
                break;
        }

        return null;
    }


    /*
    Recv Msg
    @results:pkgs
    @return:pkg count
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static int RecvMsg(String[] pkgs) {
        int pkg_count = 0;
        if(sock_stat!=STAT_DONE || sock==null || in == null || sock_close) {
            Log.i(log_recv , "connection not ready!");
            return pkg_count;
        }

        try {
            //recv from server
            readed = in.read(recv_cache);
            if (readed <= 0) {
                Log.i(log_recv, "empty:" + readed + " will close sock");
                sock_close = true;
                return 0;
            }


            //cache to buff
            if ((recv_buff.length - recv_buff_rest) < readed) {
                Log.e(log_recv, "recv_buff overflow!");
                sock_close = true;
                return 0;
            }
            System.arraycopy(recv_cache, 0, recv_buff, recv_buff_rest, readed);
            recv_buff_rest += readed;
            Log.d(log_recv, "recv new bytes:" + readed + " and recv_buff_rest:" + recv_buff_rest);

            //unpack to pkgs
            while (true) {
                //check rest data
                if(recv_buff_rest <= 0) {
                    //no more data
                    break;
                }

                //pkg enough
                if(pkg_count >= pkgs.length) {
                    Log.d(log_recv , "pkg enough! pkg_count:" + pkg_count);
                    return pkg_count;
                }


                //unpack
                tag = NetPkg.UnPackPkg(recv_buff, recv_pkg_buff, pkg_attr);
                if (tag == 0xFF) {
                    Log.e(log_recv, "unpack failed!");
                    sock_close = true;
                    return pkg_count;
                }
                if (tag == 0xEF) {
                    Log.e(log_recv, "pkg_buff not enough!");
                    sock_close = true;
                    return pkg_count;
                }
                if (tag == 0) {
                    Log.i(log_recv, "data not ready!");
                    return pkg_count;
                }


                //get recv_content pkg_attr[0]:data_len; pkg_attr[1]:pkg_len
                int pkg_len = pkg_attr[1];
                Log.d(log_recv, "tag:" + tag + " data_len:" + pkg_attr[0] + " pkg_len:" + pkg_len);
                byte[] result = new byte[pkg_attr[0]];
                System.arraycopy(recv_pkg_buff, 0, result, 0, pkg_attr[0]);

                //relocate recv_buff
                if (pkg_len == recv_buff_rest) {
                    Log.i(log_recv, "complete pkg!" + recv_buff_rest + ":" + pkg_len);
                    recv_buff_rest = 0; //complete pkg
                } else {
                    Log.i(log_recv, "will resize recv_buff_rest " + recv_buff_rest + " to new" + (recv_buff_rest - pkg_len));
                    if (recv_buff_rest < pkg_len) {
                        Log.e(log_recv, "recv_buff is less than pkg_len!! error!" + recv_buff_rest + ":" + pkg_len);
                        sock_close = true;
                    } else {
                        //rest data in recv_buff
                        System.arraycopy(recv_buff, pkg_len, recv_buff, 0, recv_buff_rest - pkg_len);
                        recv_buff_rest -= pkg_len;
                    }
                }

                //spec pkg
                if (NetPkg.PkgOption((byte) tag) != NetPkg.PKG_OP_NORMAL) {
                    RecvSpecMsg(NetPkg.PkgOption((byte) tag), result);
                    return 0;
                }

                //normal pkg
                //decrpyt
                byte[] decoded = DecryptRecvMsg(result);
                if (decoded == null) {
                    return pkg_count;
                }
                //Log.i(log_recv , "decrpyt success");
                //uncompressing
                byte[] decom = decoded;
                if (AppConfig.ZlibOn) {
                    decom = ZLibTool.decompress(decoded);
                    if (decom == null) {
                        return pkg_count;
                    }
                }
                pkgs[pkg_count] = new String(decom);
                pkg_count++;
            }

        }catch(IOException e){
            e.printStackTrace();
            Log.e(log_recv, "recv failed for bad connection!");
            sock_close = true;
            return 0;
        }

        return pkg_count;
    }


    /*
    Close Socket
     */
    public static void CloseSock() {
        try {
            if (sock != null) {
                sock.close();
                sock = null;
                sock_stat = STAT_NONE;

                //close streeam
                out.close();
                out = null;

                in.close();
                in = null;

                //other
                recv_buff_rest = 0;

                Log.i(log_close , "close sock success!");
            }
        }catch (IOException e) {
            e.printStackTrace();
            Log.e(log_close , "close sock failed!");
        }
    }

}

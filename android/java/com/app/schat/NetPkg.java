package com.app.schat;

/*
=====This is a JAVA API file for parse server-client transport pkg in SGame Framework=====
* more info:https://github.com/nmsoccer/sgame
=======================================================================================
*NET-PKG
Tag   +    Len         +          Value
(1B)     (1|2|4B)                 (...)
|<-    head     ->|           |<- data ->|


*Tag: 1Byte
**Tag: 0 0 0 0 0 | 0 0 0  high-5bits:option , low-3bits: Bytes of len

*Len: 1 or 2 or 4Byte
**len [1,0xFF] :1Byte
**len (0xFF , 0xFFFF] :2Byte
**len (0xFFFF , 0xFFFFFFFF]: 4Byte

*/
public class NetPkg {

    public final static int TAG_LEN  = 1;
    public final static int INT_MAX  = 0x7FFFFFF0;
    //pkg option
    public final static byte PKG_OP_NORMAL = 0;  //normal pkg
    public final static byte PKG_OP_ECHO =  1;  //echo client <-> tcp-serv
    public final static byte PKG_OP_VALID   = 2;  //valid connection client-->server[validate] server-->client[enc_key if enc enable]
    public final static byte PKG_OP_RSA_NEGO = 3;  //encrypt by rsa_pub_key to negotiate des key client-->server[encrypted key] server-->client[result]
    public final static byte PKG_OP_MAX =  32; //max option value

    //VALID_KEY
    public final static String CONN_VALID_KEY = "c#s..x*.39&suomeI./().32&show+me_tHe_m0ney$";

    //ENC_TYPE
    public final static byte NET_ENCRYPT_NONE = 0;
    public final static byte NET_ENCRYPT_DES_ECB = 1; //desc-ecb
    public final static byte NET_ENCRYPT_AES_CBC_128 = 2; //aes-cbc-128
    public final static byte NET_ENCRYPT_RSA  = 3; //rsa + des

    /*
    Pack pkg_data to pkg_buff
    @pkg_buff:dst buff
    @pkg_data:src data
    @pkg_option: ==0 normal pkg.  > 0 PKG_OP_XX means special pkg to server
    @return: -1:failed -2:buff_len not enough >0:success(pkg_len)
    */
    public static int PackPkg(byte[] pkg_buff , byte[] pkg_data, byte pkg_option) {
        int buff_len = pkg_buff.length;
        int data_len = pkg_data.length;
        int head_len = -1;

        //pack head
        head_len = pack_head(pkg_buff , data_len , pkg_option);
        if(head_len<=0)
            return -1;

        //buff enough?
        if(buff_len < head_len + data_len)
            return -2;

        //copy
        System.arraycopy(pkg_data, 0, pkg_buff, head_len, data_len);
        return head_len + data_len;
    }


    /*
    UnPackPkg from raw data
    @raw: src raw data which will unpack from
    @pkg_buff:dst buff which store pkg-data if success
    @pkg_attr:if success will store data_len:pkg_attr[0] pkg_len:pkg_attr[1]; if fail store nothing
    @return:pkg_tag
    pkg_tag 0xFF:error , 0xEF:buff_len not enough 0:raw data not ready , else:success and valid tag of pkg
             if success , tag is valid and will set pkg_attr
    */
    public static int UnPackPkg(byte[] raw , byte[] pkg_buff , int[] pkg_attr) {
        int raw_len = raw.length;
        int buff_len = pkg_buff.length;
        int head_len = 0;
        int data_len = 0;
        int tag = 0;
        int[] head_attr = new int[2];

        //Get Head
        head_len = unpack_head(raw , head_attr);
        if(head_len < 0)
            return 0xFF;

        //Not Ready
        if(head_len == 0)
            return 0;

        tag = head_attr[0];
        data_len = head_attr[1];
        //raw not enough
        if(raw_len < data_len + head_len)
            return 0;

        //buff_len not enough
        if(buff_len < data_len)
            return 0xEF;


        //cpy
        System.arraycopy(raw, head_len , pkg_buff, 0, data_len);
        pkg_attr[0] = data_len;
        pkg_attr[1] = data_len + head_len;
        return tag;
    }

    //Get pkg-option
    //@return:PKG_OP_XX
    public static byte PkgOption(byte tag)
    {
    	return (byte)(((int)tag >> 3) & 0xFF);
    }


    //predict pkg-len according to data_len
    //-1:if data_len illegal else pkg-len
    public static int GetPkgLen(int data_len)
    {
    	if(data_len <= 0 || data_len >= INT_MAX)
    		return -1;

    	if(data_len <= 0xFF)
    		return TAG_LEN + 1 + data_len;

    	if(data_len <= 0xFFFF)
    		return TAG_LEN + 2 + data_len;

    	if(data_len >= INT_MAX)
    		return -1;

    	return TAG_LEN + 4 + data_len;
    }


    /*
    pack head to buff
    @return:  -1:failed else:success(head_len)
    */
    private static int pack_head(byte[] buff , int data_len , byte pkg_option) {
        int buff_len = buff.length;

        if(data_len <= 0 || data_len >= INT_MAX)
            return -1;

        //lenth:1Byte
        if(data_len <= 0xFF)
        {
            if(buff_len < TAG_LEN+1)
                return -1;

            buff[0] = 0x01; //tag
            buff[0] |= (pkg_option << 3);
            buff[1] = (byte)data_len;
            return 1 + TAG_LEN;
        }

        //lenth:2Byte
        if(data_len <= 0xFFFF) {
            if(buff_len < TAG_LEN+2)
                return -1;

            buff[0] = 0x02; //tag
            buff[0] |= (pkg_option << 3);
                //java default big endian?
            buff[1] = (byte)((data_len >> 8) & 0xFF);
            buff[2] = (byte)((data_len) & 0xFF);
            return TAG_LEN + 2;
        }

        //lenth:4Byte
        if(buff_len < TAG_LEN+4)
    	    return -1;

        buff[0] = 0x04; //tag
        buff[0] |= (pkg_option << 3);
        buff[1] = (byte)((data_len >> 24) & 0xFF);
        buff[2] = (byte)((data_len >> 16) & 0xFF);
        buff[3] = (byte)((data_len >> 8) & 0xFF);
        buff[4] = (byte)(data_len & 0xFF);
        return TAG_LEN + 4;
    }

    /*
    unpack head from buff
    @head_attr: [0]:tag [1]:data_len
    @return 0:data not-ready, -1:failed , ELSE:success (head_len)
    */
    private static int unpack_head(byte[] buff , int[] head_attr) {
        int buff_len = buff.length;
        byte b_len;

        if(buff_len < TAG_LEN)
            return 0;

        //bytes of len
        b_len = (byte)(buff[0] & 0x07);
        if(b_len!=1 && b_len!=2 && b_len!=4)
            return -1;


        //1Byte
        if(b_len == 0x01) {
            if(buff_len < TAG_LEN+1)
    	        return 0;

            head_attr[0] = (int)(buff[0] & 0xFF);
            head_attr[1] = (int)(buff[1] & 0xFF);
            return TAG_LEN + 1;
        }


        //2Byte
        if(b_len == 0x02) {
    	    if(buff_len < TAG_LEN+2)
    		    return 0;

    	    head_attr[0] = (int)(buff[0] & 0xFF);
    	    head_attr[1] = ((int)(buff[1] & 0xFF) << 8) | ((int)(buff[2] & 0xFF));

    	    return TAG_LEN + 2;
        }

        //4Byte
	    if(buff_len < TAG_LEN+4)
		    return 0;

	    head_attr[0] = (int)(buff[0] & 0xFF);
        head_attr[1] = ((int)(buff[1] & 0xFF) << 24) | ((int)(buff[2] & 0xFF) << 16) | ((int)(buff[3] & 0xFF) << 8) | ((int)(buff[4] & 0xFF));
        return TAG_LEN + 4;
    }



}
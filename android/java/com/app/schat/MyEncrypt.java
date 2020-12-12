package com.app.schat;


import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MyEncrypt
{
    /*-----------------------DES---------------------*/
    /**
     * Encrypt data to server
     * @param src_data
     * @param encryptKey
     * @return
     * @throws Exception
     */
    public static byte[] EncryptDesECB(byte[] src_data, byte[] encryptKey) throws Exception {

        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey, "DES"));
        byte[] encryptedData = cipher.doFinal(src_data);
        //byte[] encryptedData = cipher.doFinal(encryptString.getBytes());
        return encryptedData;
    }


    /***
     * Decrypt data from Server
     * @param decryptString
     * @param decryptKey
     * @return
     * @throws Exception
     */
    public static byte[] DecryptDesECB(byte[] decryptString, byte[] decryptKey) throws Exception {

        byte[] sourceBytes = decryptString;
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "DES"));
        byte[] decoded = cipher.doFinal(sourceBytes);
        //return new String(decoded, "UTF-8");
        return decoded;
    }


    /*-----------------------AES---------------------*/
    /**
     * Encrypt data to server
     * @param src_data
     * @param encryptKey
     * @return
     * @throws Exception
     */
    public static byte[] EncryptAesCBC(byte[] src_data, byte[] encryptKey) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(encryptKey);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey, "AES") , iv);
        byte[] encryptedData = cipher.doFinal(src_data);
        //byte[] encryptedData = cipher.doFinal(encryptString.getBytes());
        return encryptedData;
    }

    /***
     * Decrypt data from Server
     * @param decryptString
     * @param decryptKey
     * @return
     * @throws Exception
     */
    public static byte[] DecryptAesCBC(byte[] decryptString, byte[] decryptKey) throws Exception {

        byte[] sourceBytes = decryptString;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(decryptKey);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "AES") , iv);
        byte[] decoded = cipher.doFinal(sourceBytes);
        //return new String(decoded, "UTF-8");
        return decoded;
    }

    /*-----------------------RSA---------------------*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static PublicKey loadPublicKey(String publicKeyPEM) throws Exception {

        // strip of header, footer, newlines, whitespaces
        publicKeyPEM = publicKeyPEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        // decode to get the binary DER representation
        byte[] publicKeyDER = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyDER));
        return publicKey;
    }


    /**
     * Encrypt data to server
     * @param trans_key
     * @param
     * @return
     * @throws Exception
     * RSA from openssl pkcs1padding
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] EncryptRsa(byte[] trans_key, String rsa_pub_key) throws Exception {
        //get public key
        PublicKey pb_key = loadPublicKey(rsa_pub_key);

        //encrypt
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pb_key);
        byte[] encryptedData = cipher.doFinal(trans_key);
        //byte[] encryptedData = cipher.doFinal(encryptString.getBytes());
        return encryptedData;
    }


}

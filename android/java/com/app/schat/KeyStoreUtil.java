package com.app.schat;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

public class KeyStoreUtil {
    private KeyStore keyStore;
    private X500Principal x500Principal; //自签署证书
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    public KeyStoreUtil() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            /**
             *   CN      commonName
             *   O       organizationName
             *   OU      organizationalUnitName
             *   C       countryName
             * */
            x500Principal = new X500Principal("CN=SChat, OU=Nothing, O=None, C=None");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * alias exist
     * */
    public boolean AliasExist(String alias) {
        if (keyStore == null || TextUtils.isEmpty(alias)){
            return false;
        }

        boolean exist = false;
        try{
            exist = keyStore.containsAlias(alias);
        }catch (Exception e){
            e.printStackTrace();
        }
        return exist;
    }

    /**
     * generate new key by alias
     *
     * @param context
     * @param alias alias of new key
     * */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public KeyPair GenerateKey(Context context, String alias){
        if (AliasExist(alias)){
            return null;
        }

        try {
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 10);

            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context.getApplicationContext())
                    .setAlias(alias)
                    .setSubject(x500Principal)
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(Calendar.getInstance().getTime())
                    .setEndDate(endDate.getTime())
                    .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(spec);

            return  generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        return null;
    }

    public void DeleteKey(String alias){
        try{
            keyStore.deleteEntry(alias);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Encrypt
     *
     * @param data raw data
     * @param alias key of alias
     * */
    public byte[] Encrypt(byte[] data, String alias){
        try {
            //取出密钥
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypt
     *
     * @param data encrypted data
     * @param alias KeyStore中的别名
     * */
    public byte[] Decrypt(byte[] data, String alias){
        try {
            //取出密钥
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            PrivateKey privateKey = privateKeyEntry.getPrivateKey();

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }


}

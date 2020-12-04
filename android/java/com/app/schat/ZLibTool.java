package com.app.schat;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZLibTool {
    private static int BUFF_LEN = 10*1024;
    private static Inflater decompresser = new Inflater();
    private static Deflater compresser = new Deflater();
    /**
     * 压缩
     *
     * @param data
     *            待压缩数据
     * @return byte[] 压缩后的数据
     */
    public static byte[] compress(byte[] data) {
        byte[] output = new byte[0];

        //Deflater compresser = new Deflater();
        compresser.reset();
        compresser.setInput(data);
        compresser.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[BUFF_LEN];
            while (!compresser.finished()) {
                int i = compresser.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //compresser.end();
        return output;
    }

    /**
     * 解压缩
     *
     * @param data
     *            待压缩的数据
     * @return byte[] 解压缩后的数据
     */
    public static byte[] decompress(byte[] data) {
        byte[] output = new byte[0];

        //Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data , 0 , data.length);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[BUFF_LEN];
            Log.i("uncompress:" , "1A:" + data.length + " remain:" + decompresser.getRemaining());
            //while (!decompresser.finished()) {
            int i = decompresser.inflate(buf);
            o.write(buf, 0, i);
            Log.i("uncompress:" , "inflate:" + i+ " 1:" + decompresser.needsInput() + " 2:" + decompresser.needsDictionary() +
                    " 3:" + decompresser.finished());
            if(i == 0) {
                Log.e("uncompress:" , "inflate:" + i);
                return null;
            }
                //break;
            //}
            output = o.toByteArray();
        } catch (DataFormatException d) {
            Log.i("uncompress:" , "inflate failed!");
            d.printStackTrace();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //decompresser.end();
        return output;
    }

}

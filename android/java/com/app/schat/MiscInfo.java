package com.app.schat;

import android.util.Log;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

class FileServInfo {
    public int Index;
    public String Token;
    public String Addr;

}


public class MiscInfo {
    private ConcurrentHashMap<Integer , FileServInfo> file_map;


    public void set_file_map(ConcurrentHashMap<Integer, FileServInfo> file_map) {
        this.file_map = file_map;
    }

    //get file_serv by index. if index<=0 get a random one
    public FileServInfo get_file_info(int index) {
        String log_label = "get_file_info";
        if(file_map == null || file_map.size()<=0) {
            Log.e(log_label , "file_map null!");
            return null;
        }

        if(index <= 0) {
            Integer[] keys = file_map.keySet().toArray(new Integer[0]);
            Log.d(log_label , "key length:" + keys.length);
            if(keys.length == 1) {
                return file_map.get(keys[0]);
            }

            //random one
            Random random = new Random();
            Integer randomKey = keys[random.nextInt(keys.length)];
            Log.d(log_label , "random key:" + randomKey);
            return file_map.get(randomKey);
        }


        if(!file_map.containsKey(index)) {
            Log.e(log_label , "index illegal! index:" + index);
            return null;
        }

        return file_map.get(index);

    }

}

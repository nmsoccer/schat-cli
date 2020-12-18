package com.app.schat;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    public static Properties load_properties(Context context) {
        Properties prop = new Properties();

        try {
            InputStream in = context.getAssets().open("config");
            prop.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return prop;
    }

}

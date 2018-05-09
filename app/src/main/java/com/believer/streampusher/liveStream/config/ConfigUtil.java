package com.believer.streampusher.liveStream.config;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by believer on 2018/5/2.
 */

public class ConfigUtil {
    private static  ConfigUtil configUtil = null;
    private  ConfigUtil(){

    }
    public  static  ConfigUtil getinstance(){
       if(configUtil==null)
           configUtil =  new ConfigUtil();
       return  configUtil;
    }
    public Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    public void saveConfig(Context context, String file, Properties properties) {
        try {
            FileOutputStream s = new FileOutputStream(file, false);
            properties.store(s, "");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

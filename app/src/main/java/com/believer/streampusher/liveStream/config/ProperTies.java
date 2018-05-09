package com.believer.streampusher.liveStream.config;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by believer on 2018/5/2.
 */

public class ProperTies {
    public static Properties getProperties() throws IOException {
        Properties urlProps;
        Properties props = new Properties();
        props.load(ProperTies.class.getResourceAsStream("/assets/appConfig.properties"));
//        InputStream is = c.getResources().openRawResource(R.raw.appConfig);
//            InputStream in = c.getAssets().open("appConfig.properties");
        urlProps = props;
        return urlProps;
    }
}

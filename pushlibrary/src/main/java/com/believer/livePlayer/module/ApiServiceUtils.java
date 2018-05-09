package com.believer.livePlayer.module;


import com.alibaba.fastjson.JSON;
import com.believer.livePlayer.bean.LiveBean;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiServiceUtils {

    public static List<LiveBean> getLiveList() {
        List<LiveBean> list = new ArrayList<>();
        OkHttpClient client = new OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://apikg.kktv1.com:8080")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService mApiServicePresenter = retrofit.create(ApiService.class);
        Call<String> call = mApiServicePresenter.live("{\"platform\":2,\"count\":2,\"start\":0,\"c\":90013,\"FuncTag\":80010001,\"a\":10}");
        Response<String> response = null;
        try {
            response = call.execute();
            String body = response.body();
            JSONObject js = new JSONObject(body);
            if (body != null) {
                List<LiveBean> temp = JSON.parseArray(js.getJSONArray("roomList").toString(), LiveBean.class);
                list.addAll(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

}

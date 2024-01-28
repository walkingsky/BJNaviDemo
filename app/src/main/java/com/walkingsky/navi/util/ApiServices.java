package com.walkingsky.navi.util;

import android.content.Context;

import com.amap.api.maps.model.LatLng;

import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.Callback;

/**
 * api接口调用
 */
public class ApiServices {
    private Context mContext;
    public static final String API_URL = "http://data.walkinginsky.com";

    public enum AgentKind  {all,inSixRing,outSixRing}

    public ApiServices(Context context) {
        mContext = context;
    }
    public static class Monitor{
        public final int index;
        public final String name;
        public final String aa;
        public final double Longitude;
        public final double Latitude;

        public Monitor(int index, String name, String aa, double longitude, double latitude) {
            this.index = index;
            this.name = name;
            this.aa = aa;
            Longitude = longitude;
            Latitude = latitude;
        }
    }

    public interface ApiMonitorList {
        //String data = "point1_Longitude="+startPoint.longitude+"&point1_Latitude="+startPoint.latitude
        //        +"&point2_Longitude="+endPoint.longitude+"&point2_Latitude="+endPoint.latitude;
        @GET("apis/data")
        Call<List<Monitor>> getMonitors(@Query("file_name") String kind,
                                        @Query("point1_Longitude") String point1_Longitude,@Query("point1_Latitude") String point1_Latitude,
                                        @Query("point2_Longitude") String point2_Longitude,@Query("point2_Latitude") String point2_Latitude);
    }


    /**
     * 获取起点和终点围成的长方形区域内 探头的经纬坐标点
     * @param  startPoint LatLonPoint 起点
     * @param  endPoint LatLonPoint 终点
     * @return List<LatLonPoint>
     */
    public static List<LatLng> getAvoidpolygons( AgentKind kind , LatLng startPoint, LatLng endPoint) throws IOException
    {
        // Create a very simple REST adapter which points the GitHub API.
        List<LatLng> latLonPointsList = new ArrayList<>();
        String kindStr ;
        Retrofit retrofit =
            new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

        switch (kind){
            case outSixRing:
                kindStr = "outsixring";
                break;
            case inSixRing:
                kindStr = "insixring";
                break;
            default:
                kindStr = "all";
        }

        //
        ApiMonitorList apiMonitorList = retrofit.create(ApiMonitorList.class);


        Call<List<Monitor>> call = apiMonitorList.getMonitors(
                kindStr,
                String.valueOf(startPoint.longitude),
                String.valueOf(startPoint.latitude),
                String.valueOf(endPoint.longitude),
                String.valueOf(endPoint.latitude));

        // Fetch and print a list of the contributors to the library.
        //同步请求
        /*
        try {
            List<Monitor> monitorLists = call.execute().body();
            if (monitorLists != null) {
                for (Monitor monitorList : monitorLists) {
                    LatLng latLng = new LatLng(monitorList.Longitude, monitorList.Latitude);
                    latLonPointsList.add(latLng);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return  latLonPointsList;

        */
        //异步请求
        //对 发送请求 进行封装
        call.enqueue(new Callback<List<Monitor>>() {
            //请求成功时回调
            @Override
            public void onResponse(Call<List<Monitor>> call, Response<List<Monitor>> response) {

                List<Monitor> monitorLists = response.body();
                if (monitorLists != null) {
                    for (Monitor monitorList : monitorLists) {
                        LatLng latLng = new LatLng(monitorList.Longitude, monitorList.Latitude);
                        latLonPointsList.add(latLng);
                    }
                }
            }
            //请求失败时候的回调
            @Override
            public void onFailure(Call<List<Monitor>> call, Throwable throwable) {
                System.out.println("连接失败");
            }
        });
        return latLonPointsList;
    }

}



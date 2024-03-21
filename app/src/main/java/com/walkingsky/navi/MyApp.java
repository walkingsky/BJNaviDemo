package com.walkingsky.navi;

import android.app.Application;

import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.route.DriveRouteResultV2;

import java.util.ArrayList;
import java.util.List;


public class MyApp extends Application {
    private static MyApp instance;

    //全局变量

    //规划线路结果
    private DriveRouteResultV2 mDriveRouteResultV2;

    public static MyApp getInstance(){
        return instance;
    }

    public void setmDriveRouteResultV2(DriveRouteResultV2 resultV2){
        mDriveRouteResultV2 = resultV2;
    }

    public DriveRouteResultV2 getmDriveRouteResultV2(){
        return mDriveRouteResultV2;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}

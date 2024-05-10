package com.walkingsky.navi.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;


import com.amap.api.maps.model.Marker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapException;
import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviIndependentRouteListener;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.enums.MapStyle;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapCarInfo;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPathGroup;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePathV2;
import com.amap.api.services.route.DriveRouteResultV2;
import com.amap.api.services.route.DriveStepV2;
import com.amap.api.navi.model.NaviPoi;
import com.walkingsky.navi.MyApp;
import com.walkingsky.navi.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RouteNaviActivity extends Activity implements AMapNaviListener, AMapNaviViewListener, View.OnClickListener {

    private AMapNaviView mAMapNaviView;
    private AMapNavi mAMapNavi;
    private int wayCnt = 0 ;
    private int currentStepId = 0; //记录当前位置所在线路的step id
    private int mStepId = 0; //记录已经到达过的原始线路（从避让规划线路传递过来的线路）的最大step id
    private boolean gps = false;
    private final MyApp myApp = MyApp.getInstance();
    //途经点 列表
    private final List<NaviLatLng> wayList = new ArrayList<>();
    private final List<LatLonPoint> throutPointList = new ArrayList<>();
    private List<NaviPoi> wayPoiList = new ArrayList<>();
    private Map<String,LatLng> selectedMonitorsMap = new HashMap<>();

    //躲避拥堵
    private boolean congestion;
    //避免高速
    private boolean avoidhightspeed;
    //避免收费
    private boolean cost;
    //高速优先
    private boolean hightspeed;
    private boolean useIndependentNavi = false;

    //起始点坐标
    private final List<NaviLatLng> startList = new ArrayList<>();
    //终点坐标
    private final List<NaviLatLng> endList = new ArrayList<>();
    //规划线路结果
    private DriveRouteResultV2 mDriveRouteResultV2;

    private AMap mAmap;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_basic_navi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#303F9F"));
        }
        mAMapNaviView =  findViewById(R.id.navi_view);
        AMapNaviViewOptions aMapNaviViewOptions = new AMapNaviViewOptions();
        aMapNaviViewOptions.setMapStyle(MapStyle.AUTO,"");
        //aMapNaviViewOptions.setSecondActionVisible(true);
        //设置开启动态比例尺
        aMapNaviViewOptions.setAutoChangeZoom(true);
        //设置6秒后自动锁车
        aMapNaviViewOptions.setAutoLockCar(true);
        //设置菜单按钮是否在导航界面显示。
        aMapNaviViewOptions.setSettingMenuEnabled(true);
        //通过路线是否自动置灰
        aMapNaviViewOptions.setAfterRouteAutoGray(true);
        //设置是否显示下下个路口的转向引导
        aMapNaviViewOptions.setSecondActionVisible(true);
        //aMapNaviViewOptions.setReCalculateRouteForYaw(false);//设置偏航时是否重新计算路径
        //aMapNaviViewOptions.setReCalculateRouteForTrafficJam(false);//前方拥堵时是否重新计算路径
        aMapNaviViewOptions.setLaneInfoShow(true);
        mAMapNaviView.setViewOptions(aMapNaviViewOptions);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);

        mAmap = mAMapNaviView.getMap();

        FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setImageResource(R.drawable.navigation_1_icon);
        floatingActionButton.setOnClickListener(this);

        mDriveRouteResultV2 = myApp.getmDriveRouteResultV2();
        //获取传递的变量
        wayCnt = getIntent().getIntExtra("wayCnt",0);
        gps = getIntent().getBooleanExtra("gps", false);
        cost = getIntent().getBooleanExtra("cost",false);
        congestion = getIntent().getBooleanExtra("congestion",false);
        avoidhightspeed = getIntent().getBooleanExtra("avoidhightspeed",false);
        hightspeed = getIntent().getBooleanExtra("hightspeed",false);
        double startLatitude = getIntent().getDoubleExtra("startLatitude",0);
        double startLongitude = getIntent().getDoubleExtra("startLongitude",0);
        double endLatitude = getIntent().getDoubleExtra("endLatitude",0);
        double endLongitude = getIntent().getDoubleExtra("endLongitude",0);
        if(startLatitude >0 && startLongitude >0){
            startList.add(new NaviLatLng(startLatitude,startLongitude));
        }
        if(endLatitude >0 && endLongitude >0){
            endList.add(new NaviLatLng(endLatitude,endLongitude));
        }

        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            mAMapNavi.addAMapNaviListener(this);
            mAMapNavi.setEmulatorNaviSpeed(240);
            mAMapNavi.setUseInnerVoice(true,true);
            //设置电子眼播报是否开启
            mAMapNavi.setCameraInfoUpdateEnabled(true);
            //设置导航播报时压低音乐
            mAMapNavi.setControlMusicVolumeMode(0);
            //设置在通话过程中是否进行导航播报
            mAMapNavi.setListenToVoiceDuringCall(false);

            calculateNavi(false);

        } catch (AMapException e) {
            e.printStackTrace();
        }

        mContext = this.getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();
        //        仅仅是停止你当前在说的这句话，一会到新的路口还是会再说的
        //
        //        停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
        //        mAMapNavi.stopNavi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAMapNaviView.onDestroy();
        if (mAMapNavi!=null){
            mAMapNavi.stopNavi();
            /*
             * 当前页面不销毁AmapNavi对象。
             * 因为可能会返回到RestRouteShowActivity页面再次进行路线选择，然后再次进来导航。
             * 如果销毁了就没办法在上一个页面进行选择路线了。
             * 但是AmapNavi对象始终销毁，那我们就需要在上一个页面用户回退时候销毁了。
             */
            mAMapNavi.removeAMapNaviListener(this);
        }
        wayList.clear();
        wayPoiList.clear();
        mAMapNavi.destroy();
    }

    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onStartNavi(int type) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    /**
     * 当位置信息有更新时的回调函数。
     * @param location 当前位置的定位信息。
     */
    @Override
    public void onLocationChange(AMapNaviLocation location) {

        if(location.isMatchNaviPath()){
            currentStepId = location.getCurStepIndex();
        }
    }

    @Override
    public void onGetNavigationText(int type, String text) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onEndEmulatorNavi() {
    }

    @Override
    public void onArriveDestination() {
    }

    @Override
    public void onCalculateRouteFailure(int errorInfo) {
    }

    @Override
    public void onReCalculateRouteForYaw() {
        //mAMapNavi.playTTS(getResources().getString(R.string.navi_step_yaw),true);
        //mAMapNavi.stopNavi();
        //calculateNavi(true);
        //mAMapNavi.playTTS(getResources().getString(R.string.navi_yaw_auto),true);
        //mStepId = currentStepId;
        //Log.e("DEBUG", "onReCalculateRouteForYaw: currentStepId:"+String.valueOf(currentStepId));
    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    /**
     * 判断经过最后一个途经点时，重新规划线路
     * @param wayID 途经点的id
     */
    @Override
    public void onArrivedWayPoint(int wayID) {
        /*
        //如果经过的途经点的id和计数不一致（计数少，id多：也即漏过了途经点）
        if(wayID!=wayCnt) {
            mAMapNavi.playTTS(getResources().getString(R.string.navi_step_jump), true);
            mAMapNavi.stopNavi();
            wayCnt = 0;
            mStepId = wayID;
            calculateNavi(true);
            return;
        }
        */
        wayCnt++;
        if(!(wayID +1 < getResources().getInteger(R.integer.navi_max_pass_count))) {
            mAMapNavi.playTTS(getResources().getString(R.string.navi_step_finish),true);
            mAMapNavi.stopNavi();
            calculateNavi(true);
        }
    }

    @Override
    public void onGpsOpenStatus(boolean enabled) {
    }

    @Override
    public void onNaviSetting() {
    }

    @Override
    public void onNaviMapMode(int isLock) {

    }

    @Override
    public void onNaviCancel() {
        finish();
    }

    @Override
    public void onNaviTurnClick() {

    }

    @Override
    public void onNextRoadClick() {

    }

    @Override
    public void onScanViewButtonClick() {
    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] amapServiceAreaInfos) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviinfo) {
    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {
    }

    @Override
    public void hideCross() {
    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] laneInfos, byte[] laneBackgroundInfo, byte[] laneRecommendedInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onLockMap(boolean isLock) {
    }

    @Override
    public void onNaviViewLoaded() {
    }

    @Override
    public void onMapTypeChanged(int i) {

    }

    @Override
    public void onNaviViewShowMode(int i) {

    }

    @Override
    public boolean onNaviBackClick() {
        return false;
    }


    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }

    /**
     * 规划导航路径
     */
    public void calculateNavi(boolean reCalculate){

        if (avoidhightspeed && hightspeed) {
            Toast.makeText(getApplicationContext(), "不走高速与高速优先不能同时为true.", Toast.LENGTH_LONG).show();
        }
        if (cost && hightspeed) {
            Toast.makeText(getApplicationContext(), "高速优先与避免收费不能同时为true.", Toast.LENGTH_LONG).show();
        }
        if (!(mDriveRouteResultV2 == null || mDriveRouteResultV2.getPaths().isEmpty())) {
            DrivePathV2 mDriverPath = mDriveRouteResultV2.getPaths().get(0);
            if(!reCalculate) {
                wayList.clear();
                wayPoiList.clear();
                throutPointList.clear();
                    int i = 0;
                    for (DriveStepV2 mDriveStep : mDriverPath.getSteps()) {
                        if (i == getResources().getInteger(R.integer.navi_max_pass_count))
                            break;
                        List<LatLonPoint> mStepLatLonPoints = mDriveStep.getPolyline();
                        int mSize = mStepLatLonPoints.size();
                        int a = (mSize-1)/2;
                        int b = (int)(mSize-1)%2;
                        if(b == 0){
                            LatLonPoint latLonPoint = mStepLatLonPoints.get(a);
                            NaviLatLng mNaviLatLng = new NaviLatLng(latLonPoint.getLatitude(),
                                    latLonPoint.getLongitude());
                            wayList.add(mNaviLatLng);
                            throutPointList.add(latLonPoint);
                            NaviPoi navPoi = new NaviPoi("",
                                    new LatLng(latLonPoint.getLatitude(),latLonPoint.getLongitude()),
                                    "");
                            wayPoiList.add(navPoi);
                        }else{
                            LatLonPoint latLonPointA = mStepLatLonPoints.get(a);
                            LatLonPoint latLonPointB = mStepLatLonPoints.get(a+b);
                            double mPointLat = latLonPointA.getLatitude() +
                                    (latLonPointB.getLatitude() - latLonPointA.getLatitude())/2;
                            double mPointLong = latLonPointA.getLongitude() +
                                    (latLonPointB.getLongitude() - latLonPointA.getLongitude())/2;
                            NaviLatLng mNaviLatLng = new NaviLatLng(mPointLat,mPointLong);
                            wayList.add(mNaviLatLng);
                            throutPointList.add( new LatLonPoint(mPointLat,mPointLong));
                            NaviPoi navPoi = new NaviPoi("",
                                    new LatLng(mPointLat,mPointLong),
                                    "");
                            wayPoiList.add(navPoi);
                        }
                        i++;
                    }
                currentStepId = 0;

            }else{
                //清除已经路过的 途经点
                if( currentStepId< mDriverPath.getSteps().size()  )
                {
                    wayList.clear();
                    wayPoiList.clear();
                    int i = 0,j = 0;
                    for (DriveStepV2 mDriveStep : mDriverPath.getSteps()) {
                        if( i <= mStepId ){ //跳过已经走过的路
                            i++;
                            continue;
                        }
                        if(j == getResources().getInteger(R.integer.navi_max_pass_count))
                            break;
                        List<LatLonPoint> mStepLatLonPoints = mDriveStep.getPolyline();
                        int mSize = mStepLatLonPoints.size();
                        int a = (mSize-1)/2;
                        int b = (int)(mSize-1)%2;
                        if(b == 0){
                            LatLonPoint latLonPoint = mStepLatLonPoints.get(a);
                            NaviLatLng mNaviLatLng = new NaviLatLng(latLonPoint.getLatitude(),
                                    latLonPoint.getLongitude());
                            wayList.add(mNaviLatLng);
                            throutPointList.add(latLonPoint);
                            NaviPoi navPoi = new NaviPoi("",
                                    new LatLng(latLonPoint.getLatitude(),latLonPoint.getLongitude()),
                                    "");
                            wayPoiList.add(navPoi);
                        }else {
                            LatLonPoint latLonPointA = mStepLatLonPoints.get(a);
                            LatLonPoint latLonPointB = mStepLatLonPoints.get(a + b);
                            double mPointLat = latLonPointA.getLatitude() +
                                    (latLonPointB.getLatitude() - latLonPointA.getLatitude()) / 2;
                            double mPointLong = latLonPointA.getLongitude() +
                                    (latLonPointB.getLongitude() - latLonPointA.getLongitude()) / 2;
                            NaviLatLng mNaviLatLng = new NaviLatLng(mPointLat, mPointLong);
                            wayList.add(mNaviLatLng);
                            throutPointList.add( new LatLonPoint(mPointLat,mPointLong));
                            NaviPoi navPoi = new NaviPoi("",
                                    new LatLng(mPointLat, mPointLong),
                                    "");
                            wayPoiList.add(navPoi);
                        }
                        i++;
                        j++;
                    }
                    currentStepId = 0;
                    wayCnt = 0;
                }else{
                    return; //途经点已经全部到达，不需要继续导航
                }

            }
        }

        /*
         * strategyFlag转换出来的值都对应PathPlanningStrategy常量，用户也可以直接传入PathPlanningStrategy常量进行算路。
         * 如:mAMapNavi.calculateDriveRoute(mStartList, mEndList, mWayPointList,PathPlanningStrategy.DRIVING_DEFAULT);
         */
        int strategyFlag = 0;
        try {
            strategyFlag = mAMapNavi.strategyConvert(congestion, avoidhightspeed, cost, hightspeed, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (strategyFlag >= 0) {
            String carNumber = getResources().getString(R.string.car_number);
            AMapCarInfo carInfo = new AMapCarInfo();
            //设置车牌
            carInfo.setCarNumber(carNumber);
            //设置车牌是否参与限行算路
            carInfo.setRestriction(true);

            if (mAMapNavi!=null && ! endList.isEmpty() && ! useIndependentNavi){
                mAMapNavi.setCarInfo(carInfo);
                //mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
                mAMapNavi.calculateDriveRoute( endList, wayList, strategyFlag);
            }else if(mAMapNavi!=null &&  useIndependentNavi){
                mAMapNavi.setCarInfo(carInfo);
                NaviPoi startNaviPoi = new NaviPoi("起始点",new LatLng(startList.get(0).getLatitude(),startList.get(0).getLongitude()),"");
                NaviPoi endNaviPoi = new NaviPoi("终点",new LatLng(endList.get(0).getLatitude(),endList.get(0).getLongitude()),"");
                mAMapNavi.independentCalculateRoute(startNaviPoi,endNaviPoi,wayPoiList,strategyFlag,1,aMapNaviIndependentRouteListener);
            }
        }

    }

    /**
     * 规划线路返回结果 成功
     * @param aMapCalcRouteResult AMapCalcRouteResult
     */
    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {
        final DrivePathV2 drivePathV2 = mDriveRouteResultV2.getPaths().get(0);
        DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                mContext, mAmap, drivePathV2,
                mDriveRouteResultV2.getStartPos(),
                mDriveRouteResultV2.getTargetPos(), selectedMonitorsMap,throutPointList);
        drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
        drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
        drivingRouteOverlay.removeFromMap();
        drivingRouteOverlay.addToMap();

        drivingRouteOverlay.zoomToSpan();
        drivingRouteOverlay.cancelableCallback.onFinish(); //通过事件，触发重新绘制标记点

        mAmap.setOnMarkerClickListener(throughPointMarkerClick);

        if (gps) {
            mAMapNavi.startNavi(NaviType.GPS);
        } else {
            mAMapNavi.startNavi(NaviType.EMULATOR);
        }
    }
    /**
     * 规划线路返回结果 失败
     * @param result AMapCalcRouteResult
     */
    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult result) {
        //calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + result.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 独立路径规划结果回调
     */
    private AMapNaviIndependentRouteListener aMapNaviIndependentRouteListener = new AMapNaviIndependentRouteListener(){
        @Override
        public void onIndependentCalculateSuccess(AMapNaviPathGroup aMapNaviPathGroup) {
            aMapNaviPathGroup.selectRouteWithIndex(aMapNaviPathGroup.getPathCount()-1);
            if (gps) {
                mAMapNavi.startNaviWithPath(NaviType.GPS,aMapNaviPathGroup);
            } else {
                mAMapNavi.stopNavi();
                boolean a = mAMapNavi.startNaviWithPath(NaviType.EMULATOR,aMapNaviPathGroup);
            }
        }

        @Override
        public void onIndependentCalculateFail(AMapCalcRouteResult aMapCalcRouteResult) {

        }

    };

    /**
     * 点击按钮，切换独立导航
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.floatingActionButton){
            FloatingActionButton floatingActionButton = findViewById(id);
            if(useIndependentNavi){
                floatingActionButton.setImageResource(R.drawable.navigation_1_icon);
                mAMapNavi.playTTS(getResources().getString(R.string.navi_not_use_independent),true);
                useIndependentNavi = false;
            }else{
                mAMapNavi.playTTS(getResources().getString(R.string.navi_use_independent) ,true);
                floatingActionButton.setImageResource(R.drawable.navigation_2_icon);
                useIndependentNavi = true;
            }
            mAMapNavi.stopNavi();
            calculateNavi(false);
        }
    }

    /**
     * 地图添加 途经点 marker 的点击事件
     */
    AMap.OnMarkerClickListener throughPointMarkerClick = new AMap.OnMarkerClickListener(){
        @Override
        public boolean onMarkerClick(Marker marker) {
            String title = marker.getTitle();
            if(title.indexOf("throughMarker_") >= 0){
                int id = Integer.parseInt(title.substring(14));
                if(id >=0 && id <= mDriveRouteResultV2.getPaths().get(0).getSteps().size()){
                    mStepId = id;
                    mAMapNavi.playTTS(getResources().getString(R.string.navi_step_jump), true);
                    mAMapNavi.stopNavi();
                    calculateNavi(true);
                }
            }
            return false;
        }
    };
}

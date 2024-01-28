package com.walkingsky.navi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.amap.api.maps.AMapException;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.TTSPlayListener;
import com.amap.api.navi.enums.MapStyle;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.services.route.DriveStepV2;
import com.walkingsky.navi.R;

//import java.io.Serializable;
import java.util.ArrayList;
//import java.util.List;


public class RouteNaviActivity extends Activity implements AMapNaviListener, AMapNaviViewListener {

    AMapNaviView mAMapNaviView;
    AMapNavi mAMapNavi;

    int wayCnt ;

    ArrayList<DriveStepV2> mDriveStep = null;
    boolean gps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_basic_navi);

        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
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
        //
        aMapNaviViewOptions.setLaneInfoShow(true);
        mAMapNaviView.setViewOptions(aMapNaviViewOptions);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);

        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            mAMapNavi.addAMapNaviListener(this);
            mAMapNavi.setUseInnerVoice(true);
            mAMapNavi.setEmulatorNaviSpeed(120);
            wayCnt = getIntent().getIntExtra("wayCnt",0);
            //监听播报语音
            mAMapNavi.addTTSPlayListener(new TTSPlayListener() {
                @Override
                public void onPlayStart(String s) {

                    if(s.contains("到达途经点")) {
                        Log.d("debug",s);
                        mAMapNavi.setTtsPlaying(true);
                        //mAMapNavi.startSpeak();
                    }
                }

                @Override
                public void onPlayEnd(String s) {
                    if(s.contains("到达途经点")) {
                        Log.d("debug",s);
                        mAMapNavi.setTtsPlaying(false);
                    }
                }
            });
            //mDriveStep = getIntent().getParcelableArrayListExtra("mDriveStep");
            gps = getIntent().getBooleanExtra("gps", false);
            if (gps) {
                mAMapNavi.startNavi(NaviType.GPS);
            } else {
                mAMapNavi.startNavi(NaviType.EMULATOR);
            }
        } catch (AMapException e) {
            e.printStackTrace();
        }



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
            /**
             * 当前页面不销毁AmapNavi对象。
             * 因为可能会返回到RestRouteShowActivity页面再次进行路线选择，然后再次进来导航。
             * 如果销毁了就没办法在上一个页面进行选择路线了。
             * 但是AmapNavi对象始终销毁，那我们就需要在上一个页面用户回退时候销毁了。
             */
            mAMapNavi.removeAMapNaviListener(this);
        }

//		mAMapNavi.destroy();
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

    @Override
    public void onLocationChange(AMapNaviLocation location) {

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

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    /**
     * 判断经过最后一个途经点时，重新规划线路
     * @param wayID
     */
    @Override
    public void onArrivedWayPoint(int wayID) {

        if(wayID +1 < getResources().getInteger(R.integer.navi_max_pass_count))
            return;
        else { //if(wayID+1 == getResources().getInteger(R.integer.navi_max_pass_count)) {
            int code = 0;
            if(gps)
                code = 300;
            else
                code = 400;
            mAMapNavi.playTTS(getResources().getString(R.string.navi_step_finish),true);
            //mAMapNavi.stopNavi();
            Intent intent = new Intent(this, MyDriverListActivity.class);
            intent.putExtra("wayId", wayID);
            intent.putExtra("gps", gps);
            setResult(code, intent);
            finish();
            this.onDestroy();
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
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }
}

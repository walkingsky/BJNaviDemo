package com.walkingsky.navi.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapException;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapCarInfo;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapRestrictionInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.PoiInputItemWidget;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResultV2;
import com.amap.api.services.route.DrivePathV2;
import com.amap.api.services.route.DriveRouteResultV2;
import com.amap.api.services.route.DriveStepV2;
import com.amap.api.services.route.RideRouteResultV2;
import com.amap.api.services.route.RouteSearchV2;
import com.amap.api.services.route.WalkRouteResultV2;
import com.walkingsky.navi.R;
import com.walkingsky.navi.activity.search.SearchPoiActivity;
import com.walkingsky.navi.util.AMapUtil;


import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyDriverListActivity extends Activity implements AMapNaviListener, OnClickListener, OnCheckedChangeListener, RouteSearchV2.OnRouteSearchListener {
    private boolean congestion, cost, hightspeed, avoidhightspeed;
    /**
     * 导航对象(单例)
     */
    private AMapNavi mAMapNavi;
    private AMap mAmap;
    /**
     * 地图对象
     */
    private MapView mRouteMapView;
    private Marker mStartMarker;
    private Marker mEndMarker;
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    //private NaviLatLng startLatlng = new NaviLatLng(40.058741, 116.369051);
    //private NaviLatLng  endLatlng = new NaviLatLng(40.170835, 116.328945);
    private LatLonPoint mStartPoint = null;
    //private LatLonPoint mStartPoint = new LatLonPoint(40.058741, 116.369051);
    private LatLonPoint mEndPoint = null;
    //private LatLonPoint mEndPoint = new LatLonPoint(40.170835, 116.328945);

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private Context mContext;

    private final List<NaviLatLng> startList = new ArrayList<NaviLatLng>();
    private RouteSearchV2 mRouteSearch;
    private DriveRouteResultV2 mDriveRouteResultV2;
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();

    /**
     * 当前用户选中的路线，在下个页面进行导航
     */
    private int routeIndex;
    /**
     * 路线的权值，重合路线情况下，权值高的路线会覆盖权值低的路线
     **/
    private int zindex = 1;
    // 躲避点 经纬度 map
    private Map<String,LatLng> selectedMonitorsMap = new HashMap<String,LatLng>();
    /**
     * 路线计算成功标志位
     */
    private boolean calculateSuccess = false;
    private boolean chooseRouteSuccess = false;

    private ProgressDialog progDialog = null;// 搜索时进度条

    private boolean isFirstLocation = true; //定位第一次触发

    private int wayId = 0;

    private  boolean gps = false;
    private  boolean reCalculate = false;
    private  int reCalculateCnt = 1;
    private  double distanceRange = 0.000015;
    //点击地图时保存点击点的临时变量
    private LatLng tempLatLng;
    //定位点是否自动移动视角，旋转地图
    private boolean locationTypeIsMapRotate = false;


    // 全局保存搜索线路路径
    //private ArrayList<DriveStepV2> mDriveStep = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //privacyCompliance();
        privacyComplianceMy();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_rest_calculate_my);
        CheckBox congestion = (CheckBox) findViewById(R.id.congestion);
        //定位跟随 checkbox
        CheckBox locationnavi = (CheckBox) findViewById(R.id.locationnavi);
        CheckBox cost = (CheckBox) findViewById(R.id.cost);
        CheckBox hightspeed = (CheckBox) findViewById(R.id.hightspeed);
        CheckBox avoidhightspeed = (CheckBox) findViewById(R.id.avoidhightspeed);
        Button calculate = (Button) findViewById(R.id.calculate);
        Button startPoint = (Button) findViewById(R.id.startpoint);
        Button endPoint = (Button) findViewById(R.id.endpoint);
        Button gpsnavi = (Button) findViewById(R.id.gpsnavi);
        Button emulatornavi = (Button) findViewById(R.id.emulatornavi);
        Button search = (Button) findViewById(R.id.search);
        Button location = (Button) findViewById(R.id.location);
        Spinner spinner = (Spinner) findViewById(R.id.distance_range);
        calculate.setOnClickListener(this);
        startPoint.setOnClickListener(this);
        endPoint.setOnClickListener(this);
        gpsnavi.setOnClickListener(this);
        emulatornavi.setOnClickListener(this);
        congestion.setOnCheckedChangeListener(this);
        //定位跟随 checkbox
        locationnavi.setOnCheckedChangeListener(this);
        cost.setOnCheckedChangeListener(this);
        hightspeed.setOnCheckedChangeListener(this);
        avoidhightspeed.setOnCheckedChangeListener(this);
        search.setOnClickListener(this);
        location.setOnClickListener(this);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = parent.getItemAtPosition(position).toString();
                distanceRange = Double.valueOf(s);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner.setSelection(1);

        //初始化定位
        initLocation();

        //测试用
        //endList.add(endLatlng);

        mRouteMapView = (MapView) findViewById(R.id.navi_view);
        mRouteMapView.onCreate(savedInstanceState);
        mAmap = mRouteMapView.getMap();
        // 初始化Marker添加到地图
        mStartMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource( R.drawable.start)));
        mEndMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource( R.drawable.end)));
        mAmap.addOnMapLongClickListener(mapLongClickListener);

        setMyLocationStyle();

        // 设置定位监听
        //mAmap.setLocationSource(this);
        //设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        mAmap.setMyLocationEnabled(true);
        // 设置地图模式，aMap是地图控制器对象。1.MAP_TYPE_NAVI:导航地图 2.MAP_TYPE_NIGHT:夜景地图 3.MAP_TYPE_NORMAL:白昼地图（即普通地图） 4.MAP_TYPE_SATELLITE:卫星图

        mAmap.setMapType(AMap.MAP_TYPE_NORMAL);//设置默认定位按钮是否显示，非必需设置。

        mAmap.getUiSettings().setMyLocationButtonEnabled(true);//控制比例尺控件是否显示，非必须设置。
        mAmap.getUiSettings().setScaleControlsEnabled(true);        //显示比例尺
        mAmap.getUiSettings().setZoomControlsEnabled(true);       //显示缩放按钮
        mAmap.getUiSettings().setCompassEnabled(true);            //显示指南针

        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            mAMapNavi.addAMapNaviListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }

        try {
            mRouteSearch = new RouteSearchV2(this);
            mRouteSearch.setRouteSearchListener(this);
        } catch (com.amap.api.services.core.AMapException e) {
            throw new RuntimeException(e);
        }

        mContext = this.getApplicationContext();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mRouteMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mRouteMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRouteMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        startList.clear();
        wayList.clear();
        endList.clear();
        routeOverlays.clear();
        mRouteMapView.onDestroy();
        /**
         * 当前页面只是展示地图，activity销毁后不需要再回调导航的状态
         */
        if (mAMapNavi!=null){
            mAMapNavi.removeAMapNaviListener(this);
            mAMapNavi.destroy();
        }

        destroyLocation();

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id) {
            case R.id.congestion:
                congestion = isChecked;
                break;
            case R.id.avoidhightspeed:
                avoidhightspeed = isChecked;
                break;
            case R.id.cost:
                cost = isChecked;
                break;
            case R.id.hightspeed:
                hightspeed = isChecked;
                break;
            case R.id.locationnavi:
                locationTypeIsMapRotate = isChecked;
                setMyLocationStyle();
            default:
                break;
        }
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
    }

    @Override
    public void onCalculateRouteFailure(int arg0) {

    }

    private void drawRoutes(int routeId, AMapNaviPath path) {
        calculateSuccess = true;
        mAmap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(mAmap, path, this);
        routeOverLay.setTrafficLine(false);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);
    }

    public void changeRoute() {
        if (!calculateSuccess) {
            Toast.makeText(this, "请先算路", Toast.LENGTH_SHORT).show();
            return;
        }
        /**
         * 计算出来的路径只有一条
         */
        if (routeOverlays.size() == 1) {
            chooseRouteSuccess = true;
            //必须告诉AMapNavi 你最后选择的哪条路
            mAMapNavi.selectRouteId(routeOverlays.keyAt(0));
            Toast.makeText(this, "导航距离:" + (mAMapNavi.getNaviPath()).getAllLength() + "m" + "\n" + "导航时间:" + (mAMapNavi.getNaviPath()).getAllTime() + "s", Toast.LENGTH_SHORT).show();
            return;
        }

        if (routeIndex >= routeOverlays.size()) {
            routeIndex = 0;
        }
        int routeID = routeOverlays.keyAt(routeIndex);
        //突出选择的那条路
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            routeOverlays.get(key).setTransparency(0.4f);
        }
        RouteOverLay routeOverlay = routeOverlays.get(routeID);
        if(routeOverlay != null){
            routeOverlay.setTransparency(1);
            /**把用户选择的那条路的权值弄高，使路线高亮显示的同时，重合路段不会变的透明**/
            routeOverlay.setZindex(zindex++);
        }
        //必须告诉AMapNavi 你最后选择的哪条路
        mAMapNavi.selectRouteId(routeID);
        Toast.makeText(this, "路线标签:" + mAMapNavi.getNaviPath().getLabels(), Toast.LENGTH_SHORT).show();
        routeIndex++;
        chooseRouteSuccess = true;

        /**选完路径后判断路线是否是限行路线**/
        AMapRestrictionInfo info = mAMapNavi.getNaviPath().getRestrictionInfo();
        if(info != null){
            if (!TextUtils.isEmpty(info.getRestrictionTitle())  ) {
                Toast.makeText(this, info.getRestrictionTitle(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 清除当前地图上算好的路线
     */
    private void clearRoute() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            RouteOverLay routeOverlay = routeOverlays.valueAt(i);
            routeOverlay.removeFromMap();
        }
        routeOverlays.clear();
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.location: //定位当前位置
                startLocation();
                Button location = v.findViewById(R.id.location);
                location.setEnabled(false);
                break;
            case R.id.calculate:
                calculateNavi(false);
                break;
            case R.id.startpoint:
                Intent sintent = new Intent(MyDriverListActivity.this, SearchPoiActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("pointType", PoiInputItemWidget.TYPE_START);
                sintent.putExtras(bundle);
                startActivityForResult(sintent, 100);
                break;
            case R.id.endpoint:
                Intent eintent = new Intent(MyDriverListActivity.this, SearchPoiActivity.class);
                Bundle ebundle = new Bundle();
                ebundle.putInt("pointType", PoiInputItemWidget.TYPE_DEST);
                eintent.putExtras(ebundle);
                startActivityForResult(eintent, 200);
                break;
            case R.id.gpsnavi:

                calculateNavi(false);
                Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
                gpsintent.putExtra("gps", true);
                gpsintent.putExtra("wayCnt", wayList.size());
                startActivityForResult(gpsintent, 300);
                gps = true;
                reCalculate = false;
                break;
            case R.id.emulatornavi:
                Intent intent = new Intent(getApplicationContext(), RouteNaviActivity.class);
                intent.putExtra("gps", false);
                intent.putExtra("wayCnt", wayList.size());
                startActivityForResult(intent, 400);
                reCalculate = false;
                gps = false;
                break;
            case R.id.search:
                clearRoute();
                if (avoidhightspeed && hightspeed) {
                    Toast.makeText(getApplicationContext(), "不走高速与高速优先不能同时为true.", Toast.LENGTH_LONG).show();
                    break;
                }
                if (cost && hightspeed) {
                    Toast.makeText(getApplicationContext(), "高速优先与避免收费不能同时为true.", Toast.LENGTH_LONG).show();
                    break;
                }
                if (mStartPoint == null) {
                    Toast.makeText(getApplicationContext(), "起点未设置",Toast.LENGTH_LONG).show();
                    break;
                }
                if (mEndPoint == null) {
                    Toast.makeText(getApplicationContext(), "终点未设置",Toast.LENGTH_LONG).show();
                    break;
                }
                showProgressDialog();
                final RouteSearchV2.FromAndTo fromAndTo = new RouteSearchV2.FromAndTo(mStartPoint, mEndPoint);


                if(selectedMonitorsMap.isEmpty()) {

                    RouteSearchV2.DriveRouteQuery query = new RouteSearchV2.DriveRouteQuery(fromAndTo, RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_AVOID_HIGHWAY, null,
                            null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
                    //query.setShowFields(RouteSearchV2.ShowFields.COST|RouteSearchV2.ShowFields.NAVI|RouteSearchV2.ShowFields.POLINE|RouteSearchV2.ShowFields.TMCS);
                    query.setShowFields(RouteSearchV2.ShowFields.COST | RouteSearchV2.ShowFields.POLINE | RouteSearchV2.ShowFields.TMCS);
                    mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
                }else{
                    //从 selectedMonitorsMap 转换成 避让区域
                    java.util.List<java.util.List<LatLonPoint>> avoidpolygons = new ArrayList<>();

                    for (LatLng value:selectedMonitorsMap.values()) {

                        LatLng latLng1 = new LatLng(value.latitude-distanceRange,value.longitude-distanceRange);
                        LatLng latLng2 = new LatLng(value.latitude-distanceRange,value.longitude+distanceRange);
                        LatLng latLng3 = new LatLng(value.latitude+distanceRange,value.longitude+distanceRange);
                        LatLng latLng4 = new LatLng(value.latitude+distanceRange,value.longitude-distanceRange);

                        java.util.List<LatLonPoint> avoidpolygonsPoint = new ArrayList<>();

                        avoidpolygonsPoint.add(AMapUtil.convertToLatLonPoint(latLng1) );
                        avoidpolygonsPoint.add(AMapUtil.convertToLatLonPoint(latLng2) );
                        avoidpolygonsPoint.add(AMapUtil.convertToLatLonPoint(latLng3) );
                        avoidpolygonsPoint.add(AMapUtil.convertToLatLonPoint(latLng4) );

                        avoidpolygons.add(avoidpolygonsPoint );

                    }
                    //不清理
                    //selectedMonitorsMap.clear();
                    RouteSearchV2.DriveRouteQuery query = new RouteSearchV2.DriveRouteQuery(fromAndTo, RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_AVOID_HIGHWAY, null,
                            avoidpolygons, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
                    //query.setShowFields(RouteSearchV2.ShowFields.COST|RouteSearchV2.ShowFields.NAVI|RouteSearchV2.ShowFields.POLINE|RouteSearchV2.ShowFields.TMCS);
                    query.setShowFields(RouteSearchV2.ShowFields.COST | RouteSearchV2.ShowFields.POLINE | RouteSearchV2.ShowFields.TMCS);
                    mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getParcelableExtra("poi") != null) {
            clearRoute();
            Poi poi = data.getParcelableExtra("poi");

            if (requestCode == 100) {//起点选择完成
                //Toast.makeText(this, "100", Toast.LENGTH_SHORT).show();
                NaviLatLng startLatlng = new NaviLatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                mStartMarker.setPosition(new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude));
                mStartPoint = new LatLonPoint(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                startList.clear();
                startList.add(startLatlng);
            }

            if (requestCode == 200) {//终点选择完成
                //Toast.makeText(this, "200", Toast.LENGTH_SHORT).show();
                NaviLatLng endLatlng = new NaviLatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                mEndMarker.setPosition(new LatLng(poi.getCoordinate().latitude, poi.getCoordinate().longitude));
                mEndPoint = new LatLonPoint(poi.getCoordinate().latitude, poi.getCoordinate().longitude);
                endList.clear();
                endList.add(endLatlng);
                //清楚已经选择的躲避点 map
                selectedMonitorsMap.clear();

            }
        } else if (data != null && data.getIntExtra("wayId",0) != 0) { // 监听导航，模拟导航activity的返回

            wayId = data.getIntExtra("wayId",0);
            gps = data.getBooleanExtra("gps",true);
            wayId = wayId*reCalculateCnt;
            DrivePathV2 mDriverPath =  mDriveRouteResultV2.getPaths().get(0);

            if( wayId< mDriverPath.getSteps().size())
            {
                wayList.clear();
                int i = 0,j = 0;
                for (DriveStepV2 mDriveStep : mDriverPath.getSteps()) {
                    if(i<wayId){ //跳过已经走过的路
                        i++;
                        continue;
                    }
                    if(i == wayId){ //设置为新的起点

                    }
                    if(j == getResources().getInteger(R.integer.navi_max_pass_count))
                        break;
                    List<LatLonPoint> mStepLatLonPoints = mDriveStep.getPolyline();
                    LatLonPoint latLonPoint = mStepLatLonPoints.get(0);
                    NaviLatLng mNaviLatLng = new NaviLatLng(latLonPoint.getLatitude(),
                            mStepLatLonPoints.get(0).getLongitude());
                    wayList.add(mNaviLatLng);
                    i++;
                    j++;
                }

                calculateNavi(true);
                reCalculate = true;
                reCalculateCnt ++;

            }else{
                return; //途经点已经全部到达，不需要继续导航
            }
        }
    }

    /**
     * ************************************************** 在算路页面，以下接口全不需要处理，在以后的版本中我们会进行优化***********************************************************************************************
     **/

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo arg0) {


    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] arg0) {


    }

    @Override
    public void hideCross() {


    }

    @Override
    public void hideLaneInfo() {


    }

    @Override
    public void notifyParallelRoad(int arg0) {


    }

    @Override
    public void onArriveDestination() {


    }

    @Override
    public void onArrivedWayPoint(int arg0) {


    }

    @Override
    public void onEndEmulatorNavi() {


    }

    @Override
    public void onGetNavigationText(int arg0, String arg1) {


    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onGpsOpenStatus(boolean arg0) {


    }

    @Override
    public void onInitNaviFailure() {


    }

    @Override
    public void onLocationChange(AMapNaviLocation arg0) {

        //Log.d("debug","---onLocationChange");

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo arg0) {


    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] amapServiceAreaInfos) {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {


    }

    @Override
    public void onReCalculateRouteForYaw() {


    }

    @Override
    public void onStartNavi(int arg0) {


    }

    @Override
    public void onTrafficStatusUpdate() {


    }

    @Override
    public void showCross(AMapNaviCross arg0) {


    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] arg0, byte[] arg1, byte[] arg2) {


    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo arg0) {


    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat arg0) {


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
        //清空上次计算的路径列表。
        routeOverlays.clear();

        int[] routeIds = aMapCalcRouteResult.getRouteid();
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < routeIds.length; i++) {
            AMapNaviPath path = paths.get(routeIds[i]);
            if (path != null) {
                drawRoutes(routeIds[i], path);
            }
        }

        if(reCalculate) {
            Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
            gpsintent.putExtra("wayCnt", wayList.size());
            if (gps) {
                gpsintent.putExtra("gps", true);
                startActivityForResult(gpsintent, 300);
            } else {
                gpsintent.putExtra("gps", false);
                startActivityForResult(gpsintent, 400);
            }
        }
    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult result) {
        calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + result.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }

    @Override
    public void onGpsSignalWeak(boolean b) {

    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索...");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 地图长按点击菜单
     */
    AMap.OnMapLongClickListener mapLongClickListener = new AMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            //保存 latlng 到全局变量
            tempLatLng = latLng;
            Button emulatornavi = (Button) findViewById(R.id.emulatornavi);

            PopupMenu popupMenu = new PopupMenu(mContext,emulatornavi);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()){
                        case R.id.set_start_point:
                            NaviLatLng startLatlng = new NaviLatLng(tempLatLng.latitude, tempLatLng.longitude);
                            mStartMarker.setPosition(new LatLng(tempLatLng.latitude, tempLatLng.longitude));
                            mStartPoint = new LatLonPoint(tempLatLng.latitude, tempLatLng.longitude);
                            startList.clear();
                            startList.add(startLatlng);

                            break;
                        case R.id.set_end_point:
                            NaviLatLng endLatlng = new NaviLatLng(tempLatLng.latitude, tempLatLng.longitude);
                            mEndMarker.setPosition(new LatLng(tempLatLng.latitude, tempLatLng.longitude));
                            mEndPoint = new LatLonPoint(tempLatLng.latitude, tempLatLng.longitude);
                            endList.clear();
                            endList.add(endLatlng);
                            break;
                    }
                    return true;
                }
            });
            popupMenu.show();
        }

    };


    /**
     * 标记点 点击事件
     */
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener(){
        @Override
        public boolean onMarkerClick(Marker marker) {
            String markerId = marker.getId();
            if( selectedMonitorsMap.containsKey(markerId)) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(
                                mContext.getResources(), R.drawable.amap_monitor_point)));
                selectedMonitorsMap.remove(markerId);
            }else{

                marker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(
                                mContext.getResources(), R.drawable.amap_monitor_selected)));

                LatLng markerPosition = marker.getOptions().getPosition();
                selectedMonitorsMap.put(markerId,markerPosition);
                /*
                marker.setMarkerOptions(new MarkerOptions().icon(
                        BitmapDescriptorFactory.fromBitmap(
                                BitmapFactory.decodeResource(
                                        mContext.getResources(), R.drawable.amap_monitor_selected)
                        )
                ));
                 */

                //selectedMonitorsMap.put(markerId,marker.getPosition());

            }

            if(! selectedMonitorsMap.isEmpty()){
                //变换按钮
            }

            return true;
        }
    };

    @Override
    public void onDriveRouteSearched(DriveRouteResultV2 driveRouteResultV2, int errorCode) {
        //mAmap.removeOnMarkerClickListener(markerClickListener);
        mAmap.clear();// 清理地图上的所有覆盖物
        mAmap.setOnMarkerClickListener(markerClickListener);
        //selectedMonitorsMap.clear();
        if (errorCode == 1000) {
            if (driveRouteResultV2 != null && driveRouteResultV2.getPaths() != null) {
                if (driveRouteResultV2.getPaths().size() > 0) {
                    mDriveRouteResultV2 = driveRouteResultV2;
                    final DrivePathV2 drivePathV2 = mDriveRouteResultV2.getPaths().get(0);
                    //存储路径steps到序列化的arrylist
                    /*
                    int i = 0;
                    mDriveStep.clear();
                    for (DriveStepV2 driveStep : drivePathV2.getSteps()) {
                        //mDriveStep.add(driveStep);
                        mDriveStep.add(i,driveStep);
                        i++;
                    }
                    */

                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            mContext, mAmap, drivePathV2,
                            mDriveRouteResultV2.getStartPos(),
                            mDriveRouteResultV2.getTargetPos(), selectedMonitorsMap,null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    if(selectedMonitorsMap.isEmpty())  //更新地图范围
                        drivingRouteOverlay.zoomToSpan();
                    else
                        drivingRouteOverlay.cancelableCallback.onFinish(); //通过事件，触发重新绘制标记点
                    int dis = (int) drivePathV2.getDistance();
                    int dur = (int) drivePathV2.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";

                } else if (driveRouteResultV2 != null && driveRouteResultV2.getPaths() == null) {
                    Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据！" , Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据！" , Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "搜索错误："+errorCode , Toast.LENGTH_LONG).show();
        }

        dissmissProgressDialog();

    }

    /**
     * 计算导航路径
     */
    public void calculateNavi(boolean reCalculate){
        clearRoute();

        if (avoidhightspeed && hightspeed) {
            Toast.makeText(getApplicationContext(), "不走高速与高速优先不能同时为true.", Toast.LENGTH_LONG).show();
        }
        if (cost && hightspeed) {
            Toast.makeText(getApplicationContext(), "高速优先与避免收费不能同时为true.", Toast.LENGTH_LONG).show();
        }
        if(!reCalculate) {
            wayList.clear();
            if (!(mDriveRouteResultV2 == null || mDriveRouteResultV2.getPaths().isEmpty())) {
                DrivePathV2 mDriverPath = mDriveRouteResultV2.getPaths().get(0);
                int i = 0;
                for (DriveStepV2 mDriveStep : mDriverPath.getSteps()) {
                    if (i == getResources().getInteger(R.integer.navi_max_pass_count))
                        break;
                    List<LatLonPoint> mStepLatLonPoints = mDriveStep.getPolyline();
                    LatLonPoint latLonPoint = mStepLatLonPoints.get(0);
                    NaviLatLng mNaviLatLng = new NaviLatLng(latLonPoint.getLatitude(),
                            mStepLatLonPoints.get(0).getLongitude());
                    wayList.add(mNaviLatLng);
                    i++;
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
            if (mAMapNavi!=null){
                mAMapNavi.setCarInfo(carInfo);
                //mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
                mAMapNavi.calculateDriveRoute( endList, wayList, strategyFlag);
            }
        }

    }

    @Override
    public void onBusRouteSearched(BusRouteResultV2 busRouteResultV2, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResultV2 walkRouteResultV2, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResultV2 rideRouteResultV2, int i) {

    }


    /**
     * 初始化定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void initLocation(){
        //初始化client
        try {
            locationClient = new AMapLocationClient(this.getApplicationContext());
            locationOption = getDefaultOption();

            //设置定位参数
            locationClient.setLocationOption(locationOption);
            // 设置定位监听
            locationClient.setLocationListener(locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 默认的定位参数
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private AMapLocationClientOption getDefaultOption(){
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        mOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.DEFAULT);//可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
        return mOption;
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            Button locationButton = (Button) findViewById(R.id.location);
            locationButton.setEnabled(true);
            if (null != location) {
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if(location.getErrorCode() == 0){

                    mStartPoint = new LatLonPoint(location.getLatitude(),location.getLongitude());

                    //Toast.makeText(getApplicationContext(), "定位成功："+location.getAddress(), Toast.LENGTH_LONG).show();
                    stopLocation();
                    NaviLatLng startLatlng = new NaviLatLng(location.getLatitude(), location.getLongitude());
                    mStartMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));;
                    startList.clear();
                    startList.add(startLatlng);
                    if (isFirstLocation) {
                        mAmap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                        //mListener.onLocationChanged(location);// 显示系统小蓝点
                        isFirstLocation = false;
                    }
                } else {
                    //定位失败
                    //Toast.makeText(getApplicationContext(), "定位失败："+location.getErrorInfo(), Toast.LENGTH_LONG).show();

                    stopLocation();
                    Log.e("TAG", "定位失败!!!");
                }

            } else {
                //Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
                stopLocation();
                Log.e("TAG", "定位失败!!!");
            }
        }
    };

    /**
     * 开始定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void startLocation(){
        try {
            //根据控件的选择，重新设置定位参数
            if(locationClient.isStarted()){
                locationClient.stopLocation();
            }
            // 设置定位参数
            locationClient.setLocationOption(locationOption);
            // 启动定位
            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 停止定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void stopLocation(){
        try {
            // 停止定位
            locationClient.stopLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 销毁定位
     *
     * @since 2.8.0
     * @author hongming.wang
     *
     */
    private void destroyLocation() {
        if (null != locationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    private void privacyComplianceMy(){
        MapsInitializer.updatePrivacyShow(MyDriverListActivity.this,true,true);
        MapsInitializer.updatePrivacyAgree(MyDriverListActivity.this,true);
    }
    private void privacyCompliance() {
        MapsInitializer.updatePrivacyShow(MyDriverListActivity.this,true,true);
        SpannableStringBuilder spannable = new SpannableStringBuilder("\"亲，感谢您对XXX一直以来的信任！我们依据最新的监管要求更新了XXX《隐私权政策》，特向您说明如下\n1.为向您提供交易相关基本功能，我们会收集、使用必要的信息；\n2.基于您的明示授权，我们可能会获取您的位置（为您提供附近的商品、店铺及优惠资讯等）等信息，您有权拒绝或取消授权；\n3.我们会采取业界先进的安全措施保护您的信息安全；\n4.未经您同意，我们不会从第三方处获取、共享或向提供您的信息；\n");
        spannable.setSpan(new ForegroundColorSpan(Color.BLUE), 35, 42, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        new AlertDialog.Builder(this)
                .setTitle("温馨提示(隐私合规示例)")
                .setMessage(spannable)
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MapsInitializer.updatePrivacyAgree(MyDriverListActivity.this,true);
                    }
                })
                .setNegativeButton("不同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MapsInitializer.updatePrivacyAgree(MyDriverListActivity.this,false);
                    }
                })
                .show();
    }

    private void setMyLocationStyle(){
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。        myLocationStyle.interval(2000);//定位蓝点展现模式，默认是LOCATION_TYPE_LOCATION_ROTATE        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//设置是否显示定位小蓝点，用于满足只想使用定位，不想使用定位小蓝点的场景，设置false以后图面上不再有定位蓝点的概念，但是会持续回调位置信息。        myLocationStyle.showMyLocation(true);//设置定位蓝点的Style
        myLocationStyle.interval(2000);
        //定位蓝点展现模式，默认是LOCATION_TYPE_LOCATION_ROTATE
        if(locationTypeIsMapRotate){
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);
        }else{
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            //myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);
        }
        //设置是否显示定位小蓝点，用于满足只想使用定位，不想使用定位小蓝点的场景，设置false以后图面上不再有定位蓝点的概念，但是会持续回调位置信息。
        myLocationStyle.showMyLocation(true);
        //设置定位蓝点的Style
        mAmap.setMyLocationStyle(myLocationStyle);
    }

}

package com.walkingsky.navi.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapException;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.PoiInputItemWidget;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusRouteResultV2;
import com.amap.api.services.route.Cost;
import com.amap.api.services.route.DrivePathV2;
import com.amap.api.services.route.DriveRouteResultV2;
import com.amap.api.services.route.RideRouteResultV2;
import com.amap.api.services.route.RouteSearchV2;
import com.amap.api.services.route.WalkRouteResultV2;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.walkingsky.navi.MyApp;
import com.walkingsky.navi.R;
import com.walkingsky.navi.activity.search.SearchPoiActivity;
import com.walkingsky.navi.util.AMapUtil;


import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;


import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyDriverListActivity extends Activity implements OnClickListener, OnLongClickListener, OnCheckedChangeListener, RouteSearchV2.OnRouteSearchListener, GeocodeSearch.OnGeocodeSearchListener {
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
    private LatLonPoint mStartPoint = null;
    private LatLonPoint mEndPoint = null;

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private Context mContext;

    private final List<NaviLatLng> startList = new ArrayList<>();
    private RouteSearchV2 mRouteSearch;
    private DriveRouteResultV2 mDriveRouteResultV2;
    //应用的全局变量接口
    private MyApp myApp = MyApp.getInstance();

    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<>();


    // 躲避点 经纬度 map
    private Map<String,LatLng> selectedMonitorsMap = new HashMap<>();

    private ProgressDialog progDialog = null;// 搜索时进度条

    private boolean isFirstLocation = true; //定位第一次触发

    private  double distanceRange = 0.000015;
    //点击地图时保存点击点的临时变量
    private LatLng tempLatLng;
    //定位点是否自动移动视角，旋转地图
    private boolean locationTypeIsMapRotate = false;
    private GeocodeSearch geocodeSearch;
    
    private  boolean startAndEndIsSwaped = false;
    private boolean isStartPoint = false;

    //长按地图显示的marker
    private Marker longClickMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //privacyCompliance();
        privacyComplianceMy();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_res_calculate);
        CheckBox congestion = findViewById(R.id.congestion);
        //定位跟随 checkbox
        CheckBox locationnaviCheckBox = findViewById(R.id.locationnavi);
        CheckBox costCheckBox = findViewById(R.id.cost);
        CheckBox hightspeedCheckBox = findViewById(R.id.hightspeed);
        CheckBox avoidhightspeedCheckBox =  findViewById(R.id.avoidhightspeed);
        //起点终点编辑框
        EditText startPoint = findViewById(R.id.editTextFrom);
        EditText endPoint = findViewById(R.id.editTextTo);
        //交换起始点按钮
        ImageButton imageButton = findViewById(R.id.swapButton);
        Button gpsnavi =  findViewById(R.id.gpsnavi);
        Button emulatornavi =  findViewById(R.id.emulatornavi);
        Button search =  findViewById(R.id.search);
        Spinner spinner =  findViewById(R.id.distance_range);
        startPoint.setOnClickListener(this);
        startPoint.setOnLongClickListener(this);
        endPoint.setOnClickListener(this);
        gpsnavi.setOnClickListener(this);
        emulatornavi.setOnClickListener(this);
        congestion.setOnCheckedChangeListener(this);
        //定位跟随 checkbox
        locationnaviCheckBox.setOnCheckedChangeListener(this);
        costCheckBox.setOnCheckedChangeListener(this);
        hightspeedCheckBox.setOnCheckedChangeListener(this);
        avoidhightspeedCheckBox.setOnCheckedChangeListener(this);
        search.setOnClickListener(this);
        imageButton.setOnClickListener(this);
        //禁用两个输入框的输入
        startPoint.setKeyListener(null);
        endPoint.setKeyListener(null);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = parent.getItemAtPosition(position).toString();
                distanceRange = Double.parseDouble(s);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 初始化GeocodeSearch对象
        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (com.amap.api.services.core.AMapException e) {
            throw new RuntimeException(e);
        }
        spinner.setSelection(1);

        //初始化定位
        initLocation();

        mRouteMapView =  findViewById(R.id.navi_view);
        mRouteMapView.onCreate(savedInstanceState);
        mAmap = mRouteMapView.getMap();
        addStartEndMarker();
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
        mAmap.setTrafficEnabled(true);
        //缩放地图到合适级别
        mAmap.moveCamera(CameraUpdateFactory.zoomTo(12));

        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            //mAMapNavi.addAMapNaviListener(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }

        try {
            mRouteSearch = new RouteSearchV2(this);
            mRouteSearch.setRouteSearchListener(this);
        } catch (com.amap.api.services.core.AMapException e) {
            throw new RuntimeException(e);
        }

        /*  调试证书
        String str = sHA1(this);
        Log.d("DEBUG","------:"+str);
        TextView textPathDetail = findViewById(R.id.textPathDetail);
        textPathDetail.setText(str);
        */

        mContext = this.getApplicationContext();
        //开启定位
        startLocation();
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
    protected void onSaveInstanceState(@NotNull Bundle outState) {
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
        endList.clear();
        routeOverlays.clear();
        mRouteMapView.onDestroy();
        /*
         * 当前页面只是展示地图，activity销毁后不需要再回调导航的状态
         */
        if (mAMapNavi!=null){
            //mAMapNavi.removeAMapNaviListener(this);
            mAMapNavi.destroy();
        }
        destroyLocation();

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        CheckBox costCheckBox = findViewById(R.id.cost);
        CheckBox hightspeedCheckBox = findViewById(R.id.hightspeed);
        CheckBox avoidhightspeedCheckBox =  findViewById(R.id.avoidhightspeed);
        switch (id) {
            case R.id.congestion:
                congestion = isChecked;
                break;
            case R.id.avoidhightspeed:
                avoidhightspeed = isChecked;
                if(isChecked) {
                    hightspeed = false;
                    hightspeedCheckBox.setChecked(false);
                }
                break;
            case R.id.cost:
                cost = isChecked;
                if(isChecked){
                    hightspeed = false;
                    hightspeedCheckBox.setChecked(false);
                }
                break;
            case R.id.hightspeed:
                hightspeed = isChecked;
                if(isChecked){
                    cost =false;
                    costCheckBox.setChecked(false);
                    avoidhightspeed=false;
                    avoidhightspeedCheckBox.setChecked(false);
                }
                break;
            case R.id.locationnavi:
                locationTypeIsMapRotate = isChecked;
                setMyLocationStyle();
            default:
                break;
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


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            //交换起始点
            case R.id.swapButton:
                if(mEndPoint == null){
                    break;
                }else {

                    if(mStartPoint != null) {
                        double lat = mEndPoint.getLatitude();
                        double lon = mEndPoint.getLongitude();
                        mEndPoint.setLatitude(mStartPoint.getLatitude());
                        mEndPoint.setLongitude(mStartPoint.getLongitude());
                        mStartPoint.setLatitude(lat);
                        mStartPoint.setLongitude(lon);
                    }else{
                        //定位起点
                        //进入程序后就定位了，暂时不需要手动定位了
                    }

                    mAmap.clear();// 清理地图上的所有覆盖物
                    addStartEndMarker();

                    startList.clear();
                    startList.add(new NaviLatLng(mStartPoint.getLatitude(),mStartPoint.getLongitude()));


                    endList.clear();
                    endList.add(new NaviLatLng(mEndPoint.getLatitude(),mEndPoint.getLongitude()));

                    TextView editText = findViewById(R.id.textPathDetail);
                    editText.setText("");

                    EditText startEditText = findViewById(R.id.editTextFrom);
                    EditText endEditText = findViewById(R.id.editTextTo);
                    String s = startEditText.getText().toString();
                    startEditText.setText(endEditText.getText().toString());
                    endEditText.setText(s);
                    startAndEndIsSwaped = ! startAndEndIsSwaped;
                }
                break;
            //选起点
            case R.id.editTextFrom:
                Intent sintent = new Intent(MyDriverListActivity.this, SearchPoiActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("pointType", PoiInputItemWidget.TYPE_START);
                sintent.putExtras(bundle);
                startActivityForResult(sintent, 100);
                break;
            //选终点
            case R.id.editTextTo:
                Intent eintent = new Intent(MyDriverListActivity.this, SearchPoiActivity.class);
                Bundle ebundle = new Bundle();
                ebundle.putInt("pointType", PoiInputItemWidget.TYPE_DEST);
                eintent.putExtras(ebundle);
                startActivityForResult(eintent, 200);
                break;
            //规划避让线路
            case R.id.search:
                clearRoute();
                //清空下路径节点，防止干扰
                mDriveRouteResultV2 = null;
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

                RouteSearchV2.DrivingStrategy drivingStrategy ;
                if(congestion && !(cost || hightspeed || avoidhightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_CONGESTION;
                else if(congestion && avoidhightspeed && !( cost || hightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_AVOID_HIGHWAY;
                else if(congestion && avoidhightspeed && cost && ! hightspeed)
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_LESS_CHARGE_AVOID_HIGHWAY;
                else if(congestion && hightspeed )
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_HIGHWAY_PRIORITY;
                else if(avoidhightspeed && !(congestion || cost || hightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_HIGHWAY;
                else if(avoidhightspeed && cost && !(congestion || hightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.LESS_CHARGE_AVOID_HIGHWAY;
                else if(hightspeed && ! congestion)
                    drivingStrategy = RouteSearchV2.DrivingStrategy.HIGHWAY_PRIORITY;
                else if(cost  && congestion && !( hightspeed || avoidhightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.AVOID_CONGESTION_LESS_CHARGE;
                else if(cost  && !(congestion || hightspeed || avoidhightspeed))
                    drivingStrategy = RouteSearchV2.DrivingStrategy.LESS_CHARGE;
                else
                    drivingStrategy = RouteSearchV2.DrivingStrategy.DEFAULT;

                if(selectedMonitorsMap.isEmpty()) {

                    RouteSearchV2.DriveRouteQuery query = new RouteSearchV2.DriveRouteQuery(fromAndTo, drivingStrategy, null,
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

                    RouteSearchV2.DriveRouteQuery query = new RouteSearchV2.DriveRouteQuery(fromAndTo, drivingStrategy, null,
                            avoidpolygons, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
                    //query.setShowFields(RouteSearchV2.ShowFields.COST|RouteSearchV2.ShowFields.NAVI|RouteSearchV2.ShowFields.POLINE|RouteSearchV2.ShowFields.TMCS);
                    query.setShowFields(RouteSearchV2.ShowFields.COST | RouteSearchV2.ShowFields.POLINE | RouteSearchV2.ShowFields.TMCS);
                    mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
                }
                break;
            //导航
            case R.id.gpsnavi:
                break;
            //模拟导航
            case R.id.emulatornavi:
                break;
            default:
                break;
        }
        int id = v.getId();
        if(id ==  R.id.gpsnavi || id ==R.id.emulatornavi)
        {
            //起点、终点不完整
            if(startList.isEmpty() || endList.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "起点或终点未设置",Toast.LENGTH_LONG).show();
            }else {
                Intent intent = new Intent(getApplicationContext(), RouteNaviActivity.class);
                myApp.setmDriveRouteResultV2(mDriveRouteResultV2);
                double startLatitude = startList.get(0).getLatitude();
                double startLongitude = startList.get(0).getLongitude();
                double endLatitude = endList.get(0).getLatitude();
                double endLongitude = endList.get(0).getLongitude();

                intent.putExtra("gps", true);
                intent.putExtra("hightspeed", hightspeed);
                intent.putExtra("avoidhightspeed", avoidhightspeed);
                intent.putExtra("cost", cost);
                intent.putExtra("congestion", congestion);

                intent.putExtra("startLatitude", startLatitude);
                intent.putExtra("startLongitude", startLongitude);
                intent.putExtra("endLatitude", endLatitude);
                intent.putExtra("endLongitude", endLongitude);

                if (id == R.id.emulatornavi) {
                    intent.putExtra("gps", false);
                    startActivityForResult(intent, 400);
                }else
                    startActivityForResult(intent, 300);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getParcelableExtra("poi") != null) {
            clearRoute();
            Poi poi = data.getParcelableExtra("poi");

            assert poi != null;
            LatLng mLatLng = poi.getCoordinate();

            if (requestCode == 100) {//起点选择完成
                //Toast.makeText(this, "100", Toast.LENGTH_SHORT).show();
                EditText startPoint = findViewById(R.id.editTextFrom);
                startPoint.setText(poi.getName());
                NaviLatLng startLatlng = new NaviLatLng(mLatLng.latitude, mLatLng.longitude);
                mStartMarker.setPosition(new LatLng(mLatLng.latitude, mLatLng.longitude));
                mStartPoint = new LatLonPoint(mLatLng.latitude, mLatLng.longitude);
                startList.clear();
                startList.add(startLatlng);
                //清除已经选择的躲避点 map
                selectedMonitorsMap.clear();
            }

            if (requestCode == 200) {//终点选择完成
                //Toast.makeText(this, "200", Toast.LENGTH_SHORT).show();
                EditText endPoint = findViewById(R.id.editTextTo);
                endPoint.setText(poi.getName());
                NaviLatLng endLatlng = new NaviLatLng(mLatLng.latitude, mLatLng.longitude);
                mEndMarker.setPosition(new LatLng(mLatLng.latitude, mLatLng.longitude));
                mEndPoint = new LatLonPoint(mLatLng.latitude, mLatLng.longitude);
                endList.clear();
                endList.add(endLatlng);
                //清除已经选择的躲避点 map
                selectedMonitorsMap.clear();

            }
        }
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
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onMapLongClick(LatLng latLng) {

            longClickMarker.setVisible(true);
            longClickMarker.setPosition(latLng);
            //保存 latlng 到全局变量
            tempLatLng = latLng;
            EditText editText =  findViewById(R.id.editTextTo);

            PopupMenu popupMenu = new PopupMenu(mContext,editText);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());
            //菜单消失，longClickMarker
            popupMenu.setOnDismissListener(menu -> {
                if(longClickMarker != null) {
                    //longClickMarker.remove();
                    longClickMarker.setVisible(false);
                }
            });

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()){
                    case R.id.set_start_point:
                        NaviLatLng startLatlng = new NaviLatLng(tempLatLng.latitude, tempLatLng.longitude);
                        mStartMarker.setPosition(new LatLng(tempLatLng.latitude, tempLatLng.longitude));
                        mStartPoint = new LatLonPoint(tempLatLng.latitude, tempLatLng.longitude);
                        startList.clear();
                        startList.add(startLatlng);
                        getPointPoiText(mStartPoint,true);
                        //清除已经选择的躲避点 map
                        selectedMonitorsMap.clear();
                        break;
                    case R.id.set_end_point:
                        NaviLatLng endLatlng = new NaviLatLng(tempLatLng.latitude, tempLatLng.longitude);
                        mEndMarker.setPosition(new LatLng(tempLatLng.latitude, tempLatLng.longitude));
                        mEndPoint = new LatLonPoint(tempLatLng.latitude, tempLatLng.longitude);
                        endList.clear();
                        endList.add(endLatlng);
                        getPointPoiText(mEndPoint,false);
                        //清除已经选择的躲避点 map
                        selectedMonitorsMap.clear();
                        break;
                }
                longClickMarker.setVisible(false);
                return true;
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
            //跳过 起始点和终点的marker
            if(marker.getTitle().isEmpty() )
                return  true;
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

            }
            return true;
        }
    };

    @Override
    public void onDriveRouteSearched(DriveRouteResultV2 driveRouteResultV2, int errorCode) {
        //mAmap.removeOnMarkerClickListener(markerClickListener);
        mAmap.clear();// 清理地图上的所有覆盖物
        addStartEndMarker();
        mAmap.setOnMarkerClickListener(markerClickListener);
        //selectedMonitorsMap.clear();
        if (errorCode == 1000) {
            if (driveRouteResultV2 != null && driveRouteResultV2.getPaths() != null) {
                if (driveRouteResultV2.getPaths().size() > 0) {
                    mDriveRouteResultV2 = driveRouteResultV2;
                    final DrivePathV2 drivePathV2 = mDriveRouteResultV2.getPaths().get(0);
                    Cost cost = drivePathV2.getCost();
                    String str = "耗时:"+ Math.round(cost.getDuration() / 60) + "分钟 红绿灯数:"
                            + cost.getTrafficLights()
                            +"\n 费用:"+ cost.getTolls() +"元";
                    TextView textPathDetail = findViewById(R.id.textPathDetail);
                    textPathDetail.setText(str);
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            mContext, mAmap, drivePathV2,
                            mDriveRouteResultV2.getStartPos(),
                            mDriveRouteResultV2.getTargetPos(), selectedMonitorsMap,null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();

                    drivingRouteOverlay.zoomToSpan();

                    drivingRouteOverlay.cancelableCallback.onFinish(); //通过事件，触发重新绘制标记点

                } else if ( driveRouteResultV2.getPaths() == null) {
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
            //Button locationButton =  findViewById(R.id.location);
            //locationButton.setEnabled(true);
            if (null != location) {
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if(location.getErrorCode() == 0){
                    if(isFirstLocation){
                        mStartPoint = new LatLonPoint(location.getLatitude(),location.getLongitude());

                        //Toast.makeText(getApplicationContext(), "定位成功："+location.getAddress(), Toast.LENGTH_LONG).show();
                        stopLocation();
                        NaviLatLng startLatlng = new NaviLatLng(location.getLatitude(), location.getLongitude());
                        mStartMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                        startList.clear();
                        startList.add(startLatlng);
                        getPointPoiText(mStartPoint,true);

                        mAmap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                        //mListener.onLocationChanged(location);// 显示系统小蓝点
                        isFirstLocation = false;
                    }
                    if(locationTypeIsMapRotate){
                        mAmap.setMyLocationRotateAngle(location.getBearing());
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
            /*
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

    private void getPointPoiText(LatLonPoint latLonPoint,boolean isstartpoint){
        //查询 POI,同步执行
        isStartPoint = isstartpoint;
        RegeocodeQuery regeocodeQuery = new RegeocodeQuery(latLonPoint,50,GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(regeocodeQuery);
    }

    /**
     * 地理位置反编码结果
     * @param regeocodeResult 结果
     * @param i 返回码
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        //转换成功
        if(i == 1000){
            String a = regeocodeResult.getRegeocodeAddress().getFormatAddress();
            EditText editText;
            if(isStartPoint){
                editText = findViewById(R.id.editTextFrom);
            }else{
                editText = findViewById(R.id.editTextTo);
            }
            editText.setText(a);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    /**
     * 清理地图覆盖物，除定位图标 和起始点marker
     */
    private void clearMap(){
        List<Marker> allMarkers = mAmap.getMapScreenMarkers();
        if(!allMarkers.isEmpty()){
            for(Marker marker:allMarkers){
                if(marker == null)
                    continue;

                String title = marker.getTitle();
                if(!title.isEmpty())
                    marker.remove();
            }
        }
    }

    /**
     * 添加起始点
     */
    private void addStartEndMarker()
    {
        // 初始化Marker添加到地图
        mStartMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource( R.drawable.start)));
        mEndMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource( R.drawable.end)));
        //长按地图 出现的临时标记点
        longClickMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource( R.drawable.r1)));
        if(mStartPoint != null)
            mStartMarker.setPosition(new LatLng(mStartPoint.getLatitude(), mStartPoint.getLongitude()));
        if(mEndPoint != null)
            mEndMarker.setPosition(new LatLng(mEndPoint.getLatitude(), mEndPoint.getLongitude()));
    }

    @Override
    public boolean onLongClick(View v) {
        //定位
        if(v.getId() == findViewById(R.id.editTextFrom).getId()){
            //重新启动定位，以获取当前位置（），实际应用中位置可能不会变化，所有要重启一下定位
            stopLocation();
            isFirstLocation = true;
            startLocation();
        }
        return false;
    }

    public static String sHA1(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}

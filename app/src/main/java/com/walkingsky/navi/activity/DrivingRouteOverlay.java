package com.walkingsky.navi.activity;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.VisibleRegion;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePathV2;
import com.amap.api.services.route.DriveStepV2;
import com.amap.api.services.route.TMC;
import com.walkingsky.navi.R;
import com.walkingsky.navi.util.AMapUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


/**
 * 导航路线图层类。
 */
public class DrivingRouteOverlay extends RouteOverlay{

	private DrivePathV2 drivePath;
    private List<LatLonPoint> throughPointList;
    private List<Marker> throughPointMarkerList = new ArrayList<Marker>();
    private boolean throughPointMarkerVisible = true;
    private List<TMC> tmcs;
    private PolylineOptions mPolylineOptions;
    private PolylineOptions mPolylineOptionscolor;
    private Context mContext;
    private boolean isColorfulline = true;
    private float mWidth = 25;
    private List<LatLng> mLatLngsOfPath;

    private Map<String,LatLng> selectedMonitorsMap;

    private double rectangleTopLat = 500;
    private double rectangleTopLon = 500;
    private double rectangleBottomLat = 0;
    private double rectangleBottomLon = 0;

	public void setIsColorfulline(boolean iscolorfulline) {
		this.isColorfulline = iscolorfulline;
	}

	/**
     * 根据给定的参数，构造一个导航路线图层类对象。
     *
     * @param amap      地图对象。
     * @param path 导航路线规划方案。
     * @param context   当前的activity对象。
     */
    public DrivingRouteOverlay(Context context, AMap amap, DrivePathV2 path,
                               LatLonPoint start, LatLonPoint end, Map<String,LatLng> selectedMonitorsMap, List<LatLonPoint> throughPointList) {
    	super(context);
    	mContext = context; 
        mAMap = amap; 
        this.drivePath = path;
        startPoint = AMapUtil.convertToLatLng(start);
        endPoint = AMapUtil.convertToLatLng(end);
        this.throughPointList = throughPointList;
        this.selectedMonitorsMap = selectedMonitorsMap;

        initBitmapDescriptor();
    }
    public float getRouteWidth() {
        return mWidth;
    }

    /**
     * 设置路线宽度
     *
     * @param mWidth 路线宽度，取值范围：大于0
     */
    public void setRouteWidth(float mWidth) {
        this.mWidth = mWidth;
    }

    /**
     * 添加驾车路线添加到地图上显示。
     */
	public void addToMap() {
		initPolylineOptions();
        try {
            if (mAMap == null) {
                return;
            }

            if (mWidth == 0 || drivePath == null) {
                return;
            }
            mLatLngsOfPath = new ArrayList<LatLng>();
            tmcs = new ArrayList<TMC>();
            List<DriveStepV2> drivePaths = drivePath.getSteps();
            mPolylineOptions.add(startPoint);
            for (int i = 0; i < drivePaths.size(); i++) {
                DriveStepV2 step = drivePaths.get(i);
                List<LatLonPoint> latlonPoints = step.getPolyline();
                List<TMC> tmclist = step.getTMCs();
                tmcs.addAll(tmclist);
                if(latlonPoints!=null) {
                    addDrivingStationMarkers(step, convertToLatLng(latlonPoints.get(0)));
                    for (LatLonPoint latlonpoint : latlonPoints) {
                        mPolylineOptions.add(convertToLatLng(latlonpoint));
                        mLatLngsOfPath.add(convertToLatLng(latlonpoint));
                    }
                }
            }
            mPolylineOptions.add(endPoint);
            if (startMarker != null) {
                startMarker.remove();
                startMarker = null;
            }
            if (endMarker != null) {
                endMarker.remove();
                endMarker = null;
            }
            //addStartAndEndMarker();
            addThroughPointMarker();
            if (isColorfulline && tmcs.size()>0 ) {
            	colorWayUpdate(tmcs);
            	showcolorPolyline();
			}else {
				showPolyline();
			}            
            
        } catch (Throwable e) {
        	e.printStackTrace();
        }
    }

	/**
     * 初始化线段属性
     */
    private void initPolylineOptions() {

        mPolylineOptions = null;

        mPolylineOptions = new PolylineOptions();
        mPolylineOptions.color(getDriveColor()).width(getRouteWidth());
    }

    private void showPolyline() {
        addPolyLine(mPolylineOptions);
    }
    
    private void showcolorPolyline() {
    	addPolyLine(mPolylineOptionscolor);
		
	}

    /**
     * 根据不同的路段拥堵情况展示不同的颜色
     *
     * @param tmcSection
     */
    private void colorWayUpdate(List<TMC> tmcSection) {
        if (mAMap == null) {
            return;
        }
        if (tmcSection == null || tmcSection.size() <= 0) {
            return;
        }
        TMC segmentTrafficStatus;
        mPolylineOptionscolor = null;
        mPolylineOptionscolor = new PolylineOptions();
        mPolylineOptionscolor.width(getRouteWidth());
        List<Integer> colorList = new ArrayList<Integer>();
        List<BitmapDescriptor> bitmapDescriptors = new ArrayList<BitmapDescriptor>();
        List<LatLng> points = new ArrayList<>();
        List<Integer> texIndexList = new ArrayList<Integer>();
//        mPolylineOptionscolor.add(startPoint);
//        mPolylineOptionscolor.add(AMapUtil.convertToLatLng(tmcSection.get(0).getPolyline().get(0)));

        points.add(startPoint);
        points.add(AMapUtil.convertToLatLng(tmcSection.get(0).getPolyline().get(0)));
        colorList.add(getDriveColor());
        bitmapDescriptors.add(defaultRoute);

        BitmapDescriptor bitmapDescriptor = null;
        int textIndex = 0;
        texIndexList.add(textIndex);
        texIndexList.add(++textIndex);
        for (int i = 0; i < tmcSection.size(); i++) {
        	segmentTrafficStatus = tmcSection.get(i);
        	int color = getcolor(segmentTrafficStatus.getStatus());
            bitmapDescriptor = getTrafficBitmapDescriptor(segmentTrafficStatus.getStatus());
        	List<LatLonPoint> mployline = segmentTrafficStatus.getPolyline();
			for (int j = 0; j < mployline.size(); j++) {
//				mPolylineOptionscolor.add(AMapUtil.convertToLatLng(mployline.get(j)));
				points.add(AMapUtil.convertToLatLng(mployline.get(j)));

                colorList.add(color);

                texIndexList.add(++textIndex);
                bitmapDescriptors.add(bitmapDescriptor);
			}

		}


        points.add(endPoint);
        colorList.add(getDriveColor());
        bitmapDescriptors.add(defaultRoute);
        texIndexList.add(++textIndex);
        mPolylineOptionscolor.addAll(points);
        mPolylineOptionscolor.colorValues(colorList);

//        mPolylineOptionscolor.setCustomTextureIndex(texIndexList);
//        mPolylineOptionscolor.setCustomTextureList(bitmapDescriptors);
    }

    private BitmapDescriptor defaultRoute = null;
    private BitmapDescriptor unknownTraffic = null;
    private BitmapDescriptor smoothTraffic = null;
    private BitmapDescriptor slowTraffic = null;
    private BitmapDescriptor jamTraffic = null;
    private BitmapDescriptor veryJamTraffic = null;
    private void initBitmapDescriptor() {
        defaultRoute = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_6_arrow);
        smoothTraffic = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_4_arrow);
        unknownTraffic = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_0_arrow);
        slowTraffic = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_3_arrow);
        jamTraffic = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_2_arrow);
        veryJamTraffic = BitmapDescriptorFactory.fromResource(R.drawable.amap_route_color_texture_9_arrow);

    }

    private BitmapDescriptor getTrafficBitmapDescriptor(String status) {
//        if (status.trim().equals("未知")) {
//            return unknownTraffic;
//        } else

        //Log.e("ggb", "==> 路况信息 is " + status);
        if (status.equals("畅通")) {
            return smoothTraffic;
        } else if (status.equals("缓行")) {
            return slowTraffic;
        } else if (status.equals("拥堵")) {
            return jamTraffic;
        } else if (status.equals("严重拥堵")) {
            return veryJamTraffic;
        } else {
            return defaultRoute;
        }
    }

    private int getcolor(String status) {

    	if (status.equals("畅通")) {
    		return Color.GREEN;
		} else if (status.equals("缓行")) {
			 return Color.YELLOW;
		} else if (status.equals("拥堵")) {
			return Color.RED;
		} else if (status.equals("严重拥堵")) {
			return Color.parseColor("#990033");
		} else {
			return Color.parseColor("#537edc");
		}	
	}

	public LatLng convertToLatLng(LatLonPoint point) {
        return new LatLng(point.getLatitude(),point.getLongitude());
  }
    
    /**
     * @param driveStep
     * @param latLng
     */
    private void addDrivingStationMarkers(DriveStepV2 driveStep, LatLng latLng) {
        addStationMarker(new MarkerOptions()
                .position(latLng)
                .title("\u65B9\u5411:" + driveStep.getInstruction()
                        + "\n\u9053\u8DEF:" + driveStep.getRoad())
                .snippet(driveStep.getInstruction())
                .visible(nodeIconVisible)
                .anchor(0.5f, 0.5f)
                .icon(getDriveBitmapDescriptor()));
    }

    @Override
    protected LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        if(rectangleBottomLat == 0){
            b.include(new LatLng(startPoint.latitude, startPoint.longitude));
            b.include(new LatLng(endPoint.latitude, endPoint.longitude));
        }else {
            b.include(new LatLng(rectangleTopLat, rectangleTopLon));
            b.include(new LatLng(rectangleBottomLat, rectangleBottomLon));
        }
        if (this.throughPointList != null && this.throughPointList.size() > 0) {
            for (int i = 0; i < this.throughPointList.size(); i++) {
                b.include(new LatLng(
                        this.throughPointList.get(i).getLatitude(),
                        this.throughPointList.get(i).getLongitude()));
            }
        }
        return b.build();
    }

    public void setThroughPointIconVisibility(boolean visible) {
        try {
            throughPointMarkerVisible = visible;
            if (this.throughPointMarkerList != null
                    && this.throughPointMarkerList.size() > 0) {
                for (int i = 0; i < this.throughPointMarkerList.size(); i++) {
                    this.throughPointMarkerList.get(i).setVisible(visible);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    private void addThroughPointMarker() {
        if (this.throughPointList != null && this.throughPointList.size() > 0) {
            LatLonPoint latLonPoint = null;
            for (int i = 0; i < this.throughPointList.size(); i++) {
                latLonPoint = this.throughPointList.get(i);
                if (latLonPoint != null) {
                    throughPointMarkerList.add(mAMap
                            .addMarker((new MarkerOptions())
                                    .position(
                                            new LatLng(latLonPoint
                                                    .getLatitude(), latLonPoint
                                                    .getLongitude()))
                                    .visible(throughPointMarkerVisible)
                                    .icon(getThroughPointBitDes())
                                    .title("\u9014\u7ECF\u70B9")));
                }
            }
        }
    }
    
    private BitmapDescriptor getThroughPointBitDes() {
    	return BitmapDescriptorFactory.fromResource(R.drawable.amap_through);
       
    }

    /**
     * 获取两点间距离
     *
     * @param start
     * @param end
     * @return
     */
    public static int calculateDistance(LatLng start, LatLng end) {
        double x1 = start.longitude;
        double y1 = start.latitude;
        double x2 = end.longitude;
        double y2 = end.latitude;
        return calculateDistance(x1, y1, x2, y2);
    }

    public static int calculateDistance(double x1, double y1, double x2, double y2) {
        final double NF_pi = 0.01745329251994329; // 弧度 PI/180
        x1 *= NF_pi;
        y1 *= NF_pi;
        x2 *= NF_pi;
        y2 *= NF_pi;
        double sinx1 = Math.sin(x1);
        double siny1 = Math.sin(y1);
        double cosx1 = Math.cos(x1);
        double cosy1 = Math.cos(y1);
        double sinx2 = Math.sin(x2);
        double siny2 = Math.sin(y2);
        double cosx2 = Math.cos(x2);
        double cosy2 = Math.cos(y2);
        double[] v1 = new double[3];
        v1[0] = cosy1 * cosx1 - cosy2 * cosx2;
        v1[1] = cosy1 * sinx1 - cosy2 * sinx2;
        v1[2] = siny1 - siny2;
        double dist = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2]);

        return (int) (Math.asin(dist / 2) * 12742001.5798544);
    }


    //获取指定两点之间固定距离点
    public static LatLng getPointForDis(LatLng sPt, LatLng ePt, double dis) {
        double lSegLength = calculateDistance(sPt, ePt);
        double preResult = dis / lSegLength;
        return new LatLng((ePt.latitude - sPt.latitude) * preResult + sPt.latitude, (ePt.longitude - sPt.longitude) * preResult + sPt.longitude);
    }
    /**
     * 去掉DriveLineOverlay上的线段和标记。
     */
    @Override
    public void removeFromMap() {
        try {
            super.removeFromMap();
            if (this.throughPointMarkerList != null
                    && this.throughPointMarkerList.size() > 0) {
                for (int i = 0; i < this.throughPointMarkerList.size(); i++) {
                    this.throughPointMarkerList.get(i).remove();
                }
                this.throughPointMarkerList.clear();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 移动镜头到当前的视角。
     * @since V2.1.0
     */
    public void zoomToSpan() {
        if (startPoint != null) {
            if (mAMap == null)
                return;
            try {
                LatLngBounds bounds = getLatLngBounds();
                mAMap.animateCamera(CameraUpdateFactory
                        .newLatLngBounds(bounds, 100), cancelableCallback);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 监听动画，动画结束后算地图显示范围内的探头
     */
    AMap.CancelableCallback cancelableCallback = new AMap.CancelableCallback() {
        @Override
        public void onFinish() {
            try {
                VisibleRegion bounds = mAMap.getProjection().getVisibleRegion();
                //
                getAvoidpolygons(AgentKind.inSixRing);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCancel() {

        }
    };


    public static final String API_URL = "https://后台服务器域名";

    public enum AgentKind  {all,inSixRing,outSixRing}

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

    public interface ApiGetAllMonitorList {
        //String data = "point1_Longitude="+startPoint.longitude+"&point1_Latitude="+startPoint.latitude
        //        +"&point2_Longitude="+endPoint.longitude+"&point2_Latitude="+endPoint.latitude;
        @GET("apis/data")
        Call<List<Monitor>> getMonitors(@Query("file_name") String kind,
                                        @Query("point1_Longitude") String point1_Longitude, @Query("point1_Latitude") String point1_Latitude,
                                        @Query("point2_Longitude") String point2_Longitude, @Query("point2_Latitude") String point2_Latitude);
    }


    /**
     * 获取起点和终点围成的长方形区域内 探头的经纬坐标点
     * @param  kind AgentKind 探头类型
     * @return List<LatLonPoint>
     */
    public  void getAvoidpolygons( AgentKind kind) throws IOException
    {
        // Create a very simple REST adapter which points the GitHub API.

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

        DrivePathV2 mDrivePath = this.drivePath;
        List<LatLonPoint> mLatLonPointList = mDrivePath.getPolyline();
        for (LatLonPoint point : mLatLonPointList) {
            if( point.getLatitude() < rectangleTopLat)
                rectangleTopLat = point.getLatitude();
            if(point.getLongitude() < rectangleTopLon)
                rectangleTopLon = point.getLongitude();
            if(point.getLatitude() > rectangleBottomLat)
                rectangleBottomLat = point.getLatitude();
            if(point.getLongitude() > rectangleBottomLon)
                rectangleBottomLon = point.getLongitude();
        }

        rectangleTopLat = rectangleTopLat - 0.005;
        rectangleTopLon = rectangleTopLon - 0.005;
        rectangleBottomLat = rectangleBottomLat + 0.005;
        rectangleBottomLon = rectangleBottomLon + 0.005;

        //
        DrivingRouteOverlay.ApiGetAllMonitorList apiMonitorList = retrofit.create(DrivingRouteOverlay.ApiGetAllMonitorList.class);


        Call<List<Monitor>> call = apiMonitorList.getMonitors(
                kindStr,
                String.valueOf(rectangleTopLon),
                String.valueOf(rectangleTopLat),
                String.valueOf(rectangleBottomLon),
                String.valueOf(rectangleBottomLat));


        //异步请求
        //对 发送请求 进行封装
        call.enqueue(new Callback<List<Monitor>>() {
            //请求成功时回调
            @Override
            public void onResponse(Call<List<Monitor>> call, Response<List<Monitor>> response) {

                List<Monitor> monitorLists = response.body();
                if (monitorLists != null) {
                    for (Monitor monitorList : monitorLists) {
                        LatLng latLng = new LatLng(monitorList.Latitude,monitorList.Longitude);
                        Marker mMonitorsMarker;
                        boolean haveInMap = false;
                        String mapKey = null;
                        for (String key: selectedMonitorsMap.keySet()) {
                            if( selectedMonitorsMap.get(key).equals(latLng) ){
                                haveInMap = true;
                                mapKey = key;
                            }
                        }
                        if(haveInMap){
                            mMonitorsMarker = mAMap.addMarker(new MarkerOptions().icon(
                                    BitmapDescriptorFactory.fromBitmap(
                                            BitmapFactory.decodeResource(
                                                    mContext.getResources(), R.drawable.amap_monitor_selected)))
                                    .title("1"));
                            selectedMonitorsMap.remove(mapKey);
                            selectedMonitorsMap.put(mMonitorsMarker.getId(),latLng);
                        }else{
                            mMonitorsMarker = mAMap.addMarker(new MarkerOptions().icon(
                                    BitmapDescriptorFactory.fromBitmap(
                                            BitmapFactory.decodeResource(
                                                    mContext.getResources(), R.drawable.amap_monitor_point)))
                                    .title("0"));
                        }
                        mMonitorsMarker.setPosition(latLng);
                    }
                }

            }
            //请求失败时候的回调
            @Override
            public void onFailure(Call<List<Monitor>> call, Throwable throwable) {
                throwable.printStackTrace();
                System.out.println("连接失败");
            }
        });
    }

    public Map<String, LatLng> getSelectedMonitorsMap(){
        return this.selectedMonitorsMap;
    }
}
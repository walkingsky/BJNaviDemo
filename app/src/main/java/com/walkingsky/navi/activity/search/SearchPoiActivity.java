package com.walkingsky.navi.activity.search;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.view.PoiInputItemWidget;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.walkingsky.navi.R;
import com.walkingsky.navi.activity.MyDriverListActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchPoiActivity extends Activity implements TextWatcher,
        Inputtips.InputtipsListener, AdapterView.OnItemClickListener, View.OnTouchListener, View.OnClickListener, PoiSearchV2.OnPoiSearchListener {
    private AutoCompleteTextView mKeywordText;
    private ListView resultList;
    private List<Tip> mCurrentTipList;
    private SearchResultAdapter resultAdapter;
    private ProgressBar loadingBar;
    private TextView tvMsg;
    private Poi selectedPoi;
    private String city = "北京市";
    private int pointType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_poi);
        findViews();
        resultList.setOnItemClickListener(this);
        resultList.setOnTouchListener(this);

        tvMsg.setVisibility(View.GONE);
        mKeywordText.addTextChangedListener(this);
        mKeywordText.requestFocus();
        Bundle bundle = getIntent().getExtras();
        pointType = bundle.getInt("pointType", -1);

        getFavoriteList(true);

    }

    private void getFavoriteList(boolean reFresh){

        //if(reFresh)
            mCurrentTipList = new ArrayList<Tip>();

        SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        int pointsNumbers = sharedPreferences.getInt("points_numbers",0);
        //添加本地存储的 地址
        if(pointsNumbers>0){
            for(int i = 0 ; i < pointsNumbers; i++){
                Tip tip = new Tip();
                String iStr =  String.valueOf(i);
                tip.setName(sharedPreferences.getString("points_name_" + iStr,""));
                tip.setID(sharedPreferences.getString("points_poiID_" + iStr,""));
                tip.setAddress(sharedPreferences.getString("points_address_" + iStr,""));
                double a =  ((double)sharedPreferences.getLong("points_lat_" + iStr,0)/1000000);
                tip.setPostion(new LatLonPoint(
                         ((double)sharedPreferences.getLong("points_lat_" + iStr,0)/1000000),
                         ((double)sharedPreferences.getLong("points_lon_" + iStr,0)/1000000)
                ));
                mCurrentTipList.add(tip);
            }
            //if(reFresh){
                resultList.setVisibility(View.VISIBLE);
                SearchResultAdapter.ScreenSize screenSize = new SearchResultAdapter.ScreenSize();
                screenSize.widthPx = getScreenWidth();
                screenSize.pdToPxRatio = pdToPxRatio();
                resultAdapter = new SearchResultAdapter(getApplicationContext(), mCurrentTipList,screenSize);
                resultList.setAdapter(resultAdapter);
                resultAdapter.notifyDataSetChanged();
            //}
        }
    }


    private void findViews() {
        mKeywordText = (AutoCompleteTextView) findViewById(R.id.search_input);
        resultList = (ListView) findViewById(R.id.resultList);
        loadingBar = (ProgressBar) findViewById(R.id.search_loading);
        tvMsg = (TextView) findViewById(R.id.tv_msg);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        try {
            {
                if (tvMsg.getVisibility() == View.VISIBLE) {
                    tvMsg.setVisibility(View.GONE);
                }
                String newText = s.toString().trim();
                if (!TextUtils.isEmpty(newText)) {
                    setLoadingVisible(true);
                    InputtipsQuery inputquery = new InputtipsQuery(newText, city);
                    Inputtips inputTips = new Inputtips(getApplicationContext(), inputquery);
                    inputTips.setInputtipsListener(this);
                    inputTips.requestInputtipsAsyn();
                } else {
                    resultList.setVisibility(View.GONE);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void setLoadingVisible(boolean isVisible) {
        if (isVisible) {
            loadingBar.setVisibility(View.VISIBLE);
        } else {
            loadingBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //点击提示后再次进行搜索，获取POI出入口信息
        if (mCurrentTipList != null) {
            Tip tip = (Tip) parent.getItemAtPosition(position);
            selectedPoi = new Poi(tip.getName(), new LatLng(tip.getPoint().getLatitude(), tip.getPoint().getLongitude()), tip.getPoiID());
            if (!TextUtils.isEmpty(selectedPoi.getPoiId())) {
                PoiSearchV2.Query query = new PoiSearchV2.Query(selectedPoi.getName(), "", city);
                query.setDistanceSort(false);
                //query.requireSubPois(true);
                PoiSearchV2 poiSearch = null;
                try {
                    poiSearch = new PoiSearchV2(getApplicationContext(), query);
                    poiSearch.setOnPoiSearchListener(this);
                    poiSearch.searchPOIIdAsyn(selectedPoi.getPoiId());
                } catch (AMapException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        setLoadingVisible(false);
        try {
            if (rCode == 1000) {
                mCurrentTipList = new ArrayList<Tip>();

                getFavoriteList(false);

                for (Tip tip : tipList) {
                    if (null == tip.getPoint()) {
                        continue;
                    }
                    mCurrentTipList.add(tip);
                }

                if (null == mCurrentTipList || mCurrentTipList.isEmpty()) {
                    tvMsg.setText("抱歉，没有搜索到结果，请换个关键词试试");
                    tvMsg.setVisibility(View.VISIBLE);
                    resultList.setVisibility(View.GONE);
                } else {
                    resultList.setVisibility(View.VISIBLE);
                    SearchResultAdapter.ScreenSize screenSize = new SearchResultAdapter.ScreenSize();
                    screenSize.widthPx = getScreenWidth();
                    screenSize.pdToPxRatio = pdToPxRatio();
                    resultAdapter = new SearchResultAdapter(getApplicationContext(), mCurrentTipList,screenSize);
                    resultList.setAdapter(resultAdapter);
                    resultAdapter.notifyDataSetChanged();
                }
            } else {
                tvMsg.setText("出错了，请稍后重试");
                tvMsg.setVisibility(View.VISIBLE);
            }
        } catch (Throwable e) {
            tvMsg.setText("出错了，请稍后重试");
            tvMsg.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }


    @Override
    public void onPoiSearched(PoiResultV2 poiResultV2, int i) {

    }

    @Override
    public void onPoiItemSearched(PoiItemV2 poiItem, int errorCode) {
        try {
            LatLng latLng = null;
            int code = 0;
            if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                if (poiItem == null) {
                    return;
                }
                /*
                LatLonPoint exitP = poiItem.getExit();
                LatLonPoint enterP = poiItem.getEnter();
                if (pointType == PoiInputItemWidget.TYPE_START) {
                    code = 100;
                    if (exitP != null) {
                        latLng = new LatLng(exitP.getLatitude(), exitP.getLongitude());
                    } else {
                        if (enterP != null) {
                            latLng = new LatLng(enterP.getLatitude(), enterP.getLongitude());
                        }
                    }
                }
                if (pointType == PoiInputItemWidget.TYPE_DEST) {
                    code = 200;
                    if (enterP != null) {
                        latLng = new LatLng(enterP.getLatitude(), enterP.getLongitude());
                    }
                }
                 */
                LatLonPoint enterP = poiItem.getLatLonPoint();
                if (pointType == PoiInputItemWidget.TYPE_START)
                    code = 100;
                if (pointType == PoiInputItemWidget.TYPE_DEST)
                    code = 200;
                if(enterP != null)
                    latLng = new LatLng(enterP.getLatitude(),enterP.getLongitude());
            }
            Poi poi;
            if (latLng != null) {
                poi = new Poi(selectedPoi.getName(), latLng, selectedPoi.getPoiId());
            } else {
                poi = selectedPoi;
            }
            //Intent intent = new Intent(this, RestRouteShowActivity.class);
            Intent intent = new Intent(this, MyDriverListActivity.class);
            intent.putExtra("poi", poi);
            setResult(code, intent);
            finish();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取屏幕的宽度
     * @return int 宽度值px
     */
    private int getScreenWidth(){
        DisplayMetrics metrics=new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private int pdToPxRatio(){
        return (int)(this.getResources().getDisplayMetrics().density+0.5f);
    }

}

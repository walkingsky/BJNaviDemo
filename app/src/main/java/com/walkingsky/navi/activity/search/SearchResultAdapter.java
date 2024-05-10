package com.walkingsky.navi.activity.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Tip;
import com.walkingsky.navi.R;

import java.util.List;

/**
 *
 */
public class SearchResultAdapter extends BaseAdapter {
    private Context mContext;
    private List<Tip> mListTips;
    private LayoutInflater layoutInflater;
    private ScreenSize screen;

    public SearchResultAdapter(Context context, List<Tip> tipList ,ScreenSize screenSize) {
        mContext = context;
        mListTips = tipList;
        layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        screen = screenSize;
    }

    @Override
    public int getCount() {
        if (mListTips != null) {
            return mListTips.size();
        }
        return 0;
    }


    @Override
    public Object getItem(int i) {
        if (mListTips != null) {
            return mListTips.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        try {
            Holder holder;
            if (view == null) {
                holder = new Holder();
                view = layoutInflater.inflate(R.layout.search_result_item, null);
                holder.mName = (TextView) view.findViewById(R.id.name);
                holder.mAddress = (TextView) view.findViewById(R.id.adress);
                //新增几个控件
                holder.linearLayoutChild = (LinearLayout) view.findViewById(R.id.linearLayoutChild);
                holder.favoriteButton = (ImageButton) view.findViewById(R.id.favoriteButton);
                //给收藏按钮添加点击事件
                holder.favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(v.getTag() == null)
                            return;
                        String tag = (String) v.getTag();
                        SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", mContext.MODE_PRIVATE);
                        int pointsNumbers = sharedPreferences.getInt("points_numbers",0);
                        //添加本地存储的 地址
                        boolean haveTag = false;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        for(int cnt = 0 ;cnt < pointsNumbers;cnt++){
                            String pStr = String.valueOf(cnt);
                            String nTag = sharedPreferences.getString("points_tag_" + pStr,"");

                            //找到了一个一样就移除，后面的递补
                            if(nTag.equals(tag)){
                                for(int j = cnt;j<pointsNumbers;j++){
                                    String p = String.valueOf(j);
                                    String jStr = String.valueOf(j+1);

                                    String mName = sharedPreferences.getString("points_name_" + jStr,"");
                                    String mId = sharedPreferences.getString("points_poiID_" + jStr,"");
                                    String mAddress = sharedPreferences.getString("points_address_" + jStr,"");
                                    Long mLat = sharedPreferences.getLong("points_lat_" + jStr,0);
                                    Long mLon = sharedPreferences.getLong("points_lon_" + jStr,0);
                                    String mTag = sharedPreferences.getString("points_tag_" + jStr,"");

                                    editor.remove("points_name_" + p);
                                    editor.remove("points_poiID_" + p);
                                    editor.remove("points_address_" + p);
                                    editor.remove("points_lat_" + p);
                                    editor.remove("points_lon_" + p);
                                    editor.remove("points_tag_" + p);
                                    if(mTag != ""){
                                        editor.putString("points_name_" + p,mName);
                                        editor.putString("points_poiID_" + p,mId);
                                        editor.putString("points_address_" + p,mAddress);
                                        editor.putLong("points_lat_" + p,mLat);
                                        editor.putLong("points_lon_" + p,mLon);
                                        editor.putString("points_tag_"+p,mTag);
                                    }
                                    haveTag = true;
                                }
                                editor.remove("points_numbers");
                                editor.putInt("points_numbers",pointsNumbers-1);
                                editor.commit();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ((ImageButton) v).setImageDrawable(mContext.getDrawable(android.R.drawable.btn_star_big_off));
                                }
                                break;
                            }
                        }

                        if(!haveTag){
                            String iStr =  String.valueOf(pointsNumbers);
                            int cnt = 0;
                            for(;cnt < mListTips.size();cnt++)
                                if(tag == mListTips.get(cnt).getPoiID())
                                    break;

                            editor.putString("points_name_" + iStr,mListTips.get(cnt).getName());
                            editor.putString("points_poiID_" + iStr,mListTips.get(cnt).getPoiID());
                            editor.putString("points_address_" + iStr,mListTips.get(cnt).getAddress());
                            editor.putLong("points_lat_" + iStr,(long)(mListTips.get(cnt).getPoint().getLatitude()*1000000));
                            editor.putLong("points_lon_" + iStr,(long)(mListTips.get(cnt).getPoint().getLongitude()*1000000));
                            editor.putString("points_tag_"+iStr,tag);
                            editor.remove("points_numbers");
                            editor.putInt("points_numbers",pointsNumbers+1);
                            editor.commit();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ((ImageButton) v).setImageDrawable(mContext.getDrawable(android.R.drawable.btn_star_big_on));
                            }
                        }
                        //更新界面
                        //notifyDataSetChanged();
                    }
                });
                view.setTag(holder);
            } else {
                holder = (Holder) view.getTag();
            }
            if (mListTips == null) {
                return view;
            }

            holder.mName.setText(mListTips.get(i).getName());
            String address = mListTips.get(i).getAddress();

            holder.favoriteButton.setTag(mListTips.get(i).getPoiID());
            if (TextUtils.isEmpty(address)) {
                holder.mAddress.setVisibility(View.GONE);
            } else {
                holder.mAddress.setVisibility(View.VISIBLE);
                holder.mAddress.setText(address);
            }
            //动态设置宽度
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.linearLayoutChild.getLayoutParams();
            layoutParams.width = screen.widthPx - (50+20) * screen.pdToPxRatio ;
            holder.linearLayoutChild.setLayoutParams(layoutParams);
            //根据缓存配置，更新ImageButton的按钮图片
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", mContext.MODE_PRIVATE);
            boolean haveTag = false;
            for(int cnt = 0 ; cnt < sharedPreferences.getInt("points_numbers",0); cnt++){
                String a = sharedPreferences.getString("points_tag_"+ String.valueOf(cnt),"");
                String b = mListTips.get(i).getPoiID();
                if( a.equals(b)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        holder.favoriteButton.setImageDrawable(mContext.getDrawable(android.R.drawable.btn_star_big_on));
                    }
                    haveTag = true;
                    break;
                }
            }
            if(!haveTag)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.favoriteButton.setImageDrawable(mContext.getDrawable(android.R.drawable.btn_star_big_off));
                }

        } catch (Throwable e) {
        }
        return view;
    }

    class Holder {
        LinearLayout linearLayoutChild;
        TextView mName;
        TextView mAddress;
        ImageButton favoriteButton;
    }

    static class ScreenSize{
        int widthPx;
        int pdToPxRatio;
    }
}

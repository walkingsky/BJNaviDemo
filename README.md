根据高德官方demo修改来的一个安卓自定义导航APP源代码。主要用来外地车牌车辆在BeiJ区域的自定义导航。

1. ####  准备后台

   参考“python后台“目录的python代码，只有一个接口 ，从探头坐标点json数据文件中筛选出对应的数组数据

   **接口：**

   `https://服务器域名/api/data`；

   **参数 为两个坐标点围成的一个矩形区域：**

   `point1_Longitude=坐标点1的经度浮点数&point1_Latitude=坐标点1的纬度浮点数&point2_Longitude=坐标点2的经度浮点数&point2_Latitude=坐标点2的纬度浮点数`

   可以直接将python代码目录中的文件部署到flask环境中

   **返回数据：**json数据，为矩形区域内的探头对象数组

2. #### 编译、安装

   1. 安装高德官方的SDK。从https://lbs.amap.com/api/android-navi-sdk/download ，下载android 合包中的导航合包，将压缩包中的文件解压到代码中的app\libs目录

   2. 申请高德的ApiKey，填写如代码的配置文件中。app\src\main\AndroidManifest.xml 文件中的 
      ```xml
      <application
              android:name=".MyApp"
              android:icon="@drawable/ic_launcher"
              android:label="@string/app_name"
              android:theme="@android:style/Theme.Light">
              <meta-data
                  android:name="com.amap.api.v2.apikey"
                  android:value="你的高德apikey" />
      
              <service android:name="com.amap.api.location.APSService" />
      ```
      
   3. 修改代码中的服务器的域名。activity目录中的 DrivingRouteOverlay.java 文件，456行
      ```java
      public static final String API_URL = "https://后台接口域名";  //后台接口 域名
      ```

   4. 编译
   
      ​      

3. #### 使用步骤

   1. 选择起始点。可以点击“选起点”手动搜索地点作为起点，也可以点击“定位起点”，自动定位当前位置为起始点
   2. 选择终点
   3. 规划避让线路。点击后高德自动推荐出一条线路，此时地图上会出现探头标记点，点击标记点，可以设置为避让点，再次点击取消选择。选好需要避让的标记点后，再次点击“规划避让线路”，高德自动将选择的点作为避让区域重新规划线路。重复此步骤可以规划出一条合理的线路；探头范围下拉选择，可以设置以探头坐标点为中心的避让区域的大小。（大概0.00001是左右1米的范围。即以探头坐标点为中心，长宽2米的正方形区域。）
   4. 点击“规划导航线路”，自动将之前的线路转换为导航线路
   5. 点击“模拟导航”，或者“开始导航”
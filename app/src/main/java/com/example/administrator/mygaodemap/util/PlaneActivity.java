package com.example.administrator.mygaodemap.util;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.administrator.mygaodemap.IBackService;
import com.example.administrator.mygaodemap.Operation.PointsUtil;
import com.example.administrator.mygaodemap.Operation.SmoothMarker;
import com.example.administrator.mygaodemap.R;
import com.example.administrator.mygaodemap.TicketActivity;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PlaneActivity extends AppCompatActivity implements LocationSource,AMapLocationListener {
    MapView mMapView = null;
    private int twoToExit=0;
    AMap aMap;
    LocationSource.OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;
    AMapLocationClientOption mLocationOption;
    public Intent now_intent;
    private TextView query_ticket;
    private boolean ishasticket=false;
    private boolean ishasplane=false;
    private IBackService iBackService;
    private String message;
    private ProgressDialog progress;
    double latitude=0;
    double longitude=0;

    /**---------------------------------------------------------------------------------*/
    private LatLng start;
    private LatLng end;
    private LatLng firstLatLng;
    /**---------------------------------------------------------------------------------*/

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iBackService = null;

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iBackService = IBackService.Stub.asInterface(service);
        }
    };

    private Intent mServiceIntent;
    public TextView tv;
    public String ticket_id;
    public String current_taskdate;
    public String planeID;
    public String planeHeight;
    class MessageBackReciver extends BroadcastReceiver {
        private WeakReference<TextView> textView;

        public MessageBackReciver(TextView tv) {
            textView = new WeakReference<TextView>(tv);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            TextView tv = textView.get();
            if (action.equals(SocketService.HEART_BEAT_ACTION)) {
                if (null != tv) {
                    tv.setText("Get a heart heat");
                }
            } else {
                String message = intent.getStringExtra("message");
                char[] strChar = message.substring(0, 1).toCharArray();
                char firstChar = strChar[0];


                if(firstChar=='{')
                {
                    try {
                        JSONObject json=new JSONObject(message);
                        if(json.getString("type").equals("生成订单"))
                        {
                            ticket_id=json.getString("xinxi");
                            ishasticket=true;
                        }
                        else if(json.getString("type").equals("查询订单"))
                        {
                            //ishasticket=false;
                            current_taskdate=json.getString("xinxi");
                            String a[]=current_taskdate.split("###");
                            if(a[15].equals("配送中       "))
                            {
                                progress.dismiss();
                                planeID=a[1];
                                planeHeight=a[4];
                                ishasticket=false;
                                ishasplane=true;

                            }
                        }
                        else if(json.getString("type").equals("查询无人机"))
                        {
                            String a[]=json.getString("xinxi").split("###");
                            latitude=Double.parseDouble(a[2]);
                            longitude=Double.parseDouble(a[3]);
                            planeHeight=a[4];
                        }
                    }catch (Exception e)
                    {

                    }


                }
            }
        };


    }
    private MessageBackReciver mReciver;
    private IntentFilter mIntentFilter;
    private GoogleApiClient client;
    private String jsonStr;
    private Marker marker=null;
    private Marker marker1=null;
    Button messagebtn_fanghuo;
    Button messagebtn_shouhuo;
    TextView facebtn_check;

    Timer timer = new Timer();
    public List<Map<String, Object>> result;

    private LocalBroadcastManager mLocalBroadcastManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plane);
        tv=(TextView)findViewById(R.id.tv);
        messagebtn_fanghuo=(Button)findViewById(R.id.message_fanghuo);
        messagebtn_shouhuo=(Button)findViewById(R.id.message_shouhuo);
        facebtn_check=(TextView) findViewById((R.id.face_check));
        now_intent=getIntent();

        progress = new ProgressDialog(PlaneActivity.this);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReciver = new MessageBackReciver(tv);
        mServiceIntent = new Intent(this, SocketService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(SocketService.HEART_BEAT_ACTION);
        mIntentFilter.addAction(SocketService.MESSAGE_ACTION);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        mMapView = (MapView) findViewById(R.id.map_plane);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
            //设置显示定位按钮 并且可以点击
            UiSettings settings = aMap.getUiSettings();
            aMap.setLocationSource(this);
            // 是否显示定位按钮
            settings.setMyLocationButtonEnabled(true);
            // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            aMap.setMyLocationEnabled(true);
            // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
            aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        }
        MarkerOptions markerOption1 = new MarkerOptions();//创建marker设置对象
        LatLng latLng1=new LatLng(Double.parseDouble(now_intent.getStringExtra("start_lati")),Double.parseDouble(now_intent.getStringExtra("start_long")));
        markerOption1.position(latLng1);
        markerOption1.draggable(true);
        markerOption1.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_st)));
        marker1 = aMap.addMarker(markerOption1);//添加图标


        MarkerOptions markerOption2 = new MarkerOptions();//创建marker设置对象
        LatLng latLng2=new LatLng(Double.parseDouble(now_intent.getStringExtra("end_lati")),Double.parseDouble(now_intent.getStringExtra("end_long")));
        markerOption2.position(latLng2);
        markerOption2.draggable(true);
        markerOption2.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_en)));
        marker1 = aMap.addMarker(markerOption2);//添加图标

        Intent it=getIntent();
        if(it.getStringExtra("status")!=null&&it.getStringExtra("status").equals("配送中"))
        {
            ishasplane=true;
            planeID=it.getStringExtra("PlaneID");
            ticket_id=it.getStringExtra("ticket_id");
        }
        if(it.getStringExtra("whosender")!=null&&it.getStringExtra("whosender").equals("yes"))
        {
            messagebtn_fanghuo.setVisibility(View.VISIBLE);
        }
        if(it.getStringExtra("whoreciever")!=null&&it.getStringExtra("whoreciever").equals("yes"))
        {
            messagebtn_shouhuo.setVisibility(View.VISIBLE);
        }
        if(it.getStringExtra("whosender")==null&&it.getStringExtra("whoreciever")==null)
        {
            messagebtn_fanghuo.setClickable(false);
            messagebtn_shouhuo.setClickable(false);
            messagebtn_fanghuo.setVisibility(View.INVISIBLE);
            messagebtn_shouhuo.setVisibility(View.INVISIBLE);
        }
        messagebtn_fanghuo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("progress","Click!");
                Log.i("progress",latitude+":"+Double.parseDouble(now_intent.getStringExtra("start_lati")));
                Log.i("progress",longitude+":"+Double.parseDouble(now_intent.getStringExtra("start_long")));
                Log.i("progress","enenen"+planeHeight);
                if(messagebtn_fanghuo.getText().equals("确认放货"))
                {
                    Log.i("progress","haha");
                    Log.i("progress",latitude+":"+Double.parseDouble(now_intent.getStringExtra("start_lati")));
                    Log.i("progress",longitude+":"+Double.parseDouble(now_intent.getStringExtra("start_long")));
                    Log.i("progress","enenen"+planeHeight);
                    HashMap<String,Object> hashmap=new HashMap<>();
                    Intent it=getIntent();
                    String content=ticket_id+"###"+planeID;
                    hashmap.put("id",it.getStringExtra("userID"));
                    hashmap.put("password","hahaha");
                    hashmap.put("clienttype","用户端");
                    hashmap.put("taskid","无人机接收货物");
                    hashmap.put("type","无人机接收货物");
                    hashmap.put("taskdate",content);
                    try {
                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                        Toast.makeText(PlaneActivity.this, isSend ? "success" : "fail",
                                Toast.LENGTH_SHORT).show();
                        if(isSend)
                        {
                            messagebtn_fanghuo.setClickable(false);
                            messagebtn_fanghuo.setVisibility(View.INVISIBLE);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        messagebtn_shouhuo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(messagebtn_shouhuo.getText().equals("确认收货"))
                {
                    HashMap<String,Object> hashmap=new HashMap<String,Object>();
                    Intent it=getIntent();
                    String content=ticket_id+"###"+planeID;
                    hashmap.put("id",it.getStringExtra("userID"));
                    hashmap.put("password",it.getStringExtra("pass"));
                    hashmap.put("clienttype","用户端");
                    hashmap.put("taskid","无人机放出货物");
                    hashmap.put("type","无人机放出货物");
                    hashmap.put("taskdate",content);
                    try {
                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                        Toast.makeText(PlaneActivity.this, isSend ? "success" : "fail",
                                Toast.LENGTH_SHORT).show();
                        if(isSend)
                        {
                            messagebtn_shouhuo.setClickable(false);
                            messagebtn_shouhuo.setVisibility(View.INVISIBLE);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }

            }
        });
        //开始进行放货人脸检测
        facebtn_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String,Object> hashmap=new HashMap<String,Object>();
                Intent it=getIntent();
                hashmap.put("id",it.getStringExtra("userID"));
                hashmap.put("password","hahaha");
                hashmap.put("clienttype","用户端");
                hashmap.put("taskid","扫描人脸");
                hashmap.put("type","扫描人脸");
                hashmap.put("taskdate",planeID);
                try {
                    boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                    Toast.makeText(PlaneActivity.this, isSend ? "请求扫描人脸成功" : "fail",
                            Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        //写布局的时候可以注释此行
        timer.schedule(task, 500, 1500); // 1s后执行task,经过1s再次执行

    }
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
            if(ishasticket)
            {
                HashMap<String,Object> hashmap=new HashMap<String,Object>();
                Intent it=getIntent();
                hashmap.put("id",it.getStringExtra("userID"));
                hashmap.put("password","hahaha");
                hashmap.put("clienttype","用户端");
                hashmap.put("taskid","查询订单");
                hashmap.put("type","查询订单");
                hashmap.put("taskdate",ticket_id);
                try {
                    boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                    Toast.makeText(PlaneActivity.this, isSend ? "success" : "fail",
                            Toast.LENGTH_SHORT).show();

                    progress.setCanceledOnTouchOutside(true);
                    progress.setMessage( "正在为您请求...");
                    progress.show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            else if(ishasplane)
            {
                HashMap<String,Object> hashmap=new HashMap<String,Object>();
                Intent it=getIntent();
                hashmap.put("id",it.getStringExtra("userID"));
                hashmap.put("password","hahaha");
                hashmap.put("clienttype","用户端");
                hashmap.put("taskid","查询无人机");
                hashmap.put("type","查询无人机");
                hashmap.put("taskdate",planeID);
                try {
                    boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            if(latitude!=0&&longitude!=0)
            {
                LatLng latLng=new LatLng(latitude,longitude);
                Log.i("progress","haha"+latLng.toString());
                if(marker!=null){
                    marker.remove();
                }
                MarkerOptions markerOption = new MarkerOptions();//创建marker设置对象
                markerOption.position(latLng);
                markerOption.draggable(true);
                markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.plane)));
                // 将Marker设置为贴地显示，可以双指下拉看效果
                // markerOption.setFlat(true);
                // markerOption.setInfoWindowOffset(0,-10);//设置弹出框的位置
                // markerOption.visible(true);
                marker = aMap.addMarker(markerOption);//添加图标
/**---------------------------------------------------------------------------------*/
                //用于marker的平滑移动
//                start=end=firstLatLng;
//                end=latLng;
//                firstLatLng=latLng;
//                smoothMarker.stopMove();
//                smoothMarker.destroy();
//                move(start,end,1000);
/**---------------------------------------------------------------------------------*/
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));
            }

            }
            super.handleMessage(msg);
        };
    };
    TimerTask task = new TimerTask() {

        @Override
        public void run() {
            // 需要做的事:发送消息
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }
    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    boolean isFirstLoc=true;/**---------/*/
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null&&amapLocation != null) {
            if (amapLocation != null
                    &&amapLocation.getErrorCode() == 0) {
//                mListener.onLocationChanged(amapLocation);/**---------/*/
                // 显示系统小蓝点
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                if (isFirstLoc) {
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(amapLocation);
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(amapLocation.getCountry() + ""
                            + amapLocation.getProvince() + ""
                            + amapLocation.getCity() + ""
                            + amapLocation.getProvince()
                            + amapLocation.getDistrict() + ""
                            + amapLocation.getStreet() + ""
                            + amapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                    /**---------------------------------------------------------------------------------*/
                    //记录第一次定位坐标；
                   // start=end=firstLatLng;
                   // move(start,end,1000);
                    /**---------------------------------------------------------------------------------*/
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }


    public void onStart() {
        super.onStart();
        mLocalBroadcastManager.registerReceiver(mReciver, mIntentFilter);
        bindService(mServiceIntent, conn, BIND_AUTO_CREATE);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.administrator.mygaodemap/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        ((Button)findViewById(R.id.message_fanghuo)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.bottomin));
        ((Button)findViewById(R.id.message_shouhuo)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.bottomin));
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(conn);
        mLocalBroadcastManager.unregisterReceiver(mReciver);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.administrator.mygaodemap/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    /**---------------------------------------------------------------------------------*/
    SmoothMarker smoothMarker;
    public void move(LatLng start,LatLng end,int duration) {
        List<LatLng> points = new ArrayList<LatLng>();//获取模拟点
        points.add(start);
        points.add(end);
        LatLngBounds bounds = new LatLngBounds(points.get(0), points.get(points.size() - 2));//坐标点的连线
        for (int i = 0 ; i < points.size(); i++) {
            bounds.including(points.get(i));
        }
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));//将线路展示在地图下

        //设置缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        //将地图移动到定位点

        smoothMarker = new SmoothMarker(aMap);//实例化
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.car));//获取移动时的marker图标

        LatLng drivePoint = points.get(0);//获取起始点坐标
        Pair<Integer, LatLng> pair = PointsUtil.calShortestDistancePoint(points, drivePoint);
        points.set(pair.first, drivePoint);
        final List<LatLng> subList = points.subList(pair.first, points.size());//从第一个点到最后一个点

        smoothMarker.setPoints(subList);
        smoothMarker.setTotalDuration(duration);

//        smoothMarker.setMoveListener(new SmoothMarker.SmoothMarkerMoveListener() {
//            @Override
//            public void move(final double distance) {
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        aMap.moveCamera(CameraUpdateFactory.changeLatLng(smoothMarker.getPosition()));
//                    }
//                });
//
//            }
//        });
        smoothMarker.startSmoothMove();

    }
    /**---------------------------------------------------------------------------------*/
    public void onBackPressed() {
        twoToExit++;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                twoToExit=0;
            }
        },2000);
        if (twoToExit==2){
            timer.cancel();
            finish();
        }else{
            Toast.makeText(getApplicationContext(),"真的要退出吗",Toast.LENGTH_SHORT).show();//时间有限制
        }
    }
}

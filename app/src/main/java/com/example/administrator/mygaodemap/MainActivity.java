package com.example.administrator.mygaodemap;

import android.animation.Animator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.example.administrator.mygaodemap.Operation.PointsUtil;
import com.example.administrator.mygaodemap.Operation.SmoothMarker;
import com.example.administrator.mygaodemap.util.Constant;
import com.example.administrator.mygaodemap.util.FaceTest;
import com.example.administrator.mygaodemap.util.PlaneActivity;
import com.example.administrator.mygaodemap.util.Point;
import com.example.administrator.mygaodemap.util.SocketService;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationSource,AMapLocationListener, AMap.OnMapClickListener{

    MapView mMapView = null;
    AMap aMap;
    OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;
    AMapLocationClientOption mLocationOption;

    private TextView query_ticket;

    private String user_;
    private String pass_;
    private GoogleApiClient client;

    private String token="";
    private String tokens="";

    private Button submit;
    private TextView face_storage;

    private Handler handler;
    private ProgressDialog progress;

    public boolean isFirst=true;

    String items[]=new String[2];

    double latitude;
    double longitude;
    public Point start_place=new Point();
    public Point end_palce=new Point();

    private IBackService iBackService;
    private  String path_;

    private String hour="";
    private String minute="";
    private EditText etStartTime;

    private String weight_;
    private String phone_;
    private String beizhu_;
    private String distance;
    private String money;
    private String time_;
    private String sender;
    private String reciever;
    private String isPersonal_;
    private boolean isDelete=false;
    int flag=0;
    //下拉列表框
    private static final String[] m={"个人","商户"};
    private Spinner is_personal;
    private TextView tvSpinner;


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
    private TextView mResultText;
    private TextView mResultText1;
    private Intent mServiceIntent;

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
                if (firstChar == '[') {
                    Intent it0=getIntent();
                    Intent ticket_it=new Intent();
                    ticket_it.putExtra("data",message);
                    ticket_it.putExtra("user",it0.getStringExtra("userID"));
                    ticket_it.putExtra("pass",it0.getStringExtra("userPassword"));
                    Log.i("progress",message);
                    ticket_it.setClass(MainActivity.this, TicketActivity.class);
                    startActivity(ticket_it);
                    progress.dismiss();
                }
                else if(firstChar=='{')
                {
                    try {

                        JSONObject json=new JSONObject(message);
                        if(json.getString("type").equals("获取人脸数据"))
                        {
                            final String url1="https://api-cn.faceplusplus.com/facepp/v3/faceset/removeface";
                            String f=json.getString("xinxi");
                            String a[]=f.split("###");
                            if(a[0].equals("null")||a[1].equals("null"))
                            {
                                Toast.makeText(MainActivity.this,"请先存照片",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            final HashMap<String, String> map= new HashMap<>();
                            map.put("api_key", Constant.Key);
                            map.put("api_secret", Constant.Secret);
                            map.put("outer_id",user_);
                            map.put("face_tokens",a[0]+","+a[1]);
                                try{
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                byte[] haha=FaceTest.createSet(url1,map);
                                                String hehe=new String(haha);
                                                Log.i("progress","删除集合"+hehe);

                                                final String addface_url=" https://api-cn.faceplusplus.com/facepp/v3/faceset/addface";
                                                final HashMap<String, String> map_addface=new HashMap<>();

                                                map_addface.put("api_key", Constant.Key);
                                                map_addface.put("api_secret", Constant.Secret);
                                                map_addface.put("outer_id",user_);
                                                map_addface.put("face_tokens",tokens);
                                                //增加faceset
                                                byte[] heng= FaceTest.createSet(addface_url, map_addface);
                                                String str1 = new String(heng);
                                                Log.i("progress","增加"+str1);

                                            }catch (Exception e)
                                            {
                                                e.printStackTrace();
                                            }

                                        }
                                    }).start();

                                }catch (Exception e) {
                                    e.printStackTrace();
                                }


                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        };

    }
    private MessageBackReciver mReciver;
    private IntentFilter mIntentFilter;

    private LocalBroadcastManager mLocalBroadcastManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress = new ProgressDialog(MainActivity.this);
        Intent it=getIntent();
        user_=it.getStringExtra("userID");
        pass_=it.getStringExtra("userPassword");
        mResultText1 = (TextView) findViewById(R.id.tv1);
        mResultText = (TextView) findViewById(R.id.resule_text);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mReciver = new MessageBackReciver(mResultText1);

        mServiceIntent = new Intent(this, SocketService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(SocketService.HEART_BEAT_ACTION);
        mIntentFilter.addAction(SocketService.MESSAGE_ACTION);

        items[0]="起点";
        items[1]="终点";
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        face_storage=(TextView) findViewById(R.id.face_storage);
        face_storage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file1 = new File(Environment.getExternalStorageDirectory()
                        + "/DCIM/Camera", String.valueOf(System.currentTimeMillis())
                        + ".jpg");
                path_ = file1.getPath();

                Uri imageUri = Uri.fromFile(file1 );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, 3);
            }
        });

        query_ticket=(TextView)findViewById(R.id.query_ticket);
        query_ticket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String,Object> hashmap=new HashMap<String,Object>();
                String content ="";
                Intent it=getIntent();
                hashmap.put("id",user_);
                hashmap.put("password",pass_);
                hashmap.put("clienttype","用户端");
                hashmap.put("taskid","查询订单");
                hashmap.put("type","查询订单");
                hashmap.put("taskdate",content);
                try {
                    boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                    Toast.makeText(MainActivity.this, isSend ? "success" : "fail",
                            Toast.LENGTH_SHORT).show();
                    progress.setCanceledOnTouchOutside(true);
                    progress.setMessage( "正在为您加载订单...");
                    progress.show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });


        submit=(Button)findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ticketDialog();
            }
        });

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
            aMap.setOnMapClickListener(this);
        }
        // 设置定位监听

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);

            /**----------------*********/
        //这是单次定位的
            mLocationOption.setOnceLocation(true);
            mLocationOption.setOnceLocationLatest(true);

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


    boolean isFirstLoc=true;//应该在外面吧
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        Log.d("xyx","DSADSAFDAF");
        if (mListener != null&&amapLocation != null) {
            if (amapLocation != null
                    &&amapLocation.getErrorCode() == 0) {
                Log.d("xyx","asas");
//                mListener.onLocationChanged(amapLocation);
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
                    isFirstLoc = false;
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
            }
        }
    }

    protected void dialog() {
        new AlertDialog.Builder(this)
                .setTitle("将此处设置为")
                .setIcon(R.drawable.ic_pin)
                .setSingleChoiceItems(items, 0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if(items[which]=="起点")
                                {
                                    start_place.latitude=latitude;
                                    start_place.longitude=longitude;
                                    ((TextView)findViewById(R.id.resule_text)).setText("第二步：确认您的订单");
                                }

                                if(items[which]=="终点")
                                {
                                    end_palce.latitude=latitude;
                                    end_palce.longitude=longitude;
                                    ((TextView)findViewById(R.id.resule_text)).setText("第二步：确认您的订单");
                                }
                                LatLng start=new LatLng(start_place.latitude,start_place.longitude);
                                LatLng end=new LatLng(end_palce.latitude,end_palce.longitude);

                                DecimalFormat decimalFormat = new DecimalFormat("0.0");
                                double di=AMapUtils.calculateLineDistance(start,end);
                                double man=di*0.5;
                                distance= decimalFormat.format(di)+"";
                                money=decimalFormat.format(man)+"";

                                dialog.dismiss();
                            }
                        }).show();
    }

    protected void ticketDialog(){
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        ArrayAdapter<String> adapter;
        Window win = dialog.getWindow();
        win.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        win.setAttributes(lp);

        View view=LayoutInflater.from(this).inflate(R.layout.layout_ticket, null);

        dialog.setView(view);
        dialog.show();
        final EditText weight=(EditText)dialog.findViewById(R.id.weight);

        final EditText beizhu=(EditText)dialog.findViewById(R.id.beizhu);

        final EditText phone=(EditText)dialog.findViewById(R.id.telephone);

        final EditText senderEdit=(EditText)dialog.findViewById(R.id.sender);

        final EditText recieverEdit=(EditText)dialog.findViewById(R.id.reciver);

        is_personal=(Spinner)dialog.findViewById(R.id.Spi_ispersonal);
        tvSpinner=(TextView)dialog.findViewById(R.id.spinnerText);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,m);

        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //将adapter 添加到spinner中
        is_personal.setAdapter(adapter);

        //添加事件Spinner事件监听
        is_personal.setOnItemSelectedListener(new SpinnerSelectedListener());

        //设置默认值
        is_personal.setVisibility(View.VISIBLE);


        etStartTime=(EditText)dialog.findViewById(R.id.time_);

        etStartTime.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(dialog.getContext());


                    View view1= LayoutInflater.from(MainActivity.this).inflate(R.layout.time_dialog,null);
                    builder.setView(view1);
                    final TimePicker timePicker1= (TimePicker) view1.findViewById(R.id.time_picker);
                    timePicker1.setIs24HourView(true);
                    resizePikcer(timePicker1);
                    builder.setPositiveButton("确  定", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            //因为不能时间选择一开始是当前时间/*---------------------------------------------------------------------*/
                            hour=timePicker1.getCurrentHour()+"";
                            minute=timePicker1.getCurrentMinute()+ "";

                            StringBuffer sb = new StringBuffer();
                            sb.append(hour)
                                    .append(":").append(minute);

                            etStartTime.setText(sb);

                            dialog.cancel();
                            }
                        });
                    Dialog dialog = builder.create();
                    dialog.show();
                }
                return true;
            }
        });
        Button btnPositive = (Button) dialog.findViewById(R.id.sure_confirm);
        Button btnNegative = (Button) dialog.findViewById(R.id.cancel_confirm);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if((start_place.latitude!=0&&start_place.longitude!=0)||(end_palce.latitude!=0&&end_palce.longitude!=0)) {
                    weight_=weight.getText().toString();
                    beizhu_=beizhu.getText().toString();
                    phone_=phone.getText().toString();
                    time_=etStartTime.getText().toString();
                    sender=senderEdit.getText().toString();
                    reciever=recieverEdit.getText().toString();
                    isPersonal_=tvSpinner.getText().toString();
                    ticketDetailDialog();
                }
                else
                    Toast.makeText(getApplicationContext(), "请先设置起终点", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
            }
        });
        btnNegative.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
    }

    protected void ticketDetailDialog()
    {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(LayoutInflater.from(this).inflate(R.layout.layout_ticket_detail, null));
        dialog.show();
        final TextView tvUser=(TextView)dialog.findViewById(R.id.user);
        final TextView tvWeight=(TextView)dialog.findViewById(R.id.the_weight);
        final TextView tvTime=(TextView)dialog.findViewById(R.id.the_time);
        final TextView tvPhone=(TextView)dialog.findViewById(R.id.the_telephone);
        final TextView tvDistance=(TextView)dialog.findViewById(R.id.the_distance);
        final TextView tvMoney=(TextView)dialog.findViewById(R.id.the_money);
        final TextView tvBeizhu=(TextView)dialog.findViewById(R.id.the_beizhu);
        final TextView tvsender=(TextView)dialog.findViewById(R.id.sender_user);
        final TextView tvrecieve=(TextView)dialog.findViewById(R.id.reciver_user);
        final TextView tvispersonal=(TextView)dialog.findViewById(R.id.isPersonl);

        tvUser.setText(user_);
        tvWeight.setText(weight_);
        tvTime.setText(time_);
        tvPhone.setText(phone_);
        tvDistance.setText(distance);
        tvMoney.setText(money);
        tvBeizhu.setText(beizhu_);
        tvsender.setText(sender);
        tvrecieve.setText(reciever);
        tvispersonal.setText(isPersonal_);

        Button btnPositive = (Button) dialog.findViewById(R.id.sure);
        Button btnNegative = (Button) dialog.findViewById(R.id.cancel);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                    HashMap<String,Object> hashmap=new HashMap<String,Object>();
                    String content =time_+";"
                            +weight_+";"
                            +money+";"
                            +distance+";"
                            +start_place.latitude + "," + start_place.longitude+";" + end_palce.latitude + "," + end_palce.longitude+";"
                            +beizhu_+";"
                            + phone_+";"+sender+";"+reciever+";"+isPersonal_;
                    hashmap.put("id",tvUser.getText().toString());
                    hashmap.put("password",pass_);
                    hashmap.put("clienttype","用户端");
                    hashmap.put("taskid","生成订单");
                    hashmap.put("type","生成订单");
                    hashmap.put("taskdate",content);
                    try {
                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                        Log.i("progress",(new Gson()).toJson(hashmap));
                        Toast.makeText(MainActivity.this, isSend ? "success" : "fail",
                                Toast.LENGTH_SHORT).show();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                Intent it=new Intent();
                it.putExtra("userID",tvUser.getText().toString());
                it.putExtra("pass",pass_);
                it.putExtra("start_lati",start_place.latitude+"");
                it.putExtra("start_long",start_place.longitude+"");
                it.putExtra("end_lati",end_palce.latitude+"");
                it.putExtra("end_long",end_palce.longitude+"");
                if(user_.equals(sender))
                {
                    it.putExtra("whosender","yes");
                }
                if(user_.equals(reciever))
                {
                    it.putExtra("whoreciever","yes");
                }
                it.setClass(MainActivity.this, PlaneActivity.class);
                startActivity(it);
                dialog.dismiss();
            }
        });
        btnNegative.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                ticketDialog();

                dialog.dismiss();
            }
        });
    }

    //地图点击事件
    @Override
    public void onMapClick(LatLng latLng) {
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        dialog();
        MarkerOptions otMarkerOptions = new MarkerOptions();
        otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding));
        otMarkerOptions.position(latLng);
        aMap.addMarker(otMarkerOptions);
        aMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == RESULT_OK ) {

            //存脸
            File file = new File(path_);
            byte[] buff = FaceTest.getBytesFromFile(file);
            final String url = "https://api-cn.faceplusplus.com/facepp/v3/detect";


            final HashMap<String, String> map = new HashMap<>();
            final HashMap<String, byte[]> byteMap = new HashMap<>();
            map.put("api_key", Constant.Key);
            map.put("api_secret", Constant.Secret);
            byteMap.put("image_file", buff);
            try{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            byte[] bacd = FaceTest.post(url, map, byteMap);
                            String str = new String(bacd);
                            Log.i("progress",str);
                                String h=LoginActivity.getFaceToken(str);
                                if(h.equals("error"))
                                {
                                    Looper.prepare();
                                    Toast.makeText(MainActivity.this, "获取人脸失败，请重新拍照",
                                            Toast.LENGTH_SHORT).show();
                                    Looper.loop();
                                    return;
                                }
                                flag=flag+1;
                                if(flag==1)
                                {
                                    token=token+h+"###";
                                    tokens=tokens+h+",";
                                    Looper.prepare();
                                    Toast.makeText(MainActivity.this, "请再次拍照",
                                            Toast.LENGTH_SHORT).show();
                                    Looper.loop();
                                }
                                else if(flag==2)
                                {
                                    token=token+h;
                                    tokens=tokens+h;


                                    //获取数据库已有信息
                                    HashMap<String,Object> hashmap1=new HashMap<String,Object>();
                                    String content1 ="";
                                    hashmap1.put("id",user_);
                                    hashmap1.put("password",pass_);
                                    hashmap1.put("clienttype","用户端");
                                    hashmap1.put("taskid","获取人脸数据");
                                    hashmap1.put("type","获取人脸数据");
                                    hashmap1.put("taskdate",content1);
                                    try {
                                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap1));//Send Content by socket

                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                    //发送数据
                                    HashMap<String,Object> hashmap=new HashMap<>();
                                    String content =token;
                                    hashmap.put("id",user_);
                                    hashmap.put("password",pass_);
                                    hashmap.put("clienttype","用户端");
                                    hashmap.put("taskid","存储人脸数据");
                                    hashmap.put("type","存储人脸数据");
                                    hashmap.put("taskdate",content);
                                    flag=0;
                                    try {
                                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                                        Looper.prepare();
                                        Toast.makeText(MainActivity.this, isSend ? "照片存储成功" : "fail",
                                                Toast.LENGTH_SHORT).show();
                                        Looper.loop();

                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }

                                }
                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        mlocationClient.onDestroy();//销毁定位客户端。
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        //创建faceSet
        final String url1 = "https://api-cn.faceplusplus.com/facepp/v3/faceset/create";
        final HashMap<String, String> map1 = new HashMap<>();
        map1.put("api_key", Constant.Key);
        map1.put("api_secret", Constant.Secret);
        map1.put("outer_id",user_);
        try{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] bacd1 = FaceTest.createSet(url1, map1);
                        String str1= new String(bacd1);
                        Log.i("progress","人脸集合"+str1);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }).start();
        }catch (Exception e) {
            e.printStackTrace();
        }
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

        ((Button)findViewById(R.id.submit)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.bottomin));
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

    private List<NumberPicker> findNumberPicker(ViewGroup viewGroup){
        List<NumberPicker> npList = new ArrayList<NumberPicker>();
        View child = null;
        if(null != viewGroup){
            for(int i = 0;i<viewGroup.getChildCount();i++){
                child = viewGroup.getChildAt(i);
                if(child instanceof NumberPicker){
                    npList.add((NumberPicker)child);
                }
                else if(child instanceof LinearLayout){
                    List<NumberPicker> result = findNumberPicker((ViewGroup)child);
                    if(result.size()>0){
                        return result;
                    }
                }
            }
        }
        return npList;
    }

    /*
 * 调整numberpicker大小
 */
    private void resizeNumberPicker(NumberPicker np){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 0, 10, 0);
        np.setLayoutParams(params);
    }

    private void resizePikcer(FrameLayout tp){
        List<NumberPicker> npList = findNumberPicker(tp);
        for(NumberPicker np:npList){
            resizeNumberPicker(np);
        }
    }
/*--------------------------------------*/
    private int twoToExit=0;
    @Override
    public void onBackPressed() {
        twoToExit++;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                twoToExit=0;
            }
        },2000);
        if (twoToExit==2){
            super.onBackPressed();
        }else{
            Toast.makeText(getApplicationContext(),"真的要退出吗",Toast.LENGTH_SHORT).show();//时间有限制
        }
    }

    //使用数组形式操作
    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            tvSpinner.setText(m[arg2]);
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

}

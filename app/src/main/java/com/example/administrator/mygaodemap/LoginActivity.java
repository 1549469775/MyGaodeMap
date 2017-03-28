package com.example.administrator.mygaodemap;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.mygaodemap.util.Constant;
import com.example.administrator.mygaodemap.util.FaceTest;
import com.example.administrator.mygaodemap.util.SocketService;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Random;

public class LoginActivity extends Activity {
    Button login;
    EditText userID;
    ImageView camera;
    EditText userPassword;
    String path;

    int flag=0;
    double issame=0;
    //人脸识别数据
    String h;

    private IBackService iBackService;
    private GoogleApiClient client;

    //服务
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
    private Intent mServiceIntent;

    //广播
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
                        if(json.getString("type").equals("登陆")&&json.getString("xinxi").equals("登入成功"))
                        {
                                Log.i("progress",json.getString("xinxi"));
                                Intent it=new Intent(LoginActivity.this,MainActivity.class);
                                it.putExtra("userID",userID.getText().toString());
                                it.putExtra("userPassword",userPassword.getText().toString());
                                startActivity(it);
                                 Log.i("progress",json.getString("xinxi"));
                                finish();
                        }else if(json.getString("type").equals("获取人脸数据"))
                        {
                            final String url1="https://api-cn.faceplusplus.com/facepp/v3/compare";
                            String f=json.getString("xinxi");
                            String a[]=f.split("###");
                            if(a[0].equals("null")||a[1].equals("null"))
                            {
                                Toast.makeText(LoginActivity.this,"请先登录后按提示存照片",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            final HashMap<String, String> map= new HashMap<>();
                             final HashMap<String, String> map1 = new HashMap<>();
                             map.put("api_key", Constant.Key);
                             map.put("api_secret", Constant.Secret);
                            map1.put("face_token1",h);
                            for(int i=0;i<=1;i++)
                            {
                                map1.put("face_token2",a[i]);

                                try{
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                byte[] haha=FaceTest.compare(url1,map,map1);
                                                String hehe=new String(haha);
                                                Log.i("progress",hehe);
                                                JSONObject enen=new JSONObject(hehe);

                                                if(enen.has("error_message"))
                                                {
                                                    Looper.prepare();
                                                    Toast.makeText(LoginActivity.this,"人脸比对失败,请对准人脸",
                                                            Toast.LENGTH_SHORT).show();
                                                    Looper.loop();
                                                    return;
                                                }
                                                else
                                                {
                                                    issame+=Double.parseDouble(enen.getString("confidence"));
                                                    Log.i("progress",issame+"");
                                                    if(flag==1)
                                                    {
                                                        if(issame/2>85)
                                                        {
                                                            issame=0;
                                                            flag=0;
                                                            Intent it=new Intent(LoginActivity.this,MainActivity.class);
                                                            it.putExtra("userID",userID.getText().toString());
                                                            it.putExtra("userPassword",userPassword.getText().toString());
                                                            startActivity(it);
                                                            Looper.prepare();
                                                            Toast.makeText(LoginActivity.this,"登录成功",
                                                                    Toast.LENGTH_SHORT).show();
                                                            Looper.loop();
                                                        }
                                                        else
                                                        {
                                                            issame=0;
                                                            flag=0;
                                                            Looper.prepare();
                                                            Toast.makeText(LoginActivity.this,"人脸比对失败,请重新比对",
                                                                    Toast.LENGTH_SHORT).show();
                                                            Looper.loop();

                                                            return;
                                                        }

                                                    }
                                                    else
                                                        flag++;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        login=(Button)findViewById(R.id.login_confirm);
        userID=(EditText) findViewById(R.id.login_username);
        userPassword=(EditText) findViewById(R.id.login_passowrd);
        camera=(ImageView)findViewById((R.id.facecamera));

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mReciver = new MessageBackReciver(mResultText);

        mServiceIntent = new Intent(this, SocketService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(SocketService.HEART_BEAT_ACTION);
        mIntentFilter.addAction(SocketService.MESSAGE_ACTION);

        //调用系统相机拍照进行人脸识别
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file1 = new File(Environment.getExternalStorageDirectory()
                        + "/DCIM/Camera", String.valueOf(System.currentTimeMillis())
                        + ".jpg");
                path = file1.getPath();

                Uri imageUri = Uri.fromFile(file1 );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, 3);
                }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String,Object> hashmap=new HashMap<String,Object>();
                String content ="";
                hashmap.put("id",userID.getText().toString());
                hashmap.put("password",userPassword.getText().toString());
                hashmap.put("clienttype","用户端");
                hashmap.put("taskid","登陆");
                hashmap.put("type","登陆");
                hashmap.put("taskdate",content);
                try {
                    /*-------------------*/
                    Intent it=new Intent(LoginActivity.this,MainActivity.class);
                    it.putExtra("userID",userID.getText().toString());
                    it.putExtra("userPassword",userPassword.getText().toString());
                    startActivity(it);
                    boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                    Toast.makeText(LoginActivity.this, isSend ? "发送成功" : "fail",
                            Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });



        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == RESULT_OK ) {

//获取图像关键点
            File file = new File(path);
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
                            if(userID.getText().toString().equals(""))
                            {
                                Looper.prepare();
                                Toast.makeText(LoginActivity.this,"请填写您的账号",
                                        Toast.LENGTH_SHORT).show();
                                Looper.loop();
                                return;
                            }
                            byte[] bacd = FaceTest.post(url, map, byteMap);
                            String str = new String(bacd);
                            h=getFaceToken(str);
                            Log.i("progress",h);
                            HashMap<String,Object> hashmap=new HashMap<String,Object>();
                            String content ="";

                            hashmap.put("id",userID.getText().toString());
                            hashmap.put("password",userPassword.getText().toString());
                            hashmap.put("clienttype","用户端");
                            hashmap.put("taskid","获取人脸数据");
                            hashmap.put("type","获取人脸数据");
                            hashmap.put("taskdate",content);
                            try {
                                boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket

                            } catch (RemoteException e) {
                                e.printStackTrace();
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
    public static String getFaceToken(String jsonStr) {
        String facetoken="error";
        try
        {
            JSONObject json=new JSONObject(jsonStr);
            String faces=json.getString("faces");
            JsonParser parser = new JsonParser();
            JsonArray jsonArray=parser.parse(faces).getAsJsonArray();
            JsonElement el = jsonArray.get(0);
            JSONObject json1=new JSONObject(el.toString());
            if(json1.has("face_token")) {
                facetoken = json1.getString("face_token");
            }

        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return facetoken;
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

//        ((LinearLayout)findViewById(R.id.pwd_re)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.alpha));
        ((Button)findViewById(R.id.login_confirm)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.leftin));
        ((Button)findViewById(R.id.register_confirm)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.rightin));
        ((LinearLayout)findViewById(R.id.login_phone_ly)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.alpha));
        ((LinearLayout)findViewById(R.id.login_password_ly)).startAnimation(AnimationUtils.loadAnimation(this,R.anim.alpha));
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


}

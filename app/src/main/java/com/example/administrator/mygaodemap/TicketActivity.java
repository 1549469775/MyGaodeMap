package com.example.administrator.mygaodemap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.mygaodemap.util.SocketService;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;


import com.example.administrator.mygaodemap.util.PlaneActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TicketActivity extends Activity {

    private PullToRefreshListView listView;
    private IBackService iBackService;
    private GoogleApiClient client;
    //冗余代码
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
    private boolean isrecieve=false;
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
                Log.i("progress","heheda1"+message);
                char[] strChar = message.substring(0, 1).toCharArray();
                char firstChar = strChar[0];
                if (firstChar == '[') {
                    Log.i("progress","memeda"+message);
                    initListView(message);
                    // Call onRefreshComplete when the list has been refreshed.
                    listView.onRefreshComplete();

                    task.cancel();
                    timer.purge();
                    timer.cancel();

                }
            }
        };

    }
    private MessageBackReciver mReciver;
    private IntentFilter mIntentFilter;

    private LocalBroadcastManager mLocalBroadcastManager;

    Timer timer;
    TimerTask task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);
        listView=(PullToRefreshListView)findViewById(R.id.ticket_listview);
        Intent it=getIntent();
        String jsonStr=it.getStringExtra("data").trim();

        initListView(jsonStr);
        mResultText1 = (TextView) findViewById(R.id.tv2);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReciver = new MessageBackReciver(mResultText1);
        mServiceIntent = new Intent(this, SocketService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(SocketService.HEART_BEAT_ACTION);
        mIntentFilter.addAction(SocketService.MESSAGE_ACTION);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void initListView(String str) {
        Log.i("progress","haha"+str);
        JsonParser parser = new JsonParser();
        JsonArray jsonArray=parser.parse(str).getAsJsonArray();
        final List<Map<String, Object>> result ;
        result=getArraydata(jsonArray);
        SimpleAdapter adapter = new SimpleAdapter(this, result,
                R.layout.layout_ticket_item, new String[] { "ticket_id","planeID","ticket_create_time","hope_startTime","real_startTime"
        ,"real_endTime","consuming_time","weight","money","distance","departure","destination","taskdate","remarks","status","phoneNumber"
                , "senderID"
        ,"recieverID"},
                new int[] {R.id.ticket_id,R.id.ticket_plane,R.id.ticket_create_time,R.id.ticket_hopetime,R.id.ticket_realstartTime,R.id.ticket_realendtime
                ,R.id.ticket_consumTime,R.id.ticket_weight,R.id.ticket_price,R.id.ticket_distance,R.id.ticket_start,R.id.ticket_end,R.id.ticket_task,R.id.ticket_remark,
                R.id.ticket_status,R.id.ticket_phone,R.id.ticket_sender,R.id.ticket_reciever}){
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                final int i=position;
                final View view=super.getView(position, convertView, parent);
                final Button address=(Button)view.findViewById(R.id.btn_address);
                if(result.get(position).get("status").toString().trim().equals("配送中")) {
                    address.setText("跟踪");
                    address.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent it0=getIntent();
                            Intent it=new Intent();
                            it.putExtra("PlaneID",result.get(position).get("planeID").toString().trim());
                            it.putExtra("status",result.get(position).get("status").toString().trim());

                            String start=result.get(position).get("departure").toString().trim();
                            String[] a=start.split(",");
                            it.putExtra("start_lati",a[0]);
                            it.putExtra("start_long",a[1]);
                            String end=result.get(position).get("destination").toString().trim();
                            String[] b=end.split(",");
                            it.putExtra("end_lati",b[0]);
                            it.putExtra("end_long",b[1]);

                            if(result.get(position).get("senderID").equals(it0.getStringExtra("user")))
                            {
                                it.putExtra("whosender","yes");
                            }
                            if(result.get(position).get("recieverID").equals(it0.getStringExtra("user")))
                            {
                                it.putExtra("whoreciever","yes");
                            }
                            it.putExtra("userID",it0.getStringExtra("user"));
                            it.putExtra("password",it0.getStringExtra("pass"));
                            it.putExtra("ticket_id",result.get(position).get("ticket_id").toString().trim());
                            it.setClass(TicketActivity.this, PlaneActivity.class);
                            startActivity(it);
                        }
                    });
                }
                else if(result.get(position).get("status").toString().trim().equals("准备中"))
                {
                    address.setText("等待");
                    address.setClickable(false);
                }
                else if(result.get(position).get("status").toString().trim().equals("完成")) {
                    address.setText("评价");
                    address.setClickable(false);
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>()
                {
                    @Override
                    public void onRefresh(
                            PullToRefreshBase<ListView> refreshView)
                    {
                        String label = DateUtils.formatDateTime(
                                getApplicationContext(),
                                System.currentTimeMillis(),
                                DateUtils.FORMAT_SHOW_TIME
                                        | DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_ABBREV_ALL);
                        // 显示最后更新的时间
                        refreshView.getLoadingLayoutProxy()
                                .setLastUpdatedLabel(label);
                        // 模拟加载任务
                        //new GetDataTask().execute();
                        timer = new Timer();
                        task = new TimerTask() {

                            @Override
                            public void run() {
                                // 需要做的事:发送消息
                                Message message = new Message();
                                message.what = 1;
                                handler.sendMessage(message);
                            }
                        };

                        timer.schedule(task,500);
                    }
                });
    }
    public static List<Map<String, Object>> getArraydata(JsonArray json)
    {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            for(JsonElement obj:json)
            {
                Map<String, Object> map = new HashMap<String, Object>();
                String[] a=obj.toString().split("###");

                map.put("command",a[1]);
                    String str=a[2];

                    String[] b =str.split("\\+++");
                    for(int i=0;i<b.length;i++)
                    {
                        Log.i("progress",i+":"+b[i]);
                    }
                    map.put("ticket_id",b[0]);
                    map.put("planeID",b[1]);
                    map.put("userID",b[2]);
                    map.put("ticket_create_time",b[3]);
                    map.put("hope_startTime",b[4]);
                    map.put("real_startTime",b[5]);
                    map.put("real_endTime",b[6]);
                    map.put("consuming_time",b[7]);
                    map.put("weight",b[8]);
                    map.put("money",b[9]);
                    map.put("distance",b[10]);
                    map.put("departure",b[11]);
                    map.put("destination",b[12]);
                    map.put("taskdate",b[13]);
                    map.put("remarks",b[14]);
                    map.put("status",b[15]);
                    map.put("phoneNumber",b[16]);
                    map.put("senderID",b[17]);
                    String[] s=b[18].split("\"");
                    map.put("recieverID",s[0]);
                    result.add(map);
                }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
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
    }

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

    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if(isrecieve==false)
                {
                    HashMap<String,Object> hashmap=new HashMap<String,Object>();
                    String content ="";
                    Intent it=getIntent();
                    hashmap.put("id",it.getStringExtra("user"));
                    hashmap.put("password",it.getStringExtra("pass"));
                    hashmap.put("clienttype","用户端");
                    hashmap.put("taskid","查询订单");
                    hashmap.put("type","查询订单");
                    hashmap.put("taskdate",content);
                    try {
                        boolean isSend = iBackService.sendMessage((new Gson()).toJson(hashmap));//Send Content by socket
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    };

}

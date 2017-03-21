package com.example.administrator.mygaodemap.util;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.administrator.mygaodemap.IBackService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

public class SocketService extends Service {
    private static final String TAG = "BackService";
    private static final long HEART_BEAT_RATE = 30 * 1000;

    public static final String HOST = "192.168.1.121";    //服务器IP
    public static final int PORT = 6000;     //IP端口

    public static final String MESSAGE_ACTION="com.dingmore.terminal.socket";
    public static final String HEART_BEAT_ACTION="com.dingmore.terminal.socket.heart";

    public static final String HEART_BEAT_STRING="00";//心跳包内容

    public String jsonStr;

    private ReadThread mReadThread;

    private LocalBroadcastManager mLocalBroadcastManager;

    private WeakReference<Socket> mSocket;

    // For heart Beat
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {

        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                boolean isSuccess = sendMsg(HEART_BEAT_STRING);//就发送一个HEART_BEAT_STRING过去 如果发送失败，就重新初始化一个socket
                if (!isSuccess) {
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    private long sendTime = 0L;
    private IBackService.Stub iBackService = new IBackService.Stub() {

        @Override
        public boolean sendMessage(String message) throws RemoteException {
            return sendMsg(message);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return iBackService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new InitSocketThread().start();
        mLocalBroadcastManager=LocalBroadcastManager.getInstance(this);

    }
    public boolean sendMsg(String msg) {
        if (null == mSocket || null == mSocket.get()) {
            return false;
        }
        Socket soc = mSocket.get();
        try {
            if (!soc.isClosed() && !soc.isOutputShutdown()) {
                long b=0;
                byte b1[]=new byte[2];
                //将字符串按：UTF-8字节流方式传输。先传输长度，再传输字节内容。
                DataOutputStream dos=new DataOutputStream(soc.getOutputStream());
                String message = msg;
                b=message.getBytes().length+longToByte(b).length+b1.length;
                dos.writeLong(b);//传输长度
                dos.write(b1);//两个空字节，只有这样服务器才能解析
                dos.writeUTF(message);
                dos.flush();
                sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void initSocket() {//初始化Socket
        try {
            Socket so = new Socket(HOST, PORT);
            mSocket = new WeakReference<Socket>(so);
            mReadThread = new ReadThread(so);
            mReadThread.start();
            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//初始化成功后，就准备发送心跳包
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseLastSocket(WeakReference<Socket> mSocket) {
        try {
            if (null != mSocket) {
                Socket sk = mSocket.get();
                if (!sk.isClosed()) {
                    sk.close();
                }
                sk = null;
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            initSocket();
        }
    }

    // Thread to read content from Socket
    class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @Override
        public void run() {
            super.run();
            Socket socket = mWeakSocket.get();
            if (null != socket) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length = 0;

                    while (!socket.isClosed() && !socket.isInputShutdown()
                            && isStart && ((length = is.read(buffer)) != -1)) {
                        ByteArrayOutputStream baos=new ByteArrayOutputStream();
                        if (length > 0) {
                            //以输入流的形式返回
                            //将输入流转换成字符串
                            baos.write(buffer, 12, length-12);
                            Log.i("progress",baos.size()+"");
                            buffer=new byte[1024 * 4];
                        }
                        String jsonString=baos.toString();
                        baos.flush();
                        baos.close();
                        //转换成json数据处理
                        char[] strChar = jsonString.substring(0, 1).toCharArray();
                        char firstChar = strChar[0];


                        if (firstChar == '{')
                        {
                            jsonStr = jsonString.substring(jsonString.indexOf("{"));
                            try {
                              //  JSONObject json=new JSONObject(jsonStr);
                                //getJsondata(json);
                            }catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                            if(jsonString.equals(HEART_BEAT_STRING)){//处理心跳回复
                                Intent intent=new Intent(HEART_BEAT_ACTION);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            }else{
                                //其他消息回复
                                Intent intent=new Intent(MESSAGE_ACTION);
                                intent.putExtra("message", jsonString);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            }

                        }
                        else if (firstChar == '[')
                        {
                            jsonStr = jsonString.substring(jsonString.indexOf("["));
                            //这段是解析jsonarray的，我后来放到相应界面中了
                            //JsonParser parser = new JsonParser();
                            //JsonArray jsonArray=parser.parse(jsonStr).getAsJsonArray();
                           // Log.i("progress",jsonArray.toString());
                           // result=getArraydata(jsonArray);
                            if(jsonString.equals(HEART_BEAT_STRING)){//处理心跳回复
                                Intent intent=new Intent(HEART_BEAT_ACTION);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            }else{
                                //其他消息回复
                                Intent intent=new Intent(MESSAGE_ACTION);
                                intent.putExtra("message", jsonString);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            }
                        }
                        else
                        {
                        }

                        try {

                        }catch (Exception e) {
                            e.printStackTrace();
                        }



                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static byte[] longToByte(long l) {
        byte[] byt;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeLong(l);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byt = baos.toByteArray();
        return byt;
    }

    public final static long getLong(byte[] buf) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }
        if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        }
        long r = 0;
        for (int i = 0; i < buf.length; i++) {
            r <<= 8;
            r |= (buf[i] & 0x00000000000000ff);
        }
        return r;
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
                if(a[1]=="查询订单")
                {
                    String str=a[2];
                    String[] b =str.split("\\+++");
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
                    map.put("phoneNumber",b[15]);
                    result.add(map);
                }
               if(a[1]=="")
               {

               }
                //map.put("")
            }

        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }
    public static List<Map<String, Object>> getJsondata(JSONObject json)
    {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {

        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }
}

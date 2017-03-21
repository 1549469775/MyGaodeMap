package com.example.administrator.mygaodemap.util;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.mygaodemap.MainActivity;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MyThread extends Thread{
    Socket s;
    MainActivity ctx;
    Handler hd;
    boolean asc=false;
    private static ByteBuffer buffer = ByteBuffer.allocate(8);
    public MyThread(MainActivity ctx, Handler hd){
        this.ctx = ctx;
        this.hd=hd;
    }
    public void run(){

        ctx.isFirst=false;
        //向远方发起TCP连接
        try {
           // s=new Socket(ctx.ip,ctx.port);
            //获取要发送的字符串
            String data;
            long b=0;

            data=ctx.start_place.latitude+","+ctx.start_place.longitude+","+ctx.end_palce.latitude+","+ctx.end_palce.longitude;
            byte b1[]=new byte[2];
            HashMap<String,Object> hashmap=new HashMap<String,Object>();
            hashmap.put("id","123");
            hashmap.put("password","hahaha");
            hashmap.put("clienttype","用户端");
            hashmap.put("taskid","生成订单");
            hashmap.put("type","生成订单");
            hashmap.put("taskdate",data);
            b=(new Gson()).toJson(hashmap).getBytes().length+longToByte(b).length+b1.length;
            //将字符串按：UTF-8字节流方式传输。先传输长度，再传输字节内容。
            DataOutputStream dos=new DataOutputStream(s.getOutputStream());
            //dos.write(longToByte(b));
            dos.writeLong(b);
            dos.write(b1);
            dos.writeUTF((new Gson()).toJson(hashmap));
            dos.flush();
            dos.close();
            dos.close();
            hd.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    Toast.makeText(ctx, "发送成功", Toast.LENGTH_LONG).show();
                }
            });
            //接收数据
//            InputStream inputStream = s.getInputStream();
//            DataInputStream input = new DataInputStream(inputStream);
//            byte[] h = new byte[10000];
//            byte[] w=new byte[8];
//            while(true)
//            {
//
//                int length = input.read(h);
//                int j=0;
//                for(int i=0;i<8;i++) {
//                    w[j] = h[i];
//                    Log.i("progress",Byte.toString(h[i]));
//                    j++;
//                }
//                Log.i("progress",getLong(w)+"");
//                String Msg = new String(h, 12, length-12, "utf-8");
//                Log.i("progress",Msg);
//            }

        } catch (Exception e1){
            // TODO Auto-generated catch block
            e1.printStackTrace();
            //hd.post(new Runnable() {

                //@Override
                //public void run() {
                    // TODO Auto-generated method stub
                    //Toast.makeText(ctx, "发送失败", Toast.LENGTH_LONG).show();
               // }
           // });

        }//try




    }//run
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

    public byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
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

}//MyThread

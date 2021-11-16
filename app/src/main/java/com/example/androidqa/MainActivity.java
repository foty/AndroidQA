package com.example.androidqa;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        @SuppressLint("HandlerLeak") Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                int i = -1;
                if (msg.obj instanceof Integer) {
                    i = (int) msg.obj;
                }
                Log.d("lxx", "收到事件消息：msg.obj= " + i);
            }
        };

       View view =  findViewById(R.id.tvHello);
       Log.d("lxx", "view= "+ view);
       while (view != null) {
           view = (View) view.getParent();
          Log.d("lxx", "parent= "+ view);
       }


        //testHandler(handler);
        TextView tv = new TextView(this);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(tv,"translationX",0,100).setDuration(1 * 1000);
        objectAnimator.start();

    }

    private void testHandler(Handler handler) {
        Message msg = new Message();
        msg.obj = 100;
        handler.sendMessage(msg);

        Message message1 = Message.obtain();
        handler.sendMessage(message1);

        //发送同步屏障
        //sendSyncBarrier(handler);

        //发送2个异步消息
        Message m1 = Message.obtain();
        m1.obj = 200;
//        m1.setAsynchronous(true);
        handler.sendMessage(m1);

        //同步消息
        Message m3 = Message.obtain();
        m3.obj = 300;
        handler.sendMessage(m3);

        //异步消息
        Message m2 = Message.obtain();
        m2.obj = 220;
//        m1.setAsynchronous(true);
        handler.sendMessage(m2);

        //同步消息
        Message m4 = Message.obtain();
        m4.obj = 300;
        handler.sendMessage(m4);


//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 100);

//        new Handler(new Handler.Callback() {
//            @Override
//            public boolean handleMessage(@NonNull Message msg) {
//                return false;
//            }
//        });

        //使用 IdleHandler
//        handler.getLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
//            @Override
//            public boolean queueIdle() {
//                return false;
//            }
//        });

        //开启同步屏障 这个方法标注为 hide。
        // handler.getLooper().getQueue().postSyncBarrier();
    }

    int token;

    //往消息队列插入同步屏障
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void sendSyncBarrier(Handler handler) {
        try {
            Log.d("MainActivity", "插入同步屏障");
            MessageQueue queue = handler.getLooper().getQueue();
            Method method = MessageQueue.class.getDeclaredMethod("postSyncBarrier");
            method.setAccessible(true);
            token = (int) method.invoke(queue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //移除屏障
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void removeSyncBarrier(Handler handler) {
        try {
            Log.d("MainActivity", "移除屏障");
            MessageQueue queue = handler.getLooper().getQueue();
            Method method = MessageQueue.class.getDeclaredMethod("removeSyncBarrier", int.class);
            method.setAccessible(true);
            method.invoke(queue, token);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

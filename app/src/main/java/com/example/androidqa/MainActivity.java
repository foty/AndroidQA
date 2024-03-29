package com.example.androidqa;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("lxx", "A onCreate");

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
        //testHandler(handler);

        RecyclerView recycler = findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        MyRecyclerViewAdapter adapter = new MyRecyclerViewAdapter();
        recycler.setAdapter(adapter);

        ImageView imageView = findViewById(R.id.imageView);
        TextView view = findViewById(R.id.tvHello);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(MainActivity.this, MainBActivity.class));
                Log.d("lxx", "开始模拟");
                testGlideLoad();
            }
        });

//        Glide.with(this)
//                .load("")
//                .skipMemoryCache(true)
//                .into(imageView);

        // 递归父布局
//        View child = view;
//        Log.d("lxx", "view= " + view);
//        while (child != null) {
//            child = (View) child.getParent();
//            Log.d("lxx", "parent= " + child);
//        }

        // 动画
//        TextView tv = new TextView(this);
//        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(tv, "translationX", 0, 100).setDuration(1 * 1000);
//        objectAnimator.start();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient client = builder.build();
        Request request = new Request.Builder()
                .addHeader("cookie", "10086")
                .url("http://123456.com")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("lxx", "A onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("lxx", "A onRestoreInstanceState");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lxx", "A onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("lxx", "A onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lxx", "A onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lxx", "A onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lxx", "A onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lxx", "A onDestroy");
    }

    private void testGlideLoad() {
        MnEngine engine = new MnEngine();
        MnDeCode deCode = new MnDeCode(engine);
        engine.start(deCode);
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

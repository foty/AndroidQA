package com.example.androidqa;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new Handler().post(new Runnable() {
            @Override
            public void run() {
                //todo
            }
        });

        Message msg = new Message();
        msg.what = 1;
        msg.obj = 100;
        new Handler().sendMessage(msg);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },100);

        new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                return false;
            }
        });

        //使用 IdleHandler
        new Handler().getLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                return false;
            }
        });

    }
}

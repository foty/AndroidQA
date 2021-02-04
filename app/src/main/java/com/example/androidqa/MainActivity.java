package com.example.androidqa;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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

    }
}

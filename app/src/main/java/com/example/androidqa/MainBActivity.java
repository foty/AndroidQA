package com.example.androidqa;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Method;

public class MainBActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("lxx", "B onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lxx", "B onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lxx", "B onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lxx", "B onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lxx", "B onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lxx", "B onDestroy");
    }

}

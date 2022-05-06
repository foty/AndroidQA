package com.example.androidqa;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create by lxx
 * Date : 2022/5/6 11:14
 * Use by
 */
class MnEngine implements MnDeCode.Callback {
    ExecutorService executor = Executors.newCachedThreadPool();

    public void start(MnDeCode de) {
        Log.d("lxx", "开始启动");
        executor.execute(de);
    }

    @Override
    public void reschedule(MnDeCode job) {
        Log.d("lxx", "MnEngine#reschedule()   " + job);
        Executors.newCachedThreadPool().execute(job);
    }
}

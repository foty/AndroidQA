package com.example.androidqa;

import android.util.Log;


/**
 * Create by lxx
 * Date : 2022/5/6 11:16
 * Use by
 */
class MnDeCode implements Runnable {
    private int stage;
    private int value;
    private Callback callback;

    public MnDeCode(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        Log.d("lxx", "run(): 开始执行任务: stage= " + stage + " , value= " + value);
        runWrapped();
    }

    public void runWrapped() {
        Log.d("lxx", "runWrapped() stage= " + stage);
        if (stage == 0) {
            value = loop(0);
            next();
        } else if (stage == 1) {
            next();
        }
    }

    private int loop(int index) {
        value++;
        if (index == 0) {
            Log.d("lxx", "loop() 初始化");
        }
        if (index == 1) {
            Log.d("lxx", "loop() 第二次不对劲");
        }
        return value;
    }

    public void next() {
        while (value < 10) {
            Log.d("lxx", "next(): stage= " + stage + ", value= " + value);
            value = loop(value);

            if (value == 2) {
                reschedule();
                return;
            }
        }
        Log.d("lxx", "next() while结束");
    }

    public void reschedule() {
        Log.d("lxx", "reschedule()  stage= " + stage + ", value= " + value);
        stage = 1;
        callback.reschedule(this);
    }

    interface Callback {
        void reschedule(MnDeCode job);
    }

}

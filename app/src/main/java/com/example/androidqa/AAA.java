package com.example.androidqa;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Create by lxx
 * Date : 2022/7/25
 * Use by
 */
class AAA {

    private String name;
    public String age;
    String tool;

    private void say() {
    }

    public void talk() {
        StaticC buy = buy();
        System.out.println("--- "+buy);
    }

    StaticC buy() {
        System.out.println("--- AAA buy");
        return new StaticC("11");
    }

    // 匿名内部类
    Handler h = new Handler() {

        private String a;
        public String b;
        private String c;

        private void d() {
        }

        public void e() {
        }

        void f() {
        }

        @Override
        public void publish(LogRecord record) {
            say();
            talk();
            buy();
            name = "xx";
            age = "18";
            tool = "sex";
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    };

    // 静态内部类
    public static class StaticC {
        StaticC(String c) {

        }

        private String name;

        void tes() {

        }
    }


}

class BBB extends AAA {
    private void to() {
    }

    //    @Override
    public static void go() {

    }
}

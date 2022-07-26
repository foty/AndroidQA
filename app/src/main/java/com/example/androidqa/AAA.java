package com.example.androidqa;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Create by lxx
 * Date : 2022/7/25
 * Use by
 */
class AAA {

    public static void go() {

    }

    private String name;
    public String age;
    String tool;

    private void say() {
    }

    void talk() {
    }

    public void buy() {
    }

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

    static class StaticC {
    }

    class Ddd extends StaticC {

    }

    public static void main(String[] args) {
        StaticC c = new StaticC();
        Ddd ddd = new Ddd();
    }
}

class BBB extends AAA {
    private void to() {
    }

    //    @Override
    public static void go() {

    }
}

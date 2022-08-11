package com.example.androidqa;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Create by lxx
 * Date : 2022/7/25
 * Use by
 */
public class Bb extends AAA {

    static {
        System.out.println("--- 静态代码块");
    }

    public Bb(){
        System.out.println("--- 构造方法");
    }

    static class StaticB extends AAA.StaticC {

        StaticB(String c) {
            super(c);
        }
    }

    StaticC buy() {
        System.out.println("--- Bb buy");
        return new StaticB("222");
    }

    public static void main(String[] args) {
        Bb b = new Bb();
        b.talk();

        List<String> l = new ArrayList<>();
//        testString(l);

//        testNormal();

        print3();
    }

    public static void testString(List<Object> o) {
        String s = "test";
        String s1 = new String("fast");
//        String s2 = new String("a")+new String("b");
        System.out.println(o.get(0));
    }

    // ----------------------------------------------------------

    static boolean flag2;

    public static void testNormal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    while (!flag2) {
                    }
                    System.out.print(" A ");
                    flag2 = false;
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; ) {
                    while (flag2) {
                    }
                    System.out.print(" B ");
                    flag2 = true;
                }
            }
        }).start();

    }
    //------------------------------------------------------------

    //======================================================

    static Lock lock = new ReentrantLock();
    static int flag3 = 1;

    static Condition condition1 = lock.newCondition();
    static Condition condition2 = lock.newCondition();
    static Condition condition3 = lock.newCondition();

    public static void printA() {
        for (int i = 0; i < 10; i++) {
            try {
                lock.lock();
                while (flag3 != 1) {
                    condition1.await();
                }
                System.out.print(" 1、");
                flag3 = 2;
                condition2.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    public static void printB() {
        for (int i = 0; i < 10; i++) {
            try {
                lock.lock();
                while (flag3 != 2) {
                    condition2.await();
                }
                System.out.print("2、");
                flag3 = 3;
                condition3.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }


    public static void printC() {
        for (int i = 0; i < 10; i++) {
            try {
                lock.lock();
                while (flag3 != 3) {
                    condition3.await();
                }
                System.out.println("3");
                flag3 = 1;
                condition1.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    static void print3() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                printA();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                printB();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                printC();
            }
        }).start();
    }

    //======================================================

}

package com.example.androidqa;

/**
 * Create by lxx
 * Date : 2022/7/25
 * Use by
 */
public class Bb extends AAA {

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
    }
}

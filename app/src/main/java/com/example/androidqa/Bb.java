package com.example.androidqa;

import java.util.ArrayList;
import java.util.List;

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

        List<String> l =  new ArrayList<>();
        testString(l);
    }

    public static void testString(List<Object> o){
        String s = "test";
        String s1 = new String("fast");
//        String s2 = new String("a")+new String("b");
        System.out.println(o.get(0));
    }
}

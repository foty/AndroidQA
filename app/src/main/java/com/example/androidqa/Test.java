package com.example.androidqa;

import java.util.HashMap;
import java.util.List;

/**
 * Create by lxx
 * Date : 2022/2/18 11:15
 * Use by
 */
class Test extends TestSuper {
    private String name;
    private int age;
    public String sex;
    public String work;
    String money;
    public HashMap<String,String> map;
    public List<String> list;

    public Test() { }

    public Test(int age, String work) {}

    private Test(int op) {}

    private Test(String s){}

    private Test(String s,String sex){}


    public void m1(){
        System.out.println("m m m1");
    }

    public void sup1(){
        System.out.println("Override sup1");
    }

    private void m2(){
        System.out.println("m m m2");
    }

    public void m3(){
        System.out.println("m m m3");
    }
}

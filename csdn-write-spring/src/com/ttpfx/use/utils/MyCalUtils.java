package com.ttpfx.use.utils;

import com.ttpfx.myspring.annotation.Component;

/**
 * @Author: ttpfx
 * @Date: 2022/06/23/15:52
 */

@Component("utils")
public class MyCalUtils implements CalUtils{

    @Override
    public int add(int a, int b) {
        System.out.println(a + " + " + b + " = " + (a + b));
        return a + b;
    }

    @Override
    public int sub(int a, int b) {
        System.out.println(a + " - " + b + " = " + (a - b));
        return a - b;
    }
}

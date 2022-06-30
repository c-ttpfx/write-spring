package com.ttpfx.myspring.factory;

import java.lang.reflect.Method;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/23:45
 */

public class JoinPoint {

    private Method method;
    private Object[] agrs;
    public String getName(){
        return method.getName();
    }
    public Object[] getArgs(){
        return agrs;
    }

    public JoinPoint(Method method, Object[] agrs) {
        this.method = method;
        this.agrs = agrs;
    }
}

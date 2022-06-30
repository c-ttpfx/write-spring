package com.ttpfx.use.aspect;

import com.ttpfx.myspring.annotation.After;
import com.ttpfx.myspring.annotation.Aspect;
import com.ttpfx.myspring.annotation.Component;
import com.ttpfx.myspring.annotation.Order;
import com.ttpfx.myspring.factory.JoinPoint;

import java.util.Arrays;

/**
 * @Author: ttpfx
 * @Date: 2022/06/23/20:45
 */
@Aspect
@Component
@Order(200)
public class MyAspect3 {

    @After(value = "execution(public int com.ttpfx.use.utils.MyCalUtils.sub(int, int))")
    public void thread(JoinPoint joinPoint) {
        System.out.println("[MyAspect3]切面方法---->after，切入方法名：" + joinPoint.getName() + " 目标方法参数：" + Arrays.toString(joinPoint.getArgs()));
    }
}

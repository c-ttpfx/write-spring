package com.ttpfx.use.aspect;

import com.ttpfx.myspring.annotation.*;
import com.ttpfx.myspring.factory.JoinPoint;

import java.util.Arrays;

/**
 * @Author: ttpfx
 * @Date: 2022/06/23/15:47
 */

@Aspect
@Component
@Order(300)
public class MyAspect {

    @Before(value = "execution(public    int com.ttpfx.use.utils.MyCalUtils.add(int, int))")
    public void first(JoinPoint joinPoint) {
        System.out.println("切面方法---->before，切入方法名：" + joinPoint.getName() + " 目标方法参数：" + Arrays.toString(joinPoint.getArgs()));
    }

    @After(value = "execution(public int com.ttpfx.use.utils.MyCalUtils.add(int, int))")
    public void second(JoinPoint joinPoint) {
        System.out.println("切面方法---->after，切入方法名：" + joinPoint.getName() + " 目标方法参数：" + Arrays.toString(joinPoint.getArgs()));
    }

    @After(value = "execution(public int com.ttpfx.use.utils.MyCalUtils.sub(int, int))")
    public void thread(JoinPoint joinPoint) {
        System.out.println("[MyAspect1]切面方法---->after，切入方法名：" + joinPoint.getName() + " 目标方法参数：" + Arrays.toString(joinPoint.getArgs()));
    }
}

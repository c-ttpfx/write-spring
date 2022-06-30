package com.ttpfx.myspring.annotation;

import java.lang.annotation.*;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/22:28
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface Order {
    int value() default Integer.MAX_VALUE;
}

package com.ttpfx.myspring.processor;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/17:20
 */
public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
}

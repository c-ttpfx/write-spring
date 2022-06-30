package com.ttpfx.myspring.factory;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/13:03
 */

public class BeanDefinition {
    private Class<?> clazz;
    private String type;

    public BeanDefinition(Class<?> clazz, String type) {
        this.clazz = clazz;
        this.type = type;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

package com.ttpfx.use.dao;

import com.ttpfx.myspring.annotation.Component;
import com.ttpfx.myspring.annotation.ComponentScan;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/12:33
 */

@Component("myUserDao")
public class UserDao {

    public void show(){
        System.out.println("我是userDao");
    }
}

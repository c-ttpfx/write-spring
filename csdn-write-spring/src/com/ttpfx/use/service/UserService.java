package com.ttpfx.use.service;

import com.ttpfx.myspring.annotation.Component;
import com.ttpfx.myspring.annotation.Resource;
import com.ttpfx.use.dao.UserDao;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/12:34
 */

@Component
public class UserService {

    @Resource("myUserDao")
    private UserDao userDao;

    public void show(){
        userDao.show();
        System.out.println("我是userService");
    }
}

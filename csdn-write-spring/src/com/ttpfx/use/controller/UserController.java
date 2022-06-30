package com.ttpfx.use.controller;

import com.ttpfx.myspring.annotation.Component;
import com.ttpfx.myspring.annotation.Resource;
import com.ttpfx.use.service.UserService;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/12:34
 */
@Component
public class UserController {

    @Resource
    private UserService userService;

    public void show(){
        userService.show();
        System.out.println("我是userController");
    }
}

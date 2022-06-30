package com.ttpfx.use.test;

import com.ttpfx.myspring.context.ApplicationContext;
import com.ttpfx.use.config.ComponentScanPathConfig;
import com.ttpfx.use.controller.UserController;
import com.ttpfx.use.dao.MemberDao;
import com.ttpfx.use.utils.CalUtils;

/**
 * @Author: ttpfx
 * @Date: 2022/06/30/12:36
 */

public class MySpringTest {

    public static void main(String[] args) {
        ApplicationContext ioc = new ApplicationContext(ComponentScanPathConfig.class);
        CalUtils utils = ioc.getBean("utils", CalUtils.class);
        utils.sub(1, 2);
        System.out.println("-----------------------");
        utils.add(1, 2);
    }
}

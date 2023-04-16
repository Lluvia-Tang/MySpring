package com.my.service;

import com.my.spring.MyApplicationContext;

public class Test {
    public static void main(String[] args) {
        // 容器根据配置类扫描
        MyApplicationContext applicationContext = new MyApplicationContext(AppConfig.class);

        // 根据名字获取Bean
        UserInterface userService = (UserInterface) applicationContext.getBean("userService");
        userService.test();
    }
}

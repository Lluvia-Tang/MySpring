package com.my.service;

import com.my.spring.BeanPostProcessor;
import com.my.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        // 可以批量操作，也可以单独操作
        if (beanName.equals("userService")){
            System.out.println("可以初始化前进行操控userService对象");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (beanName.equals("userService")){
            System.out.println("可以初始化后进行操控userService对象");

            //可以在这里进行AOP，返回代理对象
            Object proxyInstance = Proxy.newProxyInstance(MyBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("切面逻辑");
                    return method.invoke(bean, args); //执行原始userService对象的test方法
                }
            });

            return proxyInstance;
        }

        return bean;
    }
}

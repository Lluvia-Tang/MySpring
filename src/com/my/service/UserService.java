package com.my.service;

import com.my.spring.*;

@Component
@Scope("prototype")
public class UserService implements BeanNameAware, InitializingBean, UserInterface {

    @Autowired
    private OrderService orderService;

    private String beanName;


    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void test(){
        System.out.println(orderService);
    }

    @Override
    public void afterPropertiesSet() {
        //.......
        System.out.println("afterPropertiesSet()初始化方法");
    }
}

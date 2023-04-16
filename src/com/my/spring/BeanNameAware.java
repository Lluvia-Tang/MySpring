package com.my.spring;

/**
 * 使bean知道自己的beanName
 */
public interface BeanNameAware {
    public void setBeanName(String beanName);
}

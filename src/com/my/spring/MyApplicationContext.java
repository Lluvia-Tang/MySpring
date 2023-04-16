package com.my.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MyApplicationContext {
    private Class configClass;

    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    //单例池
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();

    private ArrayList<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    /**
     * 容器构造方法
     * @param configClass
     */
    public MyApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 扫描 --> BeanDefinition --> BeanDefinitionMap
        if (configClass.isAnnotationPresent(ComponentScan.class)){
            // 获取到注解
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);

            // 根据注解获取到扫描路径
            String path = componentScanAnnotation.value(); // com.my.service

            // 根据上述路径拿到class文件的路径
            // 优化path
            path = path.replace(".", "/"); // com/my/service 相对路径
            ClassLoader classLoader = MyApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);//获取相对路径的资源绝对路径
            File file = new File(resource.getFile());
//            System.out.println(file); // D:\java_workplace\MySpring\out\production\MySpring\com\my\service
            if (file.isDirectory()){
                File[] files = file.listFiles();
                for (File f : files) {
                    String fileName = f.getAbsolutePath();
//                    System.out.println(fileName); //D:\java_workplace\MySpring\out\production\MySpring\com\my\service\UserService.class
                    if (fileName.endsWith(".class")){
                        // 判断class文件对应的是否是bean ==> 判断类上是否有Component注解
                        String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                        className = className.replace("\\", "."); //com.my.service.UserService
                        try {
                            // 得到类的Class对象
                            Class<?> clazz = classLoader.loadClass(className);
                            // 是Bean
                            if (clazz.isAnnotationPresent(Component.class)){

                                // 该类是否实现了BeanPostProcessor接口
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)){
                                    BeanPostProcessor instance = (BeanPostProcessor) clazz.newInstance();
                                    beanPostProcessorList.add(instance);
                                }

                                // 获取bean的名字
                                Component component = clazz.getAnnotation(Component.class);
                                String beanName = component.value();
                                // 自动生成beanName
                                if (beanName.equals("")){
                                    beanName = Introspector.decapitalize(clazz.getSimpleName());
                                }

                                // 首先生成BeanDefinition对象
                                BeanDefinition beanDefinition = new BeanDefinition();
                                beanDefinition.setType(clazz);

                                // 类上是否有scope注解
                                if (clazz.isAnnotationPresent(Scope.class)){
                                    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                    beanDefinition.setScope(scopeAnnotation.value());
                                }else{
                                    //单例
                                    beanDefinition.setScope("singleton");
                                }

                                //存储BeanDefinition对象
                                beanDefinitionMap.put(beanName, beanDefinition);
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
        }

        // 实例化单例bean
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                Object bean = creatBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }

    }

    // Bean的生命周期
    private Object creatBean(String beanName, BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getType();

        try {
            Object instance = clazz.getConstructor().newInstance();

            // 依赖注入
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Autowired.class)){
                    f.setAccessible(true);
                    // 给带有Autowired注解的属性赋值
                    f.set(instance, getBean(f.getName()));
                }
            }

            // Aware回调
            // 判断对象是否实现了BeanNameAware接口，进行赋值
            if (instance instanceof BeanNameAware){
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            // 初始化前增强器
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(beanName, instance);
            }

            // 初始化
            // 调用bean的初始化方法afterPropertiesSet()
            if (instance instanceof InitializingBean){
                ((InitializingBean) instance).afterPropertiesSet();
            }

            // BeanPostProcessor bean的后置增强器（可以灵活地对bean的创建过程进行控制）
            // 初始化后 AOP
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(beanName, instance);
            }

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getBean(String beanName){

        // 根据beanName找到对应的类生成对应的bean对象（还需要知道是单例还是多例）
        // 获取BeanDefinition对象
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if (beanDefinition == null){
            throw new NullPointerException();
        }else {
            String scope = beanDefinition.getScope();
            if (scope.equals("singleton")){
                // 单例
                Object bean = singletonObjects.get(beanName);
                if (bean == null){
                    Object o = creatBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, o);
                }
                return bean;
            } else {
                // 多例
                return creatBean(beanName, beanDefinition);
            }
        }

    }
}

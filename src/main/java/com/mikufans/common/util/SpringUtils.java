package com.mikufans.common.util;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * spring 工具类
 * 扩展spring  方便管理  spring类
 */
@Component
public class SpringUtils implements BeanFactoryPostProcessor
{
    //spring上下文环境
    private static ConfigurableListableBeanFactory beanFactory;

    /**
     * 获取 spring对象
     *
     * @param name
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name) throws BeansException
    {
        return (T) beanFactory.getBean(name);
    }

    public static <T> T getBean(Class<T> cls)
    {
        T result = (T) beanFactory.getBean(cls);
        return result;
    }

    /**
     * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
     */
    public static boolean containsBean(String name)
    {
        return beanFactory.containsBean(name);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException
    {
        SpringUtils.beanFactory = configurableListableBeanFactory;
    }

    /**
     * 获取aop代理对象
     * @param <T>
     * @return
     */
    public static <T> T getAopProxy(T invoker)
    {
        return (T) AopContext.currentProxy();
    }

}

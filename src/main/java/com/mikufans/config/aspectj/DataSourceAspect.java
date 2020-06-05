package com.mikufans.config.aspectj;

import com.mikufans.common.util.StringUtils;
import com.mikufans.config.aspectj.annotation.DataSource;
import com.mikufans.config.datasource.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;


/**
 * 多数据源处理
 */
@Aspect
@Order(1)
@Component
@Slf4j
public class DataSourceAspect
{

    @Pointcut("@annotation(com.mikufans.config.aspectj.annotation.DataSource)" + "||" +
            "@within(com.mikufans.config.aspectj.annotation.DataSource)")
    public void dsPointCut()
    {

    }

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        DataSource dataSource = getDataSourceAnnotation(point);
        if(StringUtils.isNotNull(dataSource))
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
        try
        {
            return point.proceed();
        } finally
        {
            //执行完后 销毁数据源
            DynamicDataSourceContextHolder.clearDataSourceType();;
        }
    }

    /**
     * 获取要切换的数据源
     *
     * @param point
     * @return
     */
    public DataSource getDataSourceAnnotation(ProceedingJoinPoint point)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource))
            return dataSource;
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }

}

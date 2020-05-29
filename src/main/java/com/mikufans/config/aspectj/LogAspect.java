package com.mikufans.config.aspectj;

import com.mikufans.common.util.ShiroUtils;
import com.mikufans.config.aspectj.annotation.Log;
import com.mikufans.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.Joinpoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class LogAspect
{

    @Pointcut("@annotation(com.mikufans.config.aspectj.annotation.Log)")
    public void logPointCut() {}

    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinpoint, Object jsonResult)
    {

    }

    protected void handleLog(final JoinPoint joinpoint, final Exception e, Object jsonObject)
    {
        try
        {
            Log controllerLog=getAnnotationLog(joinpoint);
            if(controllerLog==null)
                return;

            User user= ShiroUtils.getSysUser();
            //todo  记录日志

        }catch (Exception exp)
        {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }


    /**
     * 是否存在注解，如果存在就获取
     */
    private Log getAnnotationLog(JoinPoint joinpoint)
    {
        Signature signature = joinpoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        if (method != null)
            return method.getAnnotation(Log.class);
        return null;
    }

}

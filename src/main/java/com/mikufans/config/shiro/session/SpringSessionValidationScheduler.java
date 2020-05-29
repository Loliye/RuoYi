package com.mikufans.config.shiro.session;

import com.mikufans.common.util.Threads;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.mgt.SessionValidationScheduler;
import org.apache.shiro.session.mgt.ValidatingSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自定义任务调度器完成
 */
@Slf4j
@Component
public class SpringSessionValidationScheduler implements SessionValidationScheduler
{
    /**
     * 定时器任务，用于处理超时的挂起请求   也用于连接断开时的重连
     */
    @Autowired
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService executorService;

    @Autowired
    @Qualifier("sessionManager")
    @Lazy
    private ValidatingSessionManager sessionManager;

    // 相隔多久检查一次session的有效性，单位毫秒，默认就是10分钟
    @Value("${shiro.session.validationInterval}")
    private long sessionValidationInterval;

    private volatile boolean enabled = false;

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public void enableSessionValidation()
    {
        enabled = true;

        if (log.isDebugEnabled())
        {
            log.debug("Scheduling session validation job using Spring Scheduler with "
                    + "session validation interval of [" + sessionValidationInterval + "]ms...");
        }
        try
        {
            executorService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    if (enabled)
                        sessionManager.validateSessions();
                }
            }, 1000, sessionValidationInterval * 60 * 1000, TimeUnit.MILLISECONDS);

            this.enabled = true;

            if (log.isDebugEnabled())
                log.debug("Session validation job successfully scheduled with Spring Scheduler.");

        } catch (Exception e)
        {
            if (log.isErrorEnabled())
                log.error("Error starting the Spring Scheduler session validation job.  Session validation may not occur.", e);
        }
    }

    @Override
    public void disableSessionValidation()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Stopping Spring Scheduler session validation job...");
        }
        if (this.enabled)
            Threads.shutdownAndAwaitTermination(executorService);
        this.enabled = false;
    }
}

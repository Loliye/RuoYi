package com.mikufans.config.manager;

import com.mikufans.common.util.SpringUtils;
import com.mikufans.common.util.Threads;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务管理器
 */
public class AsyncManager
{
    private static AsyncManager me = new AsyncManager();
    //操作推迟10ms
    private final int OPERATE_DELAY_TIME = 10;
    //异步操作任务跳度线程池
    private ScheduledExecutorService executorService = SpringUtils.getBean("scheduledExecutorService");

    /**
     * 单例模式
     */
    private AsyncManager()
    {
    }

    public static AsyncManager me()
    {
        return me;
    }


    /**
     * 执行任务
     *
     * @param task 任务
     */
    public void execute(TimerTask task)
    {
        executorService.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown()
    {
        Threads.shutdownAndAwaitTermination(executorService);
    }
}

package com.mikufans.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class Threads
{
    public static void sleep(long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e)
        {
            return;
        }
    }

    /**
     * 停止线程池
     * 先使用shutdown, 停止接收新任务并尝试完成所有已存在任务.
     * 如果超时, 则调用shutdownNow, 取消在workQueue中Pending的任务,并中断所有阻塞函数.
     * 如果仍人超時，則強制退出.
     * 另对在shutdown时线程本身被调用中断做了处理.
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool)
    {
        if (pool != null && !pool.isShutdown())
        {
            pool.shutdown();
            try
            {
                if (!pool.awaitTermination(120, TimeUnit.SECONDS))
                {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(120, TimeUnit.SECONDS))
                    {
                        log.info("Pool did not terminate");
                    }
                }
            }
            catch (InterruptedException ie)
            {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void printException(Runnable runnable,Throwable throwable)
    {
        if (throwable == null && runnable instanceof Future<?>)
        {
            try
            {
                Future<?> future = (Future<?>) runnable;
                if (future.isDone())
                {
                    future.get();
                }
            }
            catch (CancellationException ce)
            {
                throwable = ce;
            }
            catch (ExecutionException ee)
            {
                throwable = ee.getCause();
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
            }
        }
        if (throwable != null)
        {
            log.error(throwable.getMessage(), throwable);
        }
    }
}

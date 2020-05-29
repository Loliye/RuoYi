package com.mikufans.common.exception;

import com.mikufans.common.util.JobInvokeUtil;
import com.mikufans.domain.monitor.AbstractQuartzJob;
import com.mikufans.domain.monitor.Job;
import org.quartz.JobExecutionContext;

/**
 * 定时任务处理（允许并发执行）
 *
 * @author ruoyi
 *
 */
public class QuartzJobExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, Job job) throws Exception
    {
        JobInvokeUtil.invokeMethod(job);
    }
}

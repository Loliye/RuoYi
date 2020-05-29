package com.mikufans.config.manager.factory;

import com.mikufans.common.constant.Constants;
import com.mikufans.common.util.*;
import com.mikufans.domain.monitor.Logininfor;
import com.mikufans.domain.monitor.OperLog;
import com.mikufans.domain.online.OnlineSession;
import com.mikufans.domain.online.UserOnline;
import com.mikufans.service.monitor.IOperLogService;
import com.mikufans.service.monitor.IUserOnlineService;
import com.mikufans.service.monitor.LogininforServiceImpl;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * 异步工厂（产生任务用）
 */
@Slf4j
public class AsyncFactory
{

    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    public static TimerTask syncSessionToDb(final OnlineSession session)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {

                UserOnline online = new UserOnline();
                online.setSessionId(String.valueOf(session.getId()));
                online.setDeptName(session.getDeptName());
                online.setLoginName(session.getLoginName());
                online.setStartTimestamp(session.getStartTimestamp());
                online.setLastAccessTime(session.getLastAccessTime());
                online.setExpireTime(session.getTimeout());
                online.setIpaddr(session.getHost());
                online.setLoginLocation(AddressUtils.getRealAddressByIP(session.getHost()));
                online.setBrowser(session.getBrowser());
                online.setOs(session.getOs());
                online.setStatus(session.getStatus());
                online.setSession(session);
                SpringUtils.getBean(IUserOnlineService.class).saveOnline(online);
            }
        };
    }


        /**
         * 操作日志记录
         *
         * @param operLog 操作日志信息
         * @return 任务task
         */
        public static TimerTask recordOper(final OperLog operLog)
        {
            return new TimerTask()
            {
                @Override
                public void run()
                {
                    // 远程查询操作地点
                    operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
                    SpringUtils.getBean(IOperLogService.class).insertOperlog(operLog);
                }
            };
        }

    /**
     * 记录登陆信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息
     * @param args     列表
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message, final Object... args)
    {
        final UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        final String ip = ShiroUtils.getIp();
        return new TimerTask()
        {
            @Override
            public void run()
            {
                String address = AddressUtils.getRealAddressByIP(ip);
                StringBuilder s = new StringBuilder();
                s.append(LogUtils.getBlock(ip));
                s.append(address);
                s.append(LogUtils.getBlock(username));
                s.append(LogUtils.getBlock(status));
                s.append(LogUtils.getBlock(message));
                // 打印信息到日志
                sys_user_logger.info(s.toString(), args);
                // 获取客户端操作系统
                String os = userAgent.getOperatingSystem().getName();
                // 获取客户端浏览器
                String browser = userAgent.getBrowser().getName();
                // 封装对象
                Logininfor logininfor = new Logininfor();
                logininfor.setLoginName(username);
                logininfor.setIpaddr(ip);
                logininfor.setLoginLocation(address);
                logininfor.setBrowser(browser);
                logininfor.setOs(os);
                logininfor.setMsg(message);
                // 日志状态
                if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
                {
                    logininfor.setStatus(Constants.SUCCESS);
                } else if (Constants.LOGIN_FAIL.equals(status))
                {
                    logininfor.setStatus(Constants.FAIL);
                }
                // 插入数据
                SpringUtils.getBean(LogininforServiceImpl.class).insertLogininfor(logininfor);
            }
        };
    }

}

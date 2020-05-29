package com.mikufans.config.shiro.session;

import com.mikufans.config.manager.AsyncManager;
import com.mikufans.config.manager.factory.AsyncFactory;
import com.mikufans.domain.online.OnlineSession;
import com.mikufans.domain.online.UserOnline;
import com.mikufans.service.monitor.IUserOnlineService;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;
import java.util.Date;


/**
 * 针对自定义得shiroSession得db操作
 */
public class OnlineSessionDao extends EnterpriseCacheSessionDAO
{
    /**
     * 上次同步数据库得时间戳
     */
    private static final String LAST_SYNC_DB_TIMESTAMP = OnlineSessionDao.class.getName() + "LAST_SYNC_DB_TIMESTAMP";
    /**
     * 同步session到数据库的周期 单位为毫秒（默认1分钟）
     */
    @Value("${shiro.session.dbSyncPeriod}")
    private int dbSyncPeriod;

    @Autowired
    private OnlineSessionFactory onlineSessionFactory;

    @Autowired
    private IUserOnlineService onlineService;

    public OnlineSessionDao()
    {
        super();
    }

    public OnlineSessionDao(long expireTime)
    {
        super();
    }

    /**
     * 根据会话id获取会话
     *
     * @param sessionId
     * @return Shiro.Session
     */
    @Override
    protected Session doReadSession(Serializable sessionId)
    {
        UserOnline userOnline = onlineService.selectOnlineById(String.valueOf(sessionId));
        if (userOnline == null)
            return null;
        return onlineSessionFactory.createSession(userOnline);
    }

    @Override
    public void update(Session session) throws UnknownSessionException
    {
        super.update(session);
    }

    /**
     * 当前会话过期/停止 （用户退出）属性等会调用
     */
    @Override
    protected void doDelete(Session session)
    {
        OnlineSession onlineSession = (OnlineSession) session;
        if (onlineSession == null)
            return;

        onlineSession.setStatus(OnlineSession.OnlineStatus.off_line);
        onlineService.deleteOnlineById(String.valueOf(onlineSession.getId()));

    }

    /**
     * 更新会话：如更新会话最后访问时间/停止会话/设置超时时间/设置移除属性等会调用
     */
    public void syncToDb(OnlineSession onlineSession)
    {
        Date lastSyncTimestamp = (Date) onlineSession.getAttribute(LAST_SYNC_DB_TIMESTAMP);
        if (lastSyncTimestamp != null)
        {
            boolean needSync = true;
            long deltaTime = onlineSession.getLastAccessTime().getTime() - lastSyncTimestamp.getTime();
            if (deltaTime < dbSyncPeriod * 60 * 1000)
                //时间差不足 无需同步
                needSync = false;

            boolean isGuest = onlineSession.getUserId() == null || onlineSession.getUserId() == 0L;
            if (!isGuest && onlineSession.isAttributeChanged())
                needSync = true;

            if (!needSync)
                return;

        }
        //更新数据库同步时间
        onlineSession.setAttribute(LAST_SYNC_DB_TIMESTAMP, onlineSession.getLastAccessTime());
        if (onlineSession.isAttributeChanged())
            onlineSession.resetAttributeChanged();

        AsyncManager.me().execute(AsyncFactory.syncSessionToDb(onlineSession));

    }
}

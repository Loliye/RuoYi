package com.mikufans.config.shiro.filter;

import com.mikufans.common.constant.Constants;
import com.mikufans.common.constant.ShiroConstants;
import com.mikufans.common.util.MessageUtils;
import com.mikufans.common.util.ShiroUtils;
import com.mikufans.common.util.StringUtils;
import com.mikufans.config.manager.AsyncManager;
import com.mikufans.config.manager.factory.AsyncFactory;
import com.mikufans.domain.user.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.subject.Subject;
import org.apache.velocity.util.introspection.SecureUberspector;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Deque;

/**
 * 退出过滤器
 */
@Slf4j
public class LogoutFilter extends org.apache.shiro.web.filter.authc.LogoutFilter
{
    private String loginUrl;
    private Cache<String, Deque<Serializable>> cache;

    public String getLoginUrl()
    {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl)
    {
        this.loginUrl = loginUrl;
    }

    // 设置Cache的key的前缀
    public void setCacheManager(CacheManager cacheManager)
    {
        // 必须和ehcache缓存配置中的缓存name一致
        this.cache = cacheManager.getCache(ShiroConstants.SYS_USERCACHE);
    }


    /**
     * 清理缓存  添加日志操作
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception
    {
        Subject subject = this.getSubject(request, response);
        String redirectUrl = this.getRedirectUrl(request, response, subject);
        User user = ShiroUtils.getSysUser();
        if (StringUtils.isNotNull(user))
        {
            String loginName = user.getLoginName();
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
            //清理缓存
            cache.remove(loginName);
        }
        subject.logout();
        this.issueRedirect(request, response, redirectUrl);
        return false;
    }

    /**
     * 修改跳转得url
     */
    @Override
    protected String getRedirectUrl(ServletRequest request, ServletResponse response, Subject subject)
    {
        String url = this.getLoginUrl();
        if (StringUtils.isNotEmpty(url))
            return url;
        return super.getRedirectUrl(request, response, subject);
    }
}



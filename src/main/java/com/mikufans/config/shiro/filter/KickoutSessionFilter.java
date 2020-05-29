package com.mikufans.config.shiro.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikufans.common.constant.ShiroConstants;
import com.mikufans.common.util.ServletUtils;
import com.mikufans.common.util.ShiroUtils;
import com.mikufans.common.web.AjaxResult;
import com.mikufans.domain.user.User;
import com.sun.xml.internal.ws.developer.Serialization;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

public class KickoutSessionFilter extends AccessControlFilter
{
    private final static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 同一个用户得最大会话数
     */
    private int maxSession = -1;

    /**
     * 踢出之前得登陆用户/之后登陆得用户   默认false踢出之前登陆得用户
     */
    private Boolean kickoutAfter = false;

    /**
     * 踢出得地址
     */
    private String kickoutUrl;

    private SessionManager sessionManager;
    private Cache<String, Deque<Serializable>> cache;

    public void setMaxSession(int maxSession)
    {
        this.maxSession = maxSession;
    }

    public void setKickoutAfter(boolean kickoutAfter)
    {
        this.kickoutAfter = kickoutAfter;
    }

    public void setKickoutUrl(String kickoutUrl)
    {
        this.kickoutUrl = kickoutUrl;
    }

    public void setSessionManager(SessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }


    // 设置Cache的key的前缀
    public void setCacheManager(CacheManager cacheManager)
    {
        // 必须和ehcache缓存配置中的缓存name一致
        this.cache = cacheManager.getCache(ShiroConstants.SYS_USERCACHE);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception
    {
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception
    {
        Subject subject = this.getSubject(servletRequest, servletResponse);
        if (!subject.isAuthenticated() && !subject.isRemembered() || maxSession == -1)
            // 如果没有登录或用户最大会话数为-1，直接进行之后的流程
            return true;

        try
        {
            Session session = subject.getSession();
            User user = ShiroUtils.getSysUser();
            String loginName = user.getLoginName();
            Serializable sessionId = session.getId();


            //读取缓存用户，没有就载入
            Deque<Serializable> deque = cache.get(loginName);
            if (deque == null)
                deque = new ArrayDeque<>();
            //如果队列里又没此sessionId，且用没有呗踢出；放入队列中
            if(!deque.contains(sessionId)&&session.getAttribute("kickout")==null)
            {
                deque.push(sessionId);
                cache.put(loginName,deque);
            }

            //如果队列里得sessionId超过最大会话数，开始踢人
            while(deque.size()>maxSession)
            {
                Serializable kickoutSessionId = null;
                // 是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；
                if (kickoutAfter)
                {
                    // 踢出后者
                    kickoutSessionId = deque.removeFirst();
                }
                else
                {
                    // 踢出前者
                    kickoutSessionId = deque.removeLast();
                }
                // 踢出后再更新下缓存队列
                cache.put(loginName, deque);

                //获取被踢出得sessionId得session对象
                Session kickoutSession = sessionManager.getSession(new DefaultSessionKey(kickoutSessionId));
                if(kickoutSession!=null)
                    //设置会话得kickout属性标识踢出
                    kickoutSession.setAttribute("kickout",true);
            }

            //如果进行踢出  重定向到踢出后得页面
            if((session.getAttribute("kickout") != null) && ((Boolean) session.getAttribute("kickout") == true))
            {
                subject.logout();
                saveRequest(servletRequest);
                return isAjaxResponse(servletRequest,servletResponse);
            }
            return true;
        }catch (Exception e)
        {
            return isAjaxResponse(servletRequest,servletResponse);
        }
    }

    private boolean isAjaxResponse(ServletRequest request, ServletResponse response) throws IOException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (ServletUtils.isAjaxRequest(req))
        {
            AjaxResult ajaxResult = AjaxResult.error("您已在别处登录，请您修改密码或重新登录");
            ServletUtils.renderString(res, objectMapper.writeValueAsString(ajaxResult));
        }
        else
        {
            WebUtils.issueRedirect(request, response, kickoutUrl);
        }
        return false;
    }


}

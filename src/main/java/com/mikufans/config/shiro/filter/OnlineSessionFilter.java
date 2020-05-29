package com.mikufans.config.shiro.filter;

import com.mikufans.common.constant.ShiroConstants;
import com.mikufans.common.util.ShiroUtils;
import com.mikufans.config.shiro.session.OnlineSessionDao;
import com.mikufans.domain.online.OnlineSession;
import com.mikufans.domain.user.User;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 自定义访问控制
 */
public class OnlineSessionFilter extends AccessControlFilter
{

    /**
     * 强制退出后重定向的地址
     */
    @Value("${shiro.user.loginUrl}")
    private String loginUrl;

    @Autowired
    private OnlineSessionDao onlineSessionDao;

    /**
     * 表示是否允许访问；mappedValue就是[urls]配置中拦截器参数部分，如果允许访问返回true，否则false；
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception
    {
        Subject subject = this.getSubject(servletRequest, servletResponse);
        if (subject == null || subject.getSession() == null)
            return true;

        Session session = onlineSessionDao.readSession(subject.getSession().getId());
        if (session != null && session instanceof OnlineSession)
        {
            OnlineSession onlineSession = (OnlineSession) session;
            servletRequest.setAttribute(ShiroConstants.ONLINE_SESSION, onlineSession);
            //把user对象添加进去
            boolean isGuest = onlineSession.getUserId() == null || onlineSession.getUserId() == 0L;
            if (isGuest == true)
            {
                User user = ShiroUtils.getSysUser();
                if (user != null)
                {
                    onlineSession.setUserId(user.getUserId());
                    onlineSession.setLoginName(user.getLoginName());
                    onlineSession.setAvatar(user.getAvatar());
                    onlineSession.setDeptName(user.getDept().getDeptName());
                    onlineSession.markAttributeChanged();
                }
            }

            if (onlineSession.getStatus() == OnlineSession.OnlineStatus.off_line)
                return false;
        }

        return true;
    }

    /**
     * 表示当访问拒绝时是否已经处理了；如果返回true表示需要继续处理；如果返回false表示该拦截器实例已经处理了，将直接返回即可。
     */
    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception
    {
        Subject subject = this.getSubject(servletRequest, servletResponse);
        if (subject != null)
            subject.logout();
        this.saveRequestAndRedirectToLogin(servletRequest, servletResponse);
        return false;
    }

    // 跳转到登录页
    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        WebUtils.issueRedirect(request, response, loginUrl);
    }
}

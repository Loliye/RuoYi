package com.mikufans.config.shiro.realm;

import com.mikufans.common.exception.user.*;
import com.mikufans.common.util.ShiroUtils;
import com.mikufans.config.shiro.service.LoginService;
import com.mikufans.domain.user.User;
import com.mikufans.service.menu.IMenuService;
import com.mikufans.service.role.IRoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义Realm  处理登陆 授权
 */
@Slf4j
public class UserRealm extends AuthorizingRealm
{
    @Autowired
    private IMenuService menuService;
    @Autowired
    private IRoleService roleService;

    @Autowired
    private LoginService loginService;

    /*
      授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection)
    {
        User user = ShiroUtils.getSysUser();
        Set<String> roles = new HashSet<>();
        Set<String> menus = new HashSet<>();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        //进行添加角色  权限
        if (user.isAdmin())
        {
            info.addRole("admin");
            info.addStringPermission("*:*:*");
        } else
        {
            roles = roleService.selectRoleKeys(user.getUserId());
            menus = menuService.selectPermsByUserId(user.getUserId());
            info.setRoles(roles);
            info.setStringPermissions(menus);
        }
        return info;
    }

    /**
     * 登陆认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException
    {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String username = token.getUsername();
        String password = "";
        if (token.getPassword() != null)
            password = String.valueOf(token.getPassword());

        User user = null;

        try
        {
            user = loginService.login(username, password);
        } catch (CaptchaException e)
        {
            throw new AuthenticationException(e.getMessage(), e);
        } catch (UserNotExistsException e)
        {
            throw new UnknownAccountException(e.getMessage(), e);
        } catch (UserPasswordNotMatchException e)
        {
            throw new IncorrectCredentialsException(e.getMessage(), e);
        } catch (UserPasswordRetryLimitExceedException e)
        {
            throw new ExcessiveAttemptsException(e.getMessage(), e);
        } catch (UserBlockedException e)
        {
            throw new LockedAccountException(e.getMessage(), e);
        } catch (RoleBlockedException e)
        {
            throw new LockedAccountException(e.getMessage(), e);
        } catch (Exception e)
        {
            log.info("对用户[" + username + "]进行登录验证..验证未通过{}", e.getMessage());
            throw new AuthenticationException(e.getMessage(), e);
        }
        SimpleAuthenticationInfo info=new SimpleAuthenticationInfo(user,password,getName());
        return info;

    }

    /**
     * 清空缓存权限
     */
    public void clearCachedAuthorizationInfo()
    {
        this.clearCachedAuthenticationInfo(SecurityUtils.getSubject().getPrincipals());
    }
}

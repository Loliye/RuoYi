package com.mikufans.controller.system.user;


import com.mikufans.common.web.AjaxResult;
import com.mikufans.config.shiro.service.RegisterService;
import com.mikufans.controller.BaseController;
import com.mikufans.domain.user.User;
import com.mikufans.service.config.IConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.mikufans.common.web.AjaxResult.error;
import static com.mikufans.common.web.AjaxResult.success;

/**
 * 注册验证
 */
@Controller
public class RegisterController extends BaseController
{
    @Autowired
    private RegisterService registerService;

    @Autowired
    private IConfigService configService;

    @GetMapping("/register")
    public String register()
    {
        return "register";
    }

    @PostMapping("/register")
    @ResponseBody
    public AjaxResult ajaxRegister(User user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}

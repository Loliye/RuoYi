package com.mikufans.controller.system.user;

import com.mikufans.common.util.ShiroUtils;
import com.mikufans.config.RuoYiConfig;
import com.mikufans.domain.menu.Menu;
import com.mikufans.domain.user.User;
import com.mikufans.service.config.IConfigService;
import com.mikufans.service.menu.IMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Controller
public class IndexController
{
    @Autowired
    private IMenuService menuService;

    @Autowired
    private IConfigService configService;

    @Autowired
    private RuoYiConfig ruoYiConfig;

    //系统首页
    @GetMapping("/index")
    public String index(ModelMap modelMap)
    {
        // 取身份信息
        User user = ShiroUtils.getSysUser();
        // 根据用户id取出菜单
        List<Menu> menus = menuService.selectMenusByUser(user);
        modelMap.put("menus", menus);
        modelMap.put("user", user);
        modelMap.put("sideTheme", configService.selectConfigByKey("sys.index.sideTheme"));
        modelMap.put("skinName", configService.selectConfigByKey("sys.index.skinName"));
        modelMap.put("copyrightYear", ruoYiConfig.getCopyrightYear());
        modelMap.put("demoEnabled", ruoYiConfig.isDemoEnabled());
        return "index";
    }

    // 切换主题
    @GetMapping("/system/switchSkin")
    public String switchSkin(ModelMap modelMap)
    {
        return "skin";
    }

    // 系统介绍
    @GetMapping("/system/main")
    public String main(ModelMap modelMap)
    {
        modelMap.put("version", ruoYiConfig.getVersion());
        return "main";
    }
}

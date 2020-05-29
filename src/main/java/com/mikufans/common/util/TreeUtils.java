package com.mikufans.common.util;

import com.mikufans.domain.menu.Menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TreeUtils
{
    /**
     * 根据父节点的id获取所有子节点
     *
     * @param list     分类表
     * @param parentId 传入来的父节点id
     * @return
     */
    public static List<Menu> getChildPerms(List<Menu> list, int parentId)
    {
        List<Menu> returnList = new ArrayList<>();
        for (Iterator<Menu> iterator = list.iterator(); iterator.hasNext(); )
        {
            Menu t = iterator.next();
            if (t.getParentId() == parentId)
            {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    private static void recursionFn(List<Menu> list, Menu t)
    {
        List<Menu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (Menu tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                Iterator<Menu> iterator = childList.iterator();
                while (iterator.hasNext())
                {
                    Menu menu = iterator.next();
                    recursionFn(list, menu);
                }
            }
        }
    }

    /**
     * 获取子节点列表
     */
    private static List<Menu> getChildList(List<Menu> list, Menu t)
    {
        List<Menu> tList = new ArrayList<>();
        Iterator<Menu> iterator = list.iterator();
        while (iterator.hasNext())
        {
            Menu next = iterator.next();
            if (next.getParentId().longValue() == t.getMenuId().longValue())
                tList.add(next);
        }
        return tList;
    }

    private static boolean hasChild(List<Menu> list, Menu t)
    {
        return getChildList(list, t).size() > 0;
    }
}

package com.mikufans.common.util;

import com.mikufans.common.constant.Constants;
import com.mikufans.domain.dict.DictData;

import java.util.List;

/**
 * 字典工具集
 */
public class DictUtils
{
    /**
     * 设置字典缓存
     *
     * @param key       参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<DictData> dictDatas)
    {
        CacheUtils.put(getCacheName(), getCacheKey(key), dictDatas);
    }

    /**
     * 获取字典缓存
     *
     * @param key
     * @return
     */
    public static List<DictData> getDictCache(String key)
    {
        Object cacheObj = CacheUtils.get(getCacheName(), getCacheKey(key));
        if (StringUtils.isNotNull(cacheObj))
        {
            List<DictData> dictDatas=StringUtils.cast(cacheObj);
            return dictDatas;
        }
        return null;
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        CacheUtils.removeAll(getCacheName());
    }


    /**
     * 获取cache name
     *
     * @return 缓存名
     */
    public static String getCacheName()
    {
        return Constants.SYS_DICT_CACHE;
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey)
    {
        return Constants.SYS_DICT_KEY + configKey;
    }

}

package com.eriwang.mbspro_updater.utils;

import java.util.Map;

public class MapUtils
{
    public static <K, V> V safeGet(Map<K, V> keyToValue, K key)
    {
        V value = keyToValue.get(key);
        ProdAssert.notNull(value);
        return value;
    }
}

package com.eriwang.mbspro_updater.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils
{
    @SafeVarargs
    public static <T> List<T> concatLists(List<T> ... lists)
    {
        List<T> concattedList = new ArrayList<>();
        for (List<T> l : lists)
        {
            concattedList.addAll(l);
        }
        return concattedList;
    }
}

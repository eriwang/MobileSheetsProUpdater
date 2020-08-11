package com.eriwang.mbspro_updater.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils
{
    public static void showShortToast(Context context, String message, Object... args)
    {
        Toast.makeText(context, String.format(message, args), Toast.LENGTH_SHORT).show();
    }
}

package com.eriwang.mbspro_updater.utils;

public class ProdAssert
{
    public static void prodAssert(boolean expression, String message, Object... args)
    {
        if (!expression)
        {
            throw new RuntimeException(String.format(message, args));
        }
    }

    public static void notNull(Object object)
    {
        // Interestingly writing it like this instead of calling prodAssert makes the Android Studio warning go away.
        if (object == null)
        {
            throw new RuntimeException("Object expected to be not null, but is null");
        }
    }
}

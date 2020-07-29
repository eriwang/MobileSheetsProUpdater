package com.eriwang.mbspro_updater.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class TaskUtils
{
    public static <T> Task<T> execute(Executor executor, Callable<T> c)
    {
        return Tasks.call(executor, FunctionWrapper.createTyped(c));
    }

    public static Task<Void> execute(Executor executor, FunctionWrapper.VoidFunction f)
    {
        return Tasks.call(executor, FunctionWrapper.createVoid(f));
    }
}

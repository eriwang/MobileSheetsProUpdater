package com.eriwang.mbspro_updater.utils;

import java.util.concurrent.Callable;

public class FunctionWrapper<T> implements Callable<T>
{
    private Callable<T> mCallable;

    public interface VoidFunction
    {
        void call() throws Exception;
    }

    public static <T> FunctionWrapper<T> createTyped(Callable<T> callable)
    {
        return new FunctionWrapper<>(callable);
    }

    // I can't make a generic Callable with primitive type void, this is a wrapper so I don't need to declare a
    // function returning class type Void everywhere.
    public static FunctionWrapper<Void> createVoid(VoidFunction function)
    {
        return new FunctionWrapper<>(new VoidCallable(function));
    }

    // I'm fine with failing hard on exceptions with a RuntimeException instead of adding a bunch of other handling
    // for checked exceptions. Worth noting though you do lose the ability to capture specific exceptions.
    @Override
    public T call()
    {
        try
        {
            return mCallable.call();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private FunctionWrapper(Callable<T> callable)
    {
        mCallable = callable;
    }
}

class VoidCallable implements Callable<Void>
{
    private FunctionWrapper.VoidFunction mFunction;

    public VoidCallable(FunctionWrapper.VoidFunction function)
    {
        mFunction = function;
    }

    @Override
    public Void call() throws Exception
    {
        mFunction.call();
        return null;
    }
}
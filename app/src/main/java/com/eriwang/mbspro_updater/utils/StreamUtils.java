package com.eriwang.mbspro_updater.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils
{
    public static void writeInputToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException
    {
        int readBytes;
        byte[] buffer = new byte[2048];
        while ((readBytes = inputStream.read(buffer)) != -1)
        {
            outputStream.write(buffer, 0, readBytes);
        }
    }
}

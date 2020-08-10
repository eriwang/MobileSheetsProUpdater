package com.eriwang.mbspro_updater.sync;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SongSyncJobServiceLogger
{
    public static final String LOG_FILE = "sync_service.log";

    private static final String TAG = "SyncService";

    private DateFormat mDateFormat;
    private ArrayList<String> mLogLines;

    public SongSyncJobServiceLogger()
    {
        mDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:dd.SSS", Locale.US);
        mLogLines = new ArrayList<>();
    }

    public void log(String message, Object... args)
    {
        String timestampedMessage = mDateFormat.format(new Date()) + " " + String.format(message, args);
        Log.d(TAG, timestampedMessage);
        mLogLines.add(timestampedMessage);
    }

    public void logException(Exception e, String message, Object... args)
    {
        String timestampedMessage = getTimestampedMessage(message, args);
        Log.e(TAG, timestampedMessage, e);
        mLogLines.add(timestampedMessage);

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        mLogLines.add(getTimestampedMessage(stringWriter.toString()));
    }

    public void flushLogsToAppFile(Context context)
    {
        try (FileOutputStream output = context.openFileOutput(LOG_FILE, Context.MODE_PRIVATE))
        {
            for (String logLine : mLogLines)
            {
                output.write(logLine.getBytes());
                output.write('\n');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static List<String> readAppFileLog(Context context)
    {
        try (FileInputStream input = context.openFileInput(LOG_FILE))
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            ArrayList<String> results = new ArrayList<>();
            while (reader.ready())
            {
                results.add(reader.readLine());
            }
            return results;
        }
        catch (FileNotFoundException e)
        {
            return new ArrayList<>();  // expected behavior if this hasn't ever run
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String getTimestampedMessage(String message, Object... args)
    {
        return mDateFormat.format(new Date()) + " " + String.format(message, args);
    }
}

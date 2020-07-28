package com.eriwang.mbspro_updater.sync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.eriwang.mbspro_updater.utils.ProdAssert;

public class SongSyncService extends Service
{
    private static final String TAG = "SongSyncService";
    private static final int SYNC_START_WAIT_TIME = 1000;
    private static final int SYNC_INTERVAL_MILLIS = 1000;
    private static final int REQ_CODE_SONG_SYNC_JOB = 1;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        ProdAssert.notNull(alarmManager);

        Intent syncIntent = new Intent(this, SongSyncJob.class);
//                .putExtra("saveLocationUri", saveLocationUri)
//                .putExtra("driveDirectoryId", TEST_FOLDER_ROOT_ID);

        PendingIntent currentPendingIntent = PendingIntent.getService(this, REQ_CODE_SONG_SYNC_JOB, syncIntent,
                PendingIntent.FLAG_NO_CREATE);
        if (currentPendingIntent != null)
        {
            alarmManager.cancel(currentPendingIntent);
        }

        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, REQ_CODE_SONG_SYNC_JOB, syncIntent, 0);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + SYNC_START_WAIT_TIME, SYNC_INTERVAL_MILLIS, alarmIntent);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "Service destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}

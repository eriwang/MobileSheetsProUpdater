package com.eriwang.mbspro_updater.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.eriwang.mbspro_updater.R;

public class SongSyncJobService extends JobService
{
    private static int notifCount = 0;

    @Override
    public boolean onStartJob(JobParameters jobParameters)
    {
        // TODO: should the sync run in the background?
        Log.d("Receiver", "hello");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel")
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle("Hello")
                .setContentText("Hello")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notifCount++, builder.build());
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters)
    {
        return false;
    }
}

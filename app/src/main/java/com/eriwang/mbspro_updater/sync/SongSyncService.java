package com.eriwang.mbspro_updater.sync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveWrapper;

import java.util.concurrent.Executor;

public class SongSyncService extends BroadcastReceiver
{
    private static String CHANNEL_ID = "channel";
    private static int notifCount = 0;
    private Executor mExecutor;
    private DriveWrapper mDrive;
    private SongSyncManager mSongSyncManager;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("Receiver", "hello");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel")
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle("Hello")
                .setContentText("Hello")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notifCount++, builder.build());
    }
}

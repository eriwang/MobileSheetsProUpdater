package com.eriwang.mbspro_updater.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.utils.TaskUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SongSyncJobService extends JobService
{
    public static final int PERIODIC_JOB_ID = 1;
    public static final int FORCE_SYNC_JOB_ID = 2;
    public static final String MBS_PRO_DATA_DIR = "MBS_PRO_DATA_DIR";
    public static final String DRIVE_FOLDER_ID = "DRIVE_FOLDER_ID";

    private static String TAG = "SongSyncJobService";

    // TODO: clean up notifications, one builder class per channel that just manages everything
    private static int notifId = 0;

    private Executor mExecutor;
    private DriveWrapper mDrive;
    private SongSyncManager mSongSyncManager;

    @Override
    public void onCreate()
    {
        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = new DriveWrapper();
        mSongSyncManager = new SongSyncManager(mDrive, this);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters)
    {
        mDrive.setCredentialFromContextAndInitialize(this);

        String mbsProDataDir = jobParameters.getExtras().getString(MBS_PRO_DATA_DIR);
        String driveFolderId = jobParameters.getExtras().getString(DRIVE_FOLDER_ID);

        Log.d(TAG, "Running job");
        TaskUtils.execute(mExecutor, () -> {
            mSongSyncManager.syncMbsProWithDrive(driveFolderId, Uri.parse(mbsProDataDir));
            Log.d(TAG, "Job complete");
            jobFinished(jobParameters, false);
        })
            .addOnFailureListener(exception -> {
                Log.e("TAG", "failed", exception);

                // TODO: on click open app and show view with error
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("MBS Pro Updater Sync Failure")
                    .setContentText("Sync job failed, see app for more details.");
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(notifId++, builder.build());
            });

        return true;  // job still running
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters)
    {
        // No-op here for now for simplicity. Some operations (i.e. writes) are operations I definitely don't want to
        // interrupt in the middle of, but it's fine if we stop in between those operations, as a future sync will
        // just fix it again.
        return false;  // Don't reschedule
    }
}

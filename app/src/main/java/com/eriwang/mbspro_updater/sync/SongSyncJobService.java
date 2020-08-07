package com.eriwang.mbspro_updater.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.util.Log;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.utils.TaskUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SongSyncJobService extends JobService
{
    public static final int JOB_ID = 1;
    public static final String MBS_PRO_DATA_DIR = "MBS_PRO_DATA_DIR";
    public static final String DRIVE_FOLDER_ID = "DRIVE_FOLDER_ID";

    private static String TAG = "SongSyncJobService";

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
        });

        return true;  // job still running
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters)
    {
        // No-op here for now for simplicity.
        return false;  // Don't reschedule
    }
}

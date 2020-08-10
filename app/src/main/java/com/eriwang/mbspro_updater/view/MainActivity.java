package com.eriwang.mbspro_updater.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.sync.SongSyncJobService;
import com.eriwang.mbspro_updater.sync.SongSyncJobServiceLogger;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.eriwang.mbspro_updater.utils.TaskUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private static final int REQ_CODE_SIGN_IN = 1;

    // Note that Android clamps the interval to 15 minutes (possibly less on some versions of Android), and forces a
    // post-job "flex" interval of 5 minutes.
    private static final int BG_SYNC_INTERVAL_MILLIS = 5000;

    private Executor mExecutor;

    // TODO: I should probably have fragments for a few different states (and manage them when they're resolved):
    //  - Not signed in: Button for users to give permissions
    //  - Signed in, folders (either one) not selected: Tell users to go to settings and fix it
    //  - Signed in, folders selected: Regular view that allows syncing
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mExecutor = Executors.newSingleThreadExecutor();

        TaskUtils.execute(mExecutor, () -> {
            List<String> lastJobLogLines = SongSyncJobServiceLogger.readAppFileLog(this);
            for (String logLine : lastJobLogLines)
            {
                Log.d(TAG, logLine);
            }
        });

        // TODO: feedback of some sort, e.g. toasts
        findViewById(R.id.force_sync_now).setOnClickListener(view -> startSyncJob(false));

        findViewById(R.id.schedule_sync_in_background).setOnClickListener(view -> startSyncJob(true));

        findViewById(R.id.stop_sync).setOnClickListener(view -> {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(SongSyncJobService.PERIODIC_JOB_ID);
        });

        findViewById(R.id.open_settings).setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel("channel", "channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // TODO: should be more user friendly in the future
        requestSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result)
    {
        switch (requestCode)
        {
        case REQ_CODE_SIGN_IN:
            handleSignInResult(resultCode, result);
            break;

        default:
            throw new RuntimeException(String.format("Unknown request code %d", requestCode));
        }

        super.onActivityResult(requestCode, resultCode, result);
    }

    private void requestSignIn()
    {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        startActivityForResult(client.getSignInIntent(), REQ_CODE_SIGN_IN);
    }

    private void handleSignInResult(int resultCode, Intent result)
    {
        // TODO: actual error handling
        if (resultCode != Activity.RESULT_OK || result == null)
        {
            return;
        }

        GoogleSignIn.getSignedInAccountFromIntent(result)
                .onSuccessTask(googleSignInAccount -> TaskUtils.execute(mExecutor, () -> {
                    ProdAssert.notNull(googleSignInAccount);
                    Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());

                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList(DriveScopes.DRIVE));
                    credential.setSelectedAccount(googleSignInAccount.getAccount());
                }))
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    // I believe having multiple jobs running at the same time is actually safe (though that's assuming that
    // concurrently writing to the same file isn't possible), as the final result of the two jobs shouldn't be
    // different. That being said, it would be nice to clean this up and get rid of duplicate work at some point.
    private void startSyncJob(boolean isPeriodic)
    {
        // Something's wrong if the user has gotten far enough to start a sync job but the preferences aren't set.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String mbsProDataDir = sharedPreferences.getString(SettingsActivity.MBSPRO_FOLDER_URI_KEY, null);
        ProdAssert.notNull(mbsProDataDir);

        String driveFolderId = sharedPreferences.getString(SettingsActivity.DRIVE_FOLDER_ID_KEY, null);
        ProdAssert.notNull(driveFolderId);

        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString(SongSyncJobService.MBS_PRO_DATA_DIR, mbsProDataDir);
        persistableBundle.putString(SongSyncJobService.DRIVE_FOLDER_ID, driveFolderId);

        int jobId = (isPeriodic) ? SongSyncJobService.PERIODIC_JOB_ID : SongSyncJobService.FORCE_SYNC_JOB_ID;
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(jobId, new ComponentName(this, SongSyncJobService.class))
                .setExtras(persistableBundle)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        if (isPeriodic)
        {
            jobInfoBuilder.setPeriodic(BG_SYNC_INTERVAL_MILLIS);
        }

        jobScheduler.schedule(jobInfoBuilder.build());
    }
}
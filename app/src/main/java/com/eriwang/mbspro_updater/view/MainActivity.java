package com.eriwang.mbspro_updater.view;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.sync.SongSyncJobService;
import com.eriwang.mbspro_updater.sync.SongSyncManager;
import com.eriwang.mbspro_updater.utils.FunctionWrapper;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private static final int REQ_CODE_SIGN_IN = 1;
    private static final int REQ_CODE_FORCE_SYNC = 2;
    private static final int REQ_CODE_START_BG_SYNC = 3;

    private static final int JOB_ID_BG_SYNC = 1;

    // Note that Android clamps the interval to 15 minutes (possibly less on some versions of Android), and forces a
    // post-job "flex" interval of 5 minutes.
    private static final int BG_SYNC_INTERVAL_MILLIS = 5000;

    private static final String TEST_FOLDER_ROOT_ID = "11HTp4Y8liv9Oc0Sof0bxvlsGSLmQAvl4";

    private Executor mExecutor;
    private DriveWrapper mDrive;
    private SongSyncManager mSongSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = new DriveWrapper();
        mSongSyncManager = new SongSyncManager(mDrive, getApplicationContext());

        // TODO: rethink what's a user-friendly and more robust way to do this (e.g. what to save)
        findViewById(R.id.force_sync_now).setOnClickListener(view -> {
            // TODO: should explicitly tell user what they should be selecting. also sanity check after that the db
            //       is there
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // TODO: specify initial URI?
            startActivityForResult(intent, REQ_CODE_FORCE_SYNC);
        });
        findViewById(R.id.schedule_sync_in_background).setOnClickListener(view -> {
            // TODO: should explicitly tell user what they should be selecting. also sanity check after that the db
            //       is there
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // TODO: specify initial URI?
            startActivityForResult(intent, REQ_CODE_START_BG_SYNC);
        });
        findViewById(R.id.stop_sync).setOnClickListener(view -> {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(JOB_ID_BG_SYNC);
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
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        switch (requestCode)
        {
        case REQ_CODE_SIGN_IN:
            handleSignInResult(resultCode, resultData);
            break;

        case REQ_CODE_FORCE_SYNC:
            handleSyncTest(resultCode, resultData);
            break;

        case REQ_CODE_START_BG_SYNC:
            handleStartBackgroundSync(resultCode, resultData);
            break;

        default:
            throw new RuntimeException(String.format("Unknown request code %d", requestCode));
        }

        super.onActivityResult(requestCode, resultCode, resultData);
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
                .onSuccessTask(googleSignInAccount -> taskExecuteVoid(() -> {
                    ProdAssert.notNull(googleSignInAccount);
                    Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());

                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList(DriveScopes.DRIVE));
                    credential.setSelectedAccount(googleSignInAccount.getAccount());
                    mDrive.setCredentialAndInitialize(credential);
                }))
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void handleSyncTest(int resultCode, Intent result)
    {
        // TODO: actual error handling
        if (resultCode != Activity.RESULT_OK || result == null)
        {
            return;
        }

        final Uri saveLocationUri = result.getData();
        ProdAssert.notNull(saveLocationUri);

        taskExecuteVoid(() -> mSongSyncManager.syncMbsProWithDrive(TEST_FOLDER_ROOT_ID, saveLocationUri))
                .addOnFailureListener(exception -> Log.e(TAG, "Sync failed", exception));
    }

    private void handleStartBackgroundSync(int resultCode, Intent result)
    {
        // TODO: actual error handling
        if (resultCode != Activity.RESULT_OK || result == null)
        {
            return;
        }

        final Uri saveLocationUri = result.getData();
        ProdAssert.notNull(saveLocationUri);

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID_BG_SYNC, new ComponentName(this, SongSyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(BG_SYNC_INTERVAL_MILLIS)
                .build();
        jobScheduler.schedule(jobInfo);
    }

    private <T> Task<T> taskExecute(Callable<T> c)
    {
        return Tasks.call(mExecutor, FunctionWrapper.createTyped(c));
    }

    private Task<Void> taskExecuteVoid(FunctionWrapper.VoidFunction f)
    {
        return Tasks.call(mExecutor, FunctionWrapper.createVoid(f));
    }
}
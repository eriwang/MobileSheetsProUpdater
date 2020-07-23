package com.eriwang.mbspro_updater.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.eriwang.mbspro_updater.R;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.drive.SongFinder;
import com.eriwang.mbspro_updater.mbspro.MbsProDatabaseManager;
import com.eriwang.mbspro_updater.mbspro.SongFileManager;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.util.Collections;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private static final int REQ_CODE_SIGN_IN = 1;
    private static final int REQ_CODE_DOWNLOAD_DRIVE_SONGS = 2;
    private static final int REQ_CODE_DB_TEST = 3;

    private static final String TEST_FOLDER_ROOT_ID = "11HTp4Y8liv9Oc0Sof0bxvlsGSLmQAvl4";

    private DriveWrapper mDrive;
    private SongFinder mSongFinder;
    private SongFileManager mSongFileManager;
    private MbsProDatabaseManager mMbsProDatabaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrive = new DriveWrapper();
        mSongFinder = new SongFinder(mDrive);
        mSongFileManager = new SongFileManager(mDrive, getApplicationContext());
        mMbsProDatabaseManager = new MbsProDatabaseManager(getContentResolver());

        findViewById(R.id.copy_test).setOnClickListener(view -> {
            // TODO: should explicitly tell user what they should be selecting. also sanity check after that the db
            //       is there
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // TODO: specify initial URI?
            startActivityForResult(intent, REQ_CODE_DOWNLOAD_DRIVE_SONGS);
        });
        findViewById(R.id.db_test).setOnClickListener(view -> {
            // TODO: remove
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_CODE_DB_TEST);
        });

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

        case REQ_CODE_DOWNLOAD_DRIVE_SONGS:
            handleDownloadDriveSongsResult(resultCode, resultData);
            break;

        case REQ_CODE_DB_TEST:
            handleDbTest(resultCode, resultData);
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
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList(DriveScopes.DRIVE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    mDrive.setCredentialAndInitialize(credential);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void handleDownloadDriveSongsResult(int resultCode, Intent result)
    {
        // TODO: actual error handling
        if (resultCode != Activity.RESULT_OK || result == null)
        {
            return;
        }

        final Uri saveLocationUri = result.getData();
        ProdAssert.notNull(saveLocationUri);

        mSongFinder.findSongsRecursivelyInDirectory(TEST_FOLDER_ROOT_ID)
                .addOnSuccessListener(songs -> {
                    mSongFileManager.downloadSongsToDirectory(songs, saveLocationUri)
                            .addOnFailureListener(exception -> {
                                    Log.d(TAG, Log.getStackTraceString(exception));
                                });
                })
                .addOnFailureListener(exception -> {
                    Log.d(TAG, Log.getStackTraceString(exception));
                });
    }

    private void handleDbTest(int resultCode, Intent result)
    {
        if (resultCode != Activity.RESULT_OK || result == null)
        {
            return;
        }

        final Uri saveLocationUri = result.getData();
        ProdAssert.notNull(saveLocationUri);

        Uri mbsProDbUri = null;
        for (DocumentFile file : DocumentFile.fromTreeUri(this, saveLocationUri).listFiles())
        {
            if (file.getName().equals("mobilesheets.db"))
            {
                mbsProDbUri = file.getUri();
            }
        }
        ProdAssert.notNull(mbsProDbUri);
        mMbsProDatabaseManager.setDbUri(mbsProDbUri);
        mSongFinder.findSongsRecursivelyInDirectory(TEST_FOLDER_ROOT_ID)
                .addOnSuccessListener(songs -> {
                    try
                    {
                        mMbsProDatabaseManager.insertSongsIntoDb(songs);
                    }
                    catch (IOException exception)
                    {
                        Log.e(TAG, "oops", exception);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.d(TAG, Log.getStackTraceString(exception));
                });
    }
}
package com.eriwang.mbspro_updater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.eriwang.mbspro_updater.drive.Song;
import com.eriwang.mbspro_updater.drive.SongFinder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOC_TREE = 2;
    private static final int REQUEST_CODE_COPY_TEST = 3;

    private static final String TEST_FOLDER_ROOT_ID = "11HTp4Y8liv9Oc0Sof0bxvlsGSLmQAvl4";

    private SongFinder mSongFinder;
    private Song mTestSong;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSongFinder = new SongFinder();
        mTestSong = null;

        findViewById(R.id.log_drive_files_btn).setOnClickListener(view -> {
            mSongFinder.findSongsRecursivelyInDirectory(TEST_FOLDER_ROOT_ID)
                    .addOnSuccessListener(songs -> {
                        for (Song song : songs)
                        {
                            Log.d(TAG, String.format("%s: %d pdfs %d audio", song.mName, song.mPdfFiles.size(),
                                    song.mAudioFiles.size()));
                        }
                    })
                    .addOnFailureListener(exception -> {
                        Log.d(TAG, Log.getStackTraceString(exception));
                    });
        });
        findViewById(R.id.ls_mbspro_dir).setOnClickListener(view -> {
            // TODO: should explicitly tell user what they should be selecting. also sanity check after that the db
            //       is there
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // TODO: specify initial URI?
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOC_TREE);
        });
        findViewById(R.id.copy_test).setOnClickListener(view -> {
            mSongFinder.findSongsRecursivelyInDirectory(TEST_FOLDER_ROOT_ID)
                    .addOnSuccessListener(songs -> {
                        for (Song song : songs)
                        {
                            if (song.mName.equals("Test"))
                            {
                                mTestSong = song;
                            }
                        }

                        // TODO: should explicitly tell user what they should be selecting. also sanity check after that the db
                        //       is there
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        // TODO: specify initial URI?
                        startActivityForResult(intent, REQUEST_CODE_COPY_TEST);
                    })
                    .addOnFailureListener(exception -> {
                        Log.d(TAG, Log.getStackTraceString(exception));
                    });
        });

        // TODO: should be more user friendly in the future
        requestSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        switch (requestCode)
        {
        case REQUEST_CODE_SIGN_IN:
            // TODO: errors
            if (resultCode == Activity.RESULT_OK && resultData != null)
            {
                handleSignInResult(resultData);
            }
            break;

        case REQUEST_CODE_OPEN_DOC_TREE:
            // TODO: errors
            if (resultCode == Activity.RESULT_OK && resultData != null)
            {
                handleOpenDocTreeResult(resultData);
            }
            break;

        case REQUEST_CODE_COPY_TEST:
            // TODO: errors
            if (resultCode == Activity.RESULT_OK && resultData != null)
            {
                handleCopyTest(resultData);
            }
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

        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void handleSignInResult(Intent result)
    {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList(DriveScopes.DRIVE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    mSongFinder.setCredentialAndInitializeDrive(credential);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void handleOpenDocTreeResult(Intent result)
    {
        final Uri uri = result.getData();
        if (uri == null)
        {
            throw new RuntimeException("Null uri");
        }

        Log.d(TAG, uri.toString());
        DocumentFile docFile = DocumentFile.fromTreeUri(this, uri);
        for (DocumentFile f : docFile.listFiles())
        {
            Log.d(TAG, f.getName());
        }
    }

    private void handleCopyTest(Intent result)
    {
        final Uri uri = result.getData();
        if (uri == null)
        {
            throw new RuntimeException("Null uri");
        }

        DocumentFile targetDir = DocumentFile.fromTreeUri(this, uri);

        File drivePdfFile = mTestSong.mPdfFiles.get(0);
        DocumentFile newFile = targetDir.createFile(drivePdfFile.getMimeType(), drivePdfFile.getName());

        Tasks.call(mSongFinder.mExecutor, () -> {
            mSongFinder.mDrive.downloadFile(mTestSong.mPdfFiles.get(0).getId(),
                    getContentResolver().openOutputStream(newFile.getUri()));
            return 0;
        });
    }
}
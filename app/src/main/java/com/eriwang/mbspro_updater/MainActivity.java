package com.eriwang.mbspro_updater;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.Collections;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Main";
    private static final int REQUEST_CODE_SIGN_IN = 1;

    private static final String TEST_FOLDER_ROOT_ID = "11HTp4Y8liv9Oc0Sof0bxvlsGSLmQAvl4";

    private DriveWrapper mDrive;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrive = new DriveWrapper();

        findViewById(R.id.button).setOnClickListener(view -> {
            mDrive.recursivelyListDirectory(TEST_FOLDER_ROOT_ID).addOnSuccessListener(fileList -> {
                for (File file : fileList)
                {
                    final boolean isGenPdf = file.getMimeType().equals("application/pdf") &&
                            file.getName().endsWith(".gen.pdf");
                    final boolean isAudioFile = file.getMimeType().startsWith("audio");
                    if (isGenPdf || isAudioFile)
                    {
                        Log.d(TAG, String.format("name=%s, mimeType=%s", file.getName(), file.getMimeType()));
                    }
                }
            });
        });

        // TODO: should be more user friendly in the future
        requestSignIn();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        if (requestCode != REQUEST_CODE_SIGN_IN)
        {
            throw new RuntimeException(String.format("Unknown request code %d", requestCode));
        }

        // TODO: errors?
        if (resultCode == Activity.RESULT_OK && resultData != null)
        {
            handleSignInResult(resultData);
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
                    mDrive.initializeWithCredential(credential);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }
}
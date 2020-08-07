package com.eriwang.mbspro_updater.drive;

import android.content.Context;

import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class DriveWrapper
{
    private Drive mDrive;

    public DriveWrapper()
    {
        mDrive = null;
    }

    // TODO: this call is unsafe, as it throws if we're not signed in. Better solution would be to return false if
    //  authentication fails for any reason, and somewhere prompt the user to re-login.
    public void setCredentialFromContextAndInitialize(Context context)
    {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        ProdAssert.notNull(account);
        GoogleAccountCredential credential = GoogleAccountCredential
                .usingOAuth2(context, Collections.singletonList(DriveScopes.DRIVE))
                .setSelectedAccount(account.getAccount());

        mDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName("MobileSheetsPro Updater").build();
    }

    public File getFileMetadata(String fileId) throws IOException
    {
        validateInitialized();
        return mDrive.files().get(fileId)
                .setFields("id, name, mimeType, parents, modifiedTime")
                .execute();
    }

    public void downloadFile(String fileId, OutputStream outputStream) throws IOException
    {
        validateInitialized();
        mDrive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
    }

    public List<File> listDirectory(String directoryId) throws IOException
    {
        validateInitialized();
        return mDrive.files().list()
                .setQ(String.format("parents in '%s' and trashed = false", directoryId))
                .setFields("incompleteSearch, files/id, files/name, files/mimeType, files/parents, files/modifiedTime")
                .execute()
                .getFiles();
    }

    private void validateInitialized()
    {
        ProdAssert.prodAssert(mDrive != null, "Tried to make an API call without initializing");
    }
}

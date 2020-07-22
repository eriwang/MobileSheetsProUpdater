package com.eriwang.mbspro_updater.drive;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class DriveWrapper  // FIXME
{
    private Drive mDrive;

    public DriveWrapper()
    {
        mDrive = null;
    }

    public void setCredentialAndInitialize(GoogleAccountCredential credential)
    {
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
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mDrive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
//        return outputStream;
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
        if (mDrive == null)
        {
            throw new RuntimeException("Tried to make an API call without initializing");
        }
    }
}

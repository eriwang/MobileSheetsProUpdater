package com.eriwang.mbspro_updater.drive;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveWrapper
{
    private final Executor mExecutor;
    private Drive mDrive;

    public DriveWrapper()
    {
        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = null;
    }

    public void initializeWithCredential(GoogleAccountCredential credential)
    {
        mDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName("MobileSheetsPro Updater").build();
    }

    public Task<List<File>> recursivelyListDirectory(String directoryId)
    {
        validateInitialized();
        return Tasks.call(mExecutor, () -> recursivelyListDirectoryForeground(directoryId));
    }

    private void validateInitialized()
    {
        if (mDrive == null)
        {
            throw new RuntimeException("Tried to make an API call without initializing");
        }
    }

    private List<File> recursivelyListDirectoryForeground(String directoryId) throws IOException
    {
        ArrayList<File> allFiles = new ArrayList<>();
        for (File file : listDirectory(directoryId))
        {
            if (file.getMimeType().equals("application/vnd.google-apps.folder"))
            {
                allFiles.addAll(recursivelyListDirectoryForeground(file.getId()));
            }
            else
            {
                allFiles.add(file);
            }
        }

        return allFiles;
    }

    private List<File> listDirectory(String directoryId) throws IOException
    {
        return mDrive.files().list()
                .setQ(String.format("parents in '%s' and trashed = false", directoryId))
                .setFields("incompleteSearch, files/id, files/name, files/mimeType, files/parents, files/modifiedTime")
                .execute()
                .getFiles();
    }
}

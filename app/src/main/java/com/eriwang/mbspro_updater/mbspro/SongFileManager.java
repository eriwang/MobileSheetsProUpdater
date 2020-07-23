package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.drive.Song;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SongFileManager
{
    private static final String TAG = "SongFileManager";

    private final Executor mExecutor;
    private final DriveWrapper mDrive;
    private final Context mContext;

    public SongFileManager(DriveWrapper drive, Context context)
    {
        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = drive;
        mContext = context;
    }

    public Task<Void> downloadSongsToDirectory(List<Song> songs, Uri directoryUri)
    {
        return Tasks.call(mExecutor, () -> downloadSongsToDirectoryForeground(songs, directoryUri));
    }

    private Void downloadSongsToDirectoryForeground(List<Song> songs, Uri directoryUri) throws IOException
    {
        DocumentFile directory = DocumentFile.fromTreeUri(mContext, directoryUri);
        ProdAssert.notNull(directory);
        ProdAssert.prodAssert(directory.isDirectory(), "Document %s is not a directory", directory.getName());

        validateNoDuplicateSongNames(songs);

        // For simplicity, we do a clean download (i.e. clearing all folders) each time. To be changed in the future.
        for (DocumentFile file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                ProdAssert.prodAssert(file.delete(), "Deletion of dir %s failed", directory.getName());
            }
        }

        for (Song song : songs)
        {
            Log.d(TAG, String.format("Downloading files for song %s", song.mName));
            DocumentFile songDirectory = directory.createDirectory(song.mName);
            ProdAssert.notNull(songDirectory);

            for (File file : song.mPdfFiles)
            {
                downloadDriveFileToDirectory(songDirectory, file);
            }
            for (File file : song.mAudioFiles)
            {
                downloadDriveFileToDirectory(songDirectory, file);
            }
        }

        return null;
    }

    private void downloadDriveFileToDirectory(DocumentFile directory, File driveFile) throws IOException
    {
        DocumentFile newFile = directory.createFile(driveFile.getMimeType(), driveFile.getName());
        ProdAssert.notNull(newFile);

        mDrive.downloadFile(driveFile.getId(), mContext.getContentResolver().openOutputStream(newFile.getUri()));
    }

    private static void validateNoDuplicateSongNames(List<Song> songs)
    {
        Set<String> songNames = new HashSet<>();
        for (Song song : songs)
        {
            ProdAssert.prodAssert(songNames.add(song.mName), "Found duplicate song name %s", song.mName);
        }
    }
}

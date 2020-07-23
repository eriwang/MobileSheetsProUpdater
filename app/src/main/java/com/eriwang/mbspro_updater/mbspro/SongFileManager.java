package com.eriwang.mbspro_updater.mbspro;

import android.content.ContentResolver;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.drive.Song;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SongFileManager
{
    // TODO: Does it matter if there's multiple single thread executors?
    private final Executor mExecutor;
    private final DriveWrapper mDrive;
    // TODO: should this class hold the context resolver? any weird conditions with activity change that I'd need to
    //  worry about?

    public SongFileManager(DriveWrapper drive)
    {
        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = drive;
    }

    // TODO: work with multiple songs
    public Task<Void> downloadSongsToDirectory(Song song, DocumentFile directory, ContentResolver contentResolver)
    {
        ProdAssert.prodAssert(directory.isDirectory(), "Document %s is not a directory", directory.getName());
        return Tasks.call(mExecutor, () -> {
            downloadSongsToDirectoryForeground(song, directory, contentResolver);
            return null;
        });
    }

    // TODO: check duplicate song names?
    private void downloadSongsToDirectoryForeground(Song song, DocumentFile directory, ContentResolver contentResolver)
            throws IOException
    {
        // For simplicity, we do a clean download (i.e. clearing all folders) each time. To be changed in the future.
        for (DocumentFile file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                ProdAssert.prodAssert(file.delete(), "Deletion of dir %s failed", directory.getName());
            }
        }

        DocumentFile songDirectory = directory.createDirectory(song.mName);
        ProdAssert.notNull(songDirectory);

        for (File file : song.mPdfFiles)
        {
            downloadDriveFileToDirectory(songDirectory, file, contentResolver);
        }
        for (File file : song.mAudioFiles)
        {
            downloadDriveFileToDirectory(songDirectory, file, contentResolver);
        }
    }

    private void downloadDriveFileToDirectory(DocumentFile directory, File driveFile, ContentResolver contentResolver)
            throws IOException
    {
        DocumentFile newFile = directory.createFile(driveFile.getMimeType(), driveFile.getName());
        ProdAssert.notNull(newFile);

        mDrive.downloadFile(driveFile.getId(), contentResolver.openOutputStream(newFile.getUri()));
    }
}

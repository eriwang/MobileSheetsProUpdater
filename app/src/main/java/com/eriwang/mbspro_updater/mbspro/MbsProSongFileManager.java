package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.drive.DriveSong;
import com.eriwang.mbspro_updater.utils.DocumentFileUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MbsProSongFileManager
{
    private static final String TAG = "SongFileManager";

    private final DriveWrapper mDrive;
    private final Context mContext;

    public MbsProSongFileManager(DriveWrapper drive, Context context)
    {
        mDrive = drive;
        mContext = context;
    }

    public void downloadSongsToDirectory(List<DriveSong> driveSongs, Uri directoryUri) throws IOException
    {
        validateNoDuplicateSongNames(driveSongs);
        DocumentFile directory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri);

        // For simplicity, we do a clean download (i.e. clearing all folders) each time. To be changed in the future.
        for (DocumentFile file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                ProdAssert.prodAssert(file.delete(), "Deletion of dir %s failed", directory.getName());
            }
        }

        for (DriveSong driveSong : driveSongs)
        {
            Log.d(TAG, String.format("Downloading files for song %s", driveSong.mName));
            DocumentFile songDirectory = directory.createDirectory(driveSong.mName);
            ProdAssert.notNull(songDirectory);

            for (File file : driveSong.mPdfFiles)
            {
                downloadDriveFileToDirectory(songDirectory, file);
            }
            for (File file : driveSong.mAudioFiles)
            {
                downloadDriveFileToDirectory(songDirectory, file);
            }
        }
    }

    public Uri findMobileSheetsDbFile(Uri directoryUri)
    {
        final String MBS_DB_FILENAME = "mobilesheets.db";

        Uri mbsProDbUri = null;
        for (DocumentFile file : DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri).listFiles())
        {
            if (file.getName().equals(MBS_DB_FILENAME))
            {
                ProdAssert.prodAssert(mbsProDbUri == null, "Found multiple files named %s", MBS_DB_FILENAME);
                mbsProDbUri = file.getUri();
            }
        }
        return mbsProDbUri;
    }

    private void downloadDriveFileToDirectory(DocumentFile directory, File driveFile) throws IOException
    {
        DocumentFile newFile = directory.createFile(driveFile.getMimeType(), driveFile.getName());
        ProdAssert.notNull(newFile);

        mDrive.downloadFile(driveFile.getId(), mContext.getContentResolver().openOutputStream(newFile.getUri()));
    }

    private static void validateNoDuplicateSongNames(List<DriveSong> driveSongs)
    {
        Set<String> songNames = new HashSet<>();
        for (DriveSong driveSong : driveSongs)
        {
            ProdAssert.prodAssert(songNames.add(driveSong.mName), "Found duplicate song name %s", driveSong.mName);
        }
    }
}

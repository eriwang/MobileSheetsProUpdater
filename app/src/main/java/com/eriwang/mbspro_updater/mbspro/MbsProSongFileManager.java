package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.drive.DriveSong;
import com.eriwang.mbspro_updater.utils.DocumentFileUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.api.services.drive.model.File;

import java.io.IOException;

public class MbsProSongFileManager
{
    private static final String MBS_DB_FILENAME = "mobilesheets.db";

    private final DriveWrapper mDrive;
    private final Context mContext;
    private Uri mDirectoryUri;

    public MbsProSongFileManager(DriveWrapper drive, Context context)
    {
        mDrive = drive;
        mContext = context;
    }

    public void setDirectoryUri(Uri directoryUri)
    {
        mDirectoryUri = directoryUri;
    }

    public Uri findMobileSheetsDbFile()
    {
        validateDirectoryUriSet();

        Uri mbsProDbUri = null;
        for (DocumentFile file : DocumentFileUtils.safeDirectoryFromTreeUri(mContext, mDirectoryUri).listFiles())
        {
            if (file.getName().equals(MBS_DB_FILENAME))
            {
                ProdAssert.prodAssert(mbsProDbUri == null, "Found multiple files named %s", MBS_DB_FILENAME);
                mbsProDbUri = file.getUri();
            }
        }
        return mbsProDbUri;
    }

    public void downloadNewDriveSongToDirectory(DriveSong driveSong) throws IOException
    {
        validateDirectoryUriSet();

        DocumentFile songDirectory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, mDirectoryUri)
                .createDirectory(driveSong.mName);
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

    public void downloadDriveFileToDirectory(DocumentFile directory, File driveFile) throws IOException
    {
        DocumentFile newFile = directory.createFile(driveFile.getMimeType(), driveFile.getName());
        ProdAssert.notNull(newFile);

        mDrive.downloadFile(driveFile.getId(), mContext.getContentResolver().openOutputStream(newFile.getUri()));
    }

    public void downloadNewDriveFileToMbsProSongDirectory(File driveFile, MbsProSong mbsProSong)
            throws IOException
    {
        validateDirectoryUriSet();
        downloadDriveFileToDirectory(safeGetMbsProSongDirectory(mbsProSong), driveFile);
    }

    public void deleteMbsProSong(MbsProSong mbsProSong)
    {
        validateDirectoryUriSet();
        safeGetMbsProSongDirectory(mbsProSong).delete();
    }

    public void deleteMbsProFile(DocumentFile mbsProFile)
    {
        ProdAssert.prodAssert(mbsProFile.delete(), "Deletion of file %s failed", mbsProFile.getName());
    }

    public void updateMbsProFile(File driveFile, DocumentFile mbsProFile) throws IOException
    {
        // Interestingly, deleting the DocumentFile and then calling the download method gives a "Failed query" error
        // in the logs, likely related to https://stackoverflow.com/questions/35985321/check-that-a-documentfile-exists
        mDrive.downloadFile(driveFile.getId(), mContext.getContentResolver().openOutputStream(mbsProFile.getUri()));
    }

    private void validateDirectoryUriSet()
    {
        ProdAssert.notNull(mDirectoryUri);
    }

    private DocumentFile safeGetMbsProSongDirectory(MbsProSong mbsProSong)
    {
        DocumentFile songDirectory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, mDirectoryUri)
                .findFile(mbsProSong.mName);
        ProdAssert.notNull(songDirectory);
        ProdAssert.prodAssert(songDirectory.isDirectory(),
                "DocumentFile for song name %s is not a directory", mbsProSong.mName);
        return songDirectory;
    }
}

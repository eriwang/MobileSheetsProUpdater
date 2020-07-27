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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MbsProSongFileManager
{
    private static final String TAG = "SongFileManager";

    private final DriveWrapper mDrive;
    private final Context mContext;
    // TODO: directoryUri should live on this class

    public MbsProSongFileManager(DriveWrapper drive, Context context)
    {
        mDrive = drive;
        mContext = context;
    }

    // TODO: this method goes away
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
            downloadNewDriveSongToDirectory(driveSong, directoryUri);
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

    // TODO: rethink a lot of this stuff
    public void downloadNewDriveSongToDirectory(DriveSong driveSong, Uri directoryUri) throws IOException
    {
        Log.d(TAG, String.format("Downloading files for song %s", driveSong.mName));
        DocumentFile songDirectory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri)
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

    public void downloadNewDriveFileToMbsProSongDirectory(File driveFile, MbsProSong mbsProSong, Uri directoryUri)
            throws IOException
    {
        downloadDriveFileToDirectory(safeGetMbsProSongDirectory(mbsProSong, directoryUri), driveFile);
    }

    public void deleteMbsProSong(MbsProSong mbsProSong, Uri directoryUri)
    {
        safeGetMbsProSongDirectory(mbsProSong, directoryUri).delete();
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

    private DocumentFile safeGetMbsProSongDirectory(MbsProSong mbsProSong, Uri directoryUri)
    {
        DocumentFile songDirectory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri)
                .findFile(mbsProSong.mName);
        ProdAssert.notNull(songDirectory);
        ProdAssert.prodAssert(songDirectory.isDirectory(),
                "DocumentFile for song name %s is not a directory", mbsProSong.mName);
        return songDirectory;
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

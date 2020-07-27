package com.eriwang.mbspro_updater.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveSong;
import com.eriwang.mbspro_updater.drive.DriveSongFinder;
import com.eriwang.mbspro_updater.drive.DriveWrapper;
import com.eriwang.mbspro_updater.mbspro.MbsProDatabaseManager;
import com.eriwang.mbspro_updater.mbspro.MbsProSong;
import com.eriwang.mbspro_updater.mbspro.MbsProSongFileManager;
import com.eriwang.mbspro_updater.mbspro.MbsProSongFinder;
import com.eriwang.mbspro_updater.utils.ListUtils;
import com.eriwang.mbspro_updater.utils.MapUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.api.services.drive.model.File;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SongSyncManager
{
    private static final String TAG = "SongSyncManager";

    private final DriveSongFinder mDriveSongFinder;
    private final MbsProSongFinder mMbsProSongFinder;
    private final MbsProSongFileManager mMbsProSongFileManager;
    private final MbsProDatabaseManager mMbsProDatabaseManager;

    public SongSyncManager(DriveWrapper drive, Context context)
    {
        mDriveSongFinder = new DriveSongFinder(drive);
        mMbsProSongFinder = new MbsProSongFinder(context);
        mMbsProSongFileManager = new MbsProSongFileManager(drive, context);
        mMbsProDatabaseManager = new MbsProDatabaseManager(context.getContentResolver());
    }

    public void syncMbsProWithDrive(String driveDirectoryId, Uri mbsProDirectoryUri) throws IOException
    {
        // TODO: should be able to set both of these earlier in the object lifetime on an "initialization-like" step
        mMbsProSongFileManager.setDirectoryUri(mbsProDirectoryUri);
        mMbsProDatabaseManager.setDbUri(mMbsProSongFileManager.findMobileSheetsDbFile());

        Map<String, DriveSong> songNameToDriveSong = validateDriveSongsAndCreateMap(
                mDriveSongFinder.findSongsRecursivelyInDirectoryId(driveDirectoryId));
        Map<String, MbsProSong> songNameToMbsProSong = validateMbsProSongsAndCreateMap(
                mMbsProSongFinder.findSongsInDirectoryUri(mbsProDirectoryUri));
        Set<String> driveSongNames = songNameToDriveSong.keySet();
        Set<String> mbsProSongNames = songNameToMbsProSong.keySet();

        Log.d(TAG, "Syncing extra drive songs");
        Sets.SetView<String> extraDriveSongNames = Sets.difference(driveSongNames, mbsProSongNames);
        for (String extraDriveSongName : extraDriveSongNames)
        {
            Log.d(TAG, String.format("Found extra drive song %s, downloading", extraDriveSongName));
            DriveSong driveSong = MapUtils.safeGet(songNameToDriveSong, extraDriveSongName);
            mMbsProSongFileManager.downloadNewDriveSongToDirectory(driveSong);
        }

        Log.d(TAG, "Syncing extra MBS Pro songs");
        Sets.SetView<String> extraMbsProSongNames = Sets.difference(mbsProSongNames, driveSongNames);
        for (String extraMbsProSongName : extraMbsProSongNames)
        {
            Log.d(TAG, String.format("Found extra MBS Pro song %s, deleting", extraMbsProSongName));
            MbsProSong mbsProSong = MapUtils.safeGet(songNameToMbsProSong, extraMbsProSongName);
            mMbsProSongFileManager.deleteMbsProSong(mbsProSong);
        }

        Log.d(TAG, "Syncing common songs");
        Sets.SetView<String> commonSongNames = Sets.intersection(driveSongNames, mbsProSongNames);
        for (String songName : commonSongNames)
        {
            Log.d(TAG, String.format("Found common song %s", songName));
            DriveSong driveSong = MapUtils.safeGet(songNameToDriveSong, songName);
            MbsProSong mbsProSong = MapUtils.safeGet(songNameToMbsProSong, songName);

            Map<String, File> driveFilenameToFile = createMapUsingListKeys(
                    ListUtils.concatLists(driveSong.mAudioFiles, driveSong.mPdfFiles), File::getName);
            Map<String, DocumentFile> mbsProFilenameToFile = createMapUsingListKeys(
                    ListUtils.concatLists(mbsProSong.mAudioFiles, mbsProSong.mPdfFiles), DocumentFile::getName);
            Set<String> driveFilenames = driveFilenameToFile.keySet();
            Set<String> mbsProFilenames = mbsProFilenameToFile.keySet();

            Sets.SetView<String> extraDriveFilenames = Sets.difference(driveFilenames, mbsProFilenames);
            for (String extraDriveFilename : extraDriveFilenames)
            {
                Log.d(TAG, String.format("Found extra drive file %s, downloading", extraDriveFilename));
                mMbsProSongFileManager.downloadNewDriveFileToMbsProSongDirectory(
                        MapUtils.safeGet(driveFilenameToFile, extraDriveFilename), mbsProSong);
            }

            Sets.SetView<String> extraMbsProFilenames = Sets.difference(mbsProFilenames, driveFilenames);
            for (String extraMbsProFilename : extraMbsProFilenames)
            {
                Log.d(TAG, String.format("Found extra MBS Pro file %s, deleting", extraMbsProFilename));
                mMbsProSongFileManager.deleteMbsProFile(MapUtils.safeGet(mbsProFilenameToFile, extraMbsProFilename));
            }

            Sets.SetView<String> commonFilenames = Sets.intersection(driveFilenames, mbsProFilenames);
            for (String commonFilename : commonFilenames)
            {
                File driveFile = MapUtils.safeGet(driveFilenameToFile, commonFilename);
                DocumentFile mbsProFile = MapUtils.safeGet(mbsProFilenameToFile, commonFilename);
                if (driveFile.getModifiedTime().getValue() > mbsProFile.lastModified())
                {
                    Log.d(TAG, String.format("Found common file %s that needs to be updated, updating",
                            commonFilename));
                    mMbsProSongFileManager.updateMbsProFile(driveFile, mbsProFile);
                }
            }
        }

        Log.d(TAG, "Sync complete, inserting songs into MBS Pro DB");
        mMbsProDatabaseManager.insertSongsIntoDb(mMbsProSongFinder.findSongsInDirectoryUri(mbsProDirectoryUri));

        Log.d(TAG, "DB populating complete");
    }

    private static Map<String, DriveSong> validateDriveSongsAndCreateMap(List<DriveSong> driveSongs)
    {
        validateListNoDuplicateKeys(driveSongs, (driveSong) -> driveSong.mName);
        for (DriveSong s : driveSongs)
        {
            validateListNoDuplicateKeys(s.mAudioFiles, File::getName);
            validateListNoDuplicateKeys(s.mPdfFiles, File::getName);
        }
        return createMapUsingListKeys(driveSongs, (driveSong) -> driveSong.mName);
    }

    private static Map<String, MbsProSong> validateMbsProSongsAndCreateMap(List<MbsProSong> mbsProSongs)
    {
        validateListNoDuplicateKeys(mbsProSongs, (mbsProSong) -> mbsProSong.mName);
        for (MbsProSong s : mbsProSongs)
        {
            validateListNoDuplicateKeys(s.mAudioFiles, DocumentFile::getName);
            validateListNoDuplicateKeys(s.mPdfFiles, DocumentFile::getName);
        }
        return createMapUsingListKeys(mbsProSongs, (mbsProSong) -> mbsProSong.mName);
    }

    private static <T> void validateListNoDuplicateKeys(List<T> list, KeyGetter<T> keyGetter)
    {
        Set<String> keys = new HashSet<>();
        for (T item : list)
        {
            String key = keyGetter.getKey(item);
            ProdAssert.prodAssert(keys.add(key), "Duplicate item key %s found", key);
        }
    }

    private static <T> Map<String, T> createMapUsingListKeys(List<T> list, KeyGetter<T> keyGetter)
    {
        Map<String, T> keyToValue = new HashMap<>();
        for (T item : list)
        {
            keyToValue.put(keyGetter.getKey(item), item);
        }
        return keyToValue;
    }
}

interface KeyGetter<T>
{
    String getKey(T item);
}

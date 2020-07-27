package com.eriwang.mbspro_updater.sync;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveSong;
import com.eriwang.mbspro_updater.mbspro.MbsProSong;
import com.eriwang.mbspro_updater.utils.ListUtils;
import com.eriwang.mbspro_updater.utils.MapUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.google.api.services.drive.model.File;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SongSyncManager
{
    // medium term the main activity shouldn't need to know anything about drive (maybe besides auth) or mbs pro, just
    // this class.
    public static void syncMbsProWithDrive(List<DriveSong> driveSongs, List<MbsProSong> mbsProSongs)
    {
        Map<String, DriveSong> songNameToDriveSong = validateDriveSongsAndCreateMap(driveSongs);
        Map<String, MbsProSong> songNameToMbsProSong = validateMbsProSongsAndCreateMap(mbsProSongs);
        Set<String> driveSongNames = songNameToDriveSong.keySet();
        Set<String> mbsProSongNames = songNameToMbsProSong.keySet();

        // - find extra drive songs: these need to be downloaded
        Sets.SetView<String> extraDriveSongNames = Sets.difference(driveSongNames, mbsProSongNames);

        // - find extra mbs pro dir songs: these need to be deleted
        Sets.SetView<String> extraMbsProSongNames = Sets.difference(mbsProSongNames, driveSongNames);

        // - find matching songs:
        Sets.SetView<String> commonSongNames = Sets.intersection(driveSongNames, mbsProSongNames);
        for (String songName : commonSongNames)
        {
            DriveSong driveSong = MapUtils.safeGet(songNameToDriveSong, songName);
            MbsProSong mbsProSong = MapUtils.safeGet(songNameToMbsProSong, songName);

            Map<String, File> driveFilenameToFile = createMapUsingListKeys(
                    ListUtils.concatLists(driveSong.mAudioFiles, driveSong.mPdfFiles), File::getName);
            Map<String, DocumentFile> mbsProFilenameToFile = createMapUsingListKeys(
                    ListUtils.concatLists(mbsProSong.mAudioFiles, mbsProSong.mPdfFiles), DocumentFile::getName);
            Set<String> driveFilenames = driveFilenameToFile.keySet();
            Set<String> mbsProFilenames = mbsProFilenameToFile.keySet();

            // - if drive song has extra files, download them
            // to download, I need a fileId (which is on the driveFile), and a new file uri. or in filemanager, the
            // DocumentFile for the containing directory
            Sets.SetView<String> extraDriveFilenames = Sets.difference(driveFilenames, mbsProFilenames);

            // - if mbspro song has extra files, delete them
            // to delete, I need the documentFile
            Sets.SetView<String> extraMbsProFilenames = Sets.difference(mbsProFilenames, driveFilenames);

            // - if drive song file has later lastModified time than mbspro song file, redownload
            Sets.SetView<String> commonFilenames = Sets.intersection(driveFilenames, mbsProFilenames);
        }
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

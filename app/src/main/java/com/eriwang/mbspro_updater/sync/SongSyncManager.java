package com.eriwang.mbspro_updater.sync;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.drive.DriveSong;
import com.eriwang.mbspro_updater.mbspro.MbsProSong;
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
            Set<String> driveAudioFiles = validateAndCreateSetFromDriveFiles(
                    MapUtils.SafeGet(songNameToDriveSong, songName).mAudioFiles);
            Set<String> drivePdfFiles = validateAndCreateSetFromDriveFiles(
                    MapUtils.SafeGet(songNameToDriveSong, songName).mPdfFiles);
            Set<String> mbsProAudioFiles = validateAndCreateSetFromDocumentFiles(
                    MapUtils.SafeGet(songNameToMbsProSong, songName).mAudioFiles);
            Set<String> mbsProPdfFiles = validateAndCreateSetFromDocumentFiles(
                    MapUtils.SafeGet(songNameToMbsProSong, songName).mPdfs);

            // - if drive song has extra files, download them
            // to download, I need a fileId (which is on the driveFile), and a new file uri. or in filemanager, the
            // DocumentFile for the containing directory
            Sets.SetView<String> extraDriveAudioFiles = Sets.difference(driveAudioFiles, mbsProAudioFiles);
            Sets.SetView<String> extraDrivePdfFiles = Sets.difference(drivePdfFiles, mbsProPdfFiles);

            // - if mbspro song has extra files, delete them
            // to delete, I need the documentFile
            Sets.SetView<String> extraMbsProAudioFiles = Sets.difference(mbsProAudioFiles, driveAudioFiles);
            Sets.SetView<String> extraMbsProPdfFiles = Sets.difference(mbsProPdfFiles, drivePdfFiles);

            // - if drive song file has later lastModified time than mbspro song file, redownload
            Sets.SetView<String> commonAudioFiles = Sets.difference(driveAudioFiles, mbsProAudioFiles);
            Sets.SetView<String> commonPdfFiles = Sets.difference(drivePdfFiles, mbsProPdfFiles);
        }
    }

    private static Map<String, DriveSong> validateDriveSongsAndCreateMap(List<DriveSong> driveSongs)
    {
        validateAndCreateSetFromListKeys(driveSongs, (driveSong) -> driveSong.mName);

        HashMap<String, DriveSong> songNameToDriveSong = new HashMap<>();
        for (DriveSong s : driveSongs)
        {
            validateAndCreateSetFromDriveFiles(s.mAudioFiles);
            validateAndCreateSetFromDriveFiles(s.mPdfFiles);
            songNameToDriveSong.put(s.mName, s);
        }
        return songNameToDriveSong;
    }

    private static Map<String, MbsProSong> validateMbsProSongsAndCreateMap(List<MbsProSong> mbsProSongs)
    {
        validateAndCreateSetFromListKeys(mbsProSongs, (mbsProSong) -> mbsProSong.mName);

        HashMap<String, MbsProSong> songNameToMbsProSong = new HashMap<>();
        for (MbsProSong s : mbsProSongs)
        {
            validateAndCreateSetFromDocumentFiles(s.mAudioFiles);
            validateAndCreateSetFromDocumentFiles(s.mPdfs);
            songNameToMbsProSong.put(s.mName, s);
        }
        return songNameToMbsProSong;
    }

    private static Set<String> validateAndCreateSetFromDriveFiles(List<File> driveFiles)
    {
        return validateAndCreateSetFromListKeys(driveFiles, File::getName);
    }

    private static Set<String> validateAndCreateSetFromDocumentFiles(List<DocumentFile> documentFiles)
    {
        return validateAndCreateSetFromListKeys(documentFiles, DocumentFile::getName);
    }

    private static <T> Set<String> validateAndCreateSetFromListKeys(List<T> list, KeyGetter<T> keyGetter)
    {
        Set<String> keys = new HashSet<>();
        for (T item : list)
        {
            String key = keyGetter.getKey(item);
            ProdAssert.prodAssert(keys.add(key), "Duplicate item key %s found", key);
        }
        return keys;
    }
}

interface KeyGetter<T>
{
    String getKey(T item);
}

package com.eriwang.mbspro_updater.drive;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SongFinder
{
    private final Executor mExecutor;
    private final DriveWrapper mDrive;

    public SongFinder(DriveWrapper drive)
    {
        mExecutor = Executors.newSingleThreadExecutor();
        mDrive = drive;
    }

    public Task<List<DriveSong>> findSongsRecursivelyInDirectory(String directoryId)
    {
        return Tasks.call(mExecutor, () ->
                findSongsRecursivelyInDirectoryForeground(mDrive.getFileMetadata(directoryId)));
    }

    private List<DriveSong> findSongsRecursivelyInDirectoryForeground(File directory) throws IOException
    {
        /*
         * Some assumptions on songs that might need to be revisited:
         *  - A directory with no child directories is a song (breaks rather easily, possible solution is to include
         * any directory with pdfs/ audio, and have user manually exclude directories)
         *  - There is only one song per directory (e.g. if I see pdfs from three different "songs" in one directory,
         *  this will still count them all as the same song)
         */
        boolean dirIsSong = true;
        ArrayList<DriveSong> driveSongs = new ArrayList<>();
        List<File> dirContents = mDrive.listDirectory(directory.getId());
        for (File file : dirContents)
        {
            if (DriveUtils.isFolder(file))
            {
                dirIsSong = false;
                driveSongs.addAll(findSongsRecursivelyInDirectoryForeground(file));
            }
        }

        return (dirIsSong) ?
                Collections.singletonList(createSongFromDirContents(directory.getName(), dirContents)) :
                driveSongs;
    }

    private static DriveSong createSongFromDirContents(String dirName, List<File> dirContents)
    {
        ArrayList<File> pdfFiles = new ArrayList<>();
        ArrayList<File> audioFiles = new ArrayList<>();
        for (File file : dirContents)
        {
            if (file.getMimeType().equals("application/pdf") && file.getName().endsWith(".gen.pdf"))
            {
                pdfFiles.add(file);
            }
            else if (file.getMimeType().startsWith("audio"))
            {
                audioFiles.add(file);
            }
        }
        return new DriveSong(dirName, pdfFiles, audioFiles);
    }
}

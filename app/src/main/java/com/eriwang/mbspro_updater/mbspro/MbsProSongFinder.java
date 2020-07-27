package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.utils.DocumentFileUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;

import java.util.ArrayList;
import java.util.List;

public class MbsProSongFinder
{
    private final Context mContext;

    public MbsProSongFinder(Context context)
    {
        mContext = context;
    }

    public List<MbsProSong> findSongsInDirectoryUri(Uri directoryUri)
    {
         DocumentFile directory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri);

        List<MbsProSong> songs = new ArrayList<>();
        for (DocumentFile rootDirFile : directory.listFiles())
        {
            if (!rootDirFile.isDirectory())
            {
                continue;
            }

            List<DocumentFile> pdfs = new ArrayList<>();
            List<DocumentFile> audioFiles = new ArrayList<>();
            for (DocumentFile songDirFile : rootDirFile.listFiles())
            {
                if (isGenPdf(songDirFile))
                {
                    pdfs.add(songDirFile);
                }
                else if (isAudio(songDirFile))
                {
                    audioFiles.add(songDirFile);
                }
            }

            songs.add(new MbsProSong(rootDirFile.getName(), pdfs, audioFiles));
        }

        return songs;
    }

    private static boolean isGenPdf(DocumentFile documentFile)
    {
        String mimeType = documentFile.getType();
        String filename = documentFile.getName();
        ProdAssert.notNull(mimeType);
        ProdAssert.notNull(filename);
        return mimeType.equals("application/pdf") && filename.endsWith(".gen.pdf");
    }

    private static boolean isAudio(DocumentFile documentFile)
    {
        String mimeType = documentFile.getType();
        ProdAssert.notNull(mimeType);
        return mimeType.startsWith("audio");
    }
}

package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.utils.DocumentFileUtils;
import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.eriwang.mbspro_updater.utils.StreamUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MbsProSongFinder
{
    private final Context mContext;

    public MbsProSongFinder(Context context)
    {
        mContext = context;
    }

    public List<MbsProSong> findSongsInDirectoryUri(Uri directoryUri) throws IOException
    {
         DocumentFile directory = DocumentFileUtils.safeDirectoryFromTreeUri(mContext, directoryUri);

        List<MbsProSong> songs = new ArrayList<>();
        for (DocumentFile rootDirFile : directory.listFiles())
        {
            if (!rootDirFile.isDirectory())
            {
                continue;
            }

            List<MbsProSong.MbsProSongPdf> pdfs = new ArrayList<>();
            List<MbsProSong.MbsProSongAudio> audioFiles = new ArrayList<>();
            for (DocumentFile songDirFile : rootDirFile.listFiles())
            {
                if (isGenPdf(songDirFile))
                {
                    pdfs.add(new MbsProSong.MbsProSongPdf(
                        songDirFile.getName(), getPdfNumPages(songDirFile), songDirFile.lastModified()));
                }
                else if (isAudio(songDirFile))
                {
                    audioFiles.add(new MbsProSong.MbsProSongAudio(
                            songDirFile.getName(), songDirFile.lastModified()));
                }
            }

            songs.add(new MbsProSong(rootDirFile.getName(), pdfs, audioFiles));
        }

        return songs;
    }

    private int getPdfNumPages(DocumentFile pdfFile) throws IOException
    {
        InputStream pdfInputStream = mContext.getContentResolver().openInputStream(pdfFile.getUri());
        ProdAssert.notNull(pdfInputStream);

        java.io.File tempPdfFile = java.io.File.createTempFile("songDirFile", "pdf");
        FileOutputStream tempPdfFileOutputStream = new FileOutputStream(tempPdfFile);
        StreamUtils.writeInputToOutputStream(pdfInputStream, tempPdfFileOutputStream);
        tempPdfFileOutputStream.close();

        return new PdfRenderer(ParcelFileDescriptor.open(tempPdfFile,ParcelFileDescriptor.MODE_READ_ONLY))
                .getPageCount();
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

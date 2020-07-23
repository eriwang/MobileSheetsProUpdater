package com.eriwang.mbspro_updater.mbspro;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.eriwang.mbspro_updater.utils.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

 // Information needed:
//  - Audio: SongId, Title (filename?), File (song name + filename), maybe EndPos (milliseconds?). LastModified
//  would be nice
public class MbsProSong
{
    public static class MbsProSongPdf  // TODO: modifier?
    {
        public final String mFilename;
        public final int mNumPages;
        public final long mLastModified;


        public MbsProSongPdf(String filename, int numPages, long lastModified)
        {
            mFilename = filename;
            mNumPages = numPages;
            mLastModified = lastModified;
        }
    }

    // TODO: MbsProSongAudio

    public final String mName;
    // TODO: private final int mCreationDate;

    public final List<MbsProSongPdf> mPdfs;
    // TODO: private final List<MbsProSongAudio> mAudioFiles;

    public MbsProSong(String name, List<MbsProSongPdf> pdfs)
    {
        mName = name;
        mPdfs = pdfs;
    }

    // TODO: build this in SongFileManager (or elsewhere) instead of iterating after the fact. Assumptions here aren't
    //  great
    public static List<MbsProSong> createFromRootUri(Uri rootUri, Context context) throws IOException
    {
        DocumentFile directory = DocumentFile.fromTreeUri(context, rootUri);
        ProdAssert.notNull(directory);
        ProdAssert.prodAssert(directory.isDirectory(), "Document %s is not a directory", directory.getName());

        List<MbsProSong> songs = new ArrayList<>();
        for (DocumentFile rootDirFile : directory.listFiles())
        {
            if (!rootDirFile.isDirectory())
            {
                continue;
            }

            List<MbsProSongPdf> pdfs = new ArrayList<>();
            for (DocumentFile songDirFile : rootDirFile.listFiles())
            {
                if (!songDirFile.getType().equals("application/pdf") || !songDirFile.getName().endsWith(".gen.pdf"))
                {
                    continue;
                }

                InputStream pdfInputStream = context.getContentResolver().openInputStream(songDirFile.getUri());
                ProdAssert.notNull(pdfInputStream);

                File tempPdfFile = File.createTempFile("songDirFile", "pdf");
                FileOutputStream tempPdfFileOutputStream = new FileOutputStream(tempPdfFile);
                StreamUtils.writeInputToOutputStream(pdfInputStream, tempPdfFileOutputStream);
                tempPdfFileOutputStream.close();

                final int pdfNumPages = new PdfRenderer(
                        ParcelFileDescriptor.open(tempPdfFile,ParcelFileDescriptor.MODE_READ_ONLY)).getPageCount();
                pdfs.add(new MbsProSongPdf(songDirFile.getName(), pdfNumPages, songDirFile.lastModified()));
            }

            songs.add(new MbsProSong(rootDirFile.getName(), pdfs));
        }

        return songs;
    }
}

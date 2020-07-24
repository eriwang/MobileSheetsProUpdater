package com.eriwang.mbspro_updater.mbspro;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.eriwang.mbspro_updater.utils.ProdAssert;
import com.eriwang.mbspro_updater.utils.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MbsProDatabaseManager
{
    private final ContentResolver mContentResolver;
    private Uri mDbUri;

    public MbsProDatabaseManager(ContentResolver contentResolver)
    {
        mContentResolver = contentResolver;
    }

    public void setDbUri(Uri dbUri)
    {
        mDbUri = dbUri;
    }

    public void insertSongsIntoDb(List<MbsProSong> mbsProSongs) throws IOException
    {
        validateDbUriKnown();

        File tempDbFile = File.createTempFile("mbs", "db");

        SQLiteDatabase db = readCurrentDatabase(tempDbFile);
        db.delete("Songs", null, null);
        db.delete("Files", null, null);
        db.delete("AudioFiles", null, null);
        db.delete("Bookmarks", null, null);

        for (MbsProSong mbsProSong : mbsProSongs)
        {
            ContentValues songValues = new ContentValues();
            songValues.put("Title", mbsProSong.mName);
            long songId = db.insert("Songs", null, songValues);
            ProdAssert.prodAssert(songId != -1, "Insertion for song %s failed", mbsProSong.mName);

            // TODO: ordering of pdfs is done by the actual ID of the entries in the database. I'd like to have a
            //  sane ordering (for example to start parts alpha order, score last)
            int currentPageCount = 0;
            for (MbsProSong.MbsProSongPdf pdf : mbsProSong.mPdfs)
            {
                ContentValues pdfFileValues = new ContentValues();
                pdfFileValues.put("SongId", songId);
                pdfFileValues.put("Path", String.format("%s/%s", mbsProSong.mName, pdf.mFilename));
                pdfFileValues.put("PageOrder", String.format("1-%d", pdf.mNumPages));
                pdfFileValues.put("LastModified", pdf.mLastModified);
                pdfFileValues.put("Type", 1);  // Not sure if PDF file type or MBS Pro SourceType
                long pdfFileId = db.insert("Files", null, pdfFileValues);
                ProdAssert.prodAssert(pdfFileId != -1, "Insertion for pdf file %s failed", pdf.mFilename);

                String partName = getPartName(pdf.mFilename);
                if (partName == null)
                {
                    partName = "Score";
                }

                ContentValues bookmarkValues = new ContentValues();
                bookmarkValues.put("SongId", songId);
                bookmarkValues.put("Name", partName);
                bookmarkValues.put("PageNum", currentPageCount);
                bookmarkValues.put("ShowInLibrary", 1);
                long bookmarkId = db.insert("Bookmarks", null, bookmarkValues);
                ProdAssert.prodAssert(bookmarkId != -1, "Insertion for bookmark %s failed", partName);

                currentPageCount += pdf.mNumPages;
            }

            for (MbsProSong.MbsProSongAudio audioFile : mbsProSong.mAudioFiles)
            {
                ContentValues audioFileValues = new ContentValues();
                audioFileValues.put("SongId", songId);
                audioFileValues.put("Title", filenameNoExtension(audioFile.mFilename));
                audioFileValues.put("File", String.format("%s/%s", mbsProSong.mName, audioFile.mFilename));
                audioFileValues.put("LastModified", audioFile.mLastModified);
                long audioFileId = db.insert("AudioFiles", null, audioFileValues);
                ProdAssert.prodAssert(audioFileId != -1, "Insertion for audio file %s failed", audioFile.mFilename);
            }
        }
        db.close();

        writeToDatabase(tempDbFile);

        Log.d("db_test", "Finished writing db with songs");
    }

    // The Android Storage Access Framework makes accessing the actual MBS Pro database java.io.File object impossible.
    // In order to manipulate the database using the built in Android SQLiteDatabase, we instead read the database to a
    // temp file (i.e. making a tempfile-backed database) and then write out the temp file again when we're done.
    private SQLiteDatabase readCurrentDatabase(File tempDbFile) throws IOException
    {
        FileOutputStream tempFileDbOutputStream = new FileOutputStream(tempDbFile);
        InputStream currentDbInputStream = mContentResolver.openInputStream(mDbUri);
        ProdAssert.notNull(currentDbInputStream);

        StreamUtils.writeInputToOutputStream(currentDbInputStream, tempFileDbOutputStream);
        tempFileDbOutputStream.close();
        currentDbInputStream.close();

        return SQLiteDatabase.openOrCreateDatabase(tempDbFile, null);
    }

    private void writeToDatabase(File tempDbFile) throws IOException
    {
        FileInputStream newDbFileInputStream = new FileInputStream(tempDbFile);
        OutputStream dbOutputStream = mContentResolver.openOutputStream(mDbUri, "w");
        ProdAssert.notNull(dbOutputStream);

        StreamUtils.writeInputToOutputStream(newDbFileInputStream, dbOutputStream);
        dbOutputStream.close();
        newDbFileInputStream.close();
    }

    private void validateDbUriKnown()
    {
        ProdAssert.notNull(mDbUri);
    }

    private static String getPartName(String filename)
    {
        int dashIndex = filename.lastIndexOf('-');
        if (dashIndex == -1)
        {
            return null;
        }

        int dotIndex = filename.indexOf('.', dashIndex);
        ProdAssert.prodAssert(dotIndex > -1, "Could not parse part name, no . in %s", filename);
        return filename.substring(dashIndex + 1, dotIndex).trim();
    }

    private static String filenameNoExtension(String filename)
    {
        int dotIndex = filename.lastIndexOf('.');
        ProdAssert.prodAssert(dotIndex > -1, "Could not find extension in filename %s", filename);
        return filename.substring(0, dotIndex);
    }
}

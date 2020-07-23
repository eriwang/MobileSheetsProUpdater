package com.eriwang.mbspro_updater.mbspro;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.eriwang.mbspro_updater.drive.Song;
import com.eriwang.mbspro_updater.utils.ProdAssert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MbsProDatabaseManager
{
    public static void insertSongsIntoDb(List<Song> songs, Uri dbUri, ContentResolver contentResolver) throws IOException
    {
        InputStream dbInputStream = contentResolver.openInputStream(dbUri);
        ProdAssert.notNull(dbInputStream);

        File dbFile = File.createTempFile("mbs", "db");
        FileOutputStream dbFileOutputStream = new FileOutputStream(dbFile);

        int readBytes;
        byte[] buffer = new byte[2048];
        while ((readBytes = dbInputStream.read(buffer)) != -1)
        {
            dbFileOutputStream.write(buffer, 0, readBytes);
        }
        dbFileOutputStream.flush();
        dbInputStream.close();

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        db.delete("Songs", null, null);
        db.delete("Files", null, null);

        for (Song song : songs)
        {
            ContentValues songValues = new ContentValues();
            songValues.put("Title", song.mName);
            songValues.put("SongId", 0);
            long songId = db.insert("Songs", null, songValues);
            ProdAssert.prodAssert(songId != -1, "Insertion for song %s failed", song.mName);

            for (com.google.api.services.drive.model.File pdfFile : song.mPdfFiles)
            {
                ContentValues pdfFileValues = new ContentValues();
                pdfFileValues.put("SongId", songId);
                pdfFileValues.put("Path", String.format("%s/%s", song.mName, pdfFile.getName()));
                pdfFileValues.put("PageOrder", "1-1");  // FIXME: wrong
                pdfFileValues.put("Type", 1);  // Not sure if PDF file type or MBS Pro SourceType
                long pdfFileId = db.insert("Files", null, pdfFileValues);
                ProdAssert.prodAssert(pdfFileId != -1, "Insertion for pdf file %s failed", pdfFile.getName());
            }
        }
        db.close();

        OutputStream dbOutputStream = contentResolver.openOutputStream(dbUri, "w");
        FileInputStream newDbFileInputStream = new FileInputStream(dbFile);
        while ((readBytes = newDbFileInputStream.read(buffer)) != -1)
        {
            dbOutputStream.write(buffer, 0, readBytes);
        }
        dbOutputStream.close();

        Log.d("db_test", "Finished writing db with songs");
    }
}

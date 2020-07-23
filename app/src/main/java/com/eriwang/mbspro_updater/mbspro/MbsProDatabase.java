package com.eriwang.mbspro_updater.mbspro;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.eriwang.mbspro_updater.utils.ProdAssert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MbsProDatabase
{
    public static void printGenres(Uri dbUri, ContentResolver contentResolver) throws IOException
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
        Cursor cursor = db.query("Genres", null, null, null, null, null, null);
        while (cursor.moveToNext())
        {
            Log.d("printGenres", cursor.getString(1));
        }

        cursor.close();
    }
}

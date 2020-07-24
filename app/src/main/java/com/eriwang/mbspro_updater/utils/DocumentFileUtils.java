package com.eriwang.mbspro_updater.utils;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

public class DocumentFileUtils
{
    public static DocumentFile safeDirectoryFromTreeUri(Context context, Uri directoryUri)
    {
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        ProdAssert.notNull(directory);
        ProdAssert.prodAssert(directory.isDirectory(), "Document %s is not a directory", directory.getName());
        return directory;
    }
}

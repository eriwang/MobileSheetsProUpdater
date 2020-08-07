package com.eriwang.mbspro_updater.drive;

import com.google.api.services.drive.model.File;

public class DriveUtils
{
    public static boolean isFolder(File file)
    {
        return file.getMimeType().equals("application/vnd.google-apps.folder");
    }
}

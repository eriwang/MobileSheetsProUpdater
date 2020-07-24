package com.eriwang.mbspro_updater.drive;

import com.google.api.services.drive.model.File;

import java.util.List;

public class DriveSong
{
    public final String mName;
    public final List<File> mPdfFiles;
    public final List<File> mAudioFiles;

    public DriveSong(String name, List<File> pdfFiles, List<File> audioFiles)
    {
        mName = name;
        mPdfFiles = pdfFiles;
        mAudioFiles = audioFiles;
    }
}

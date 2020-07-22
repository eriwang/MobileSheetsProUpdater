package com.eriwang.mbspro_updater.drive;

import com.google.api.services.drive.model.File;

import java.util.List;

public class Song
{
    public final String mName;
    public final List<File> mPdfFiles;
    public final List<File> mAudioFiles;

    public Song(String name, List<File> pdfFiles, List<File> audioFiles)
    {
        mName = name;
        mPdfFiles = pdfFiles;
        mAudioFiles = audioFiles;
    }
}

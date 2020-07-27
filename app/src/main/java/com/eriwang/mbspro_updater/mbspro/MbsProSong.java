package com.eriwang.mbspro_updater.mbspro;

import androidx.documentfile.provider.DocumentFile;

import java.util.List;

public class MbsProSong
{
    public final String mName;
    // TODO: private final int mCreationDate;

    public final List<DocumentFile> mPdfs;
    public final List<DocumentFile> mAudioFiles;

    public MbsProSong(String name, List<DocumentFile> pdfs, List<DocumentFile> audioFiles)
    {
        mName = name;
        mPdfs = pdfs;
        mAudioFiles = audioFiles;
    }
}

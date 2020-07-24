package com.eriwang.mbspro_updater.mbspro;

import java.util.List;

public class MbsProSong
{
    public final String mName;
    // TODO: private final int mCreationDate;

    public final List<MbsProSongPdf> mPdfs;
    public final List<MbsProSongAudio> mAudioFiles;

    public MbsProSong(String name, List<MbsProSongPdf> pdfs, List<MbsProSongAudio> audioFiles)
    {
        mName = name;
        mPdfs = pdfs;
        mAudioFiles = audioFiles;
    }

    public static class MbsProSongPdf
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

    public static class MbsProSongAudio
    {
        public final String mFilename;
        public final long mLastModified;

        public MbsProSongAudio(String filename, long lastModified)
        {
            mFilename = filename;
            mLastModified = lastModified;
        }
    }
}

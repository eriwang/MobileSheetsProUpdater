package com.eriwang.mbspro_updater.mbspro;

import java.util.List;

 // Information needed:
//  - Audio: SongId, Title (filename?), File (song name + filename), maybe EndPos (milliseconds?). LastModified
//  would be nice
public class MbsProSong
{
    public final String mName;
    // TODO: private final int mCreationDate;

    public final List<MbsProSongPdf> mPdfs;
    // TODO: private final List<MbsProSongAudio> mAudioFiles;

    public MbsProSong(String name, List<MbsProSongPdf> pdfs)
    {
        mName = name;
        mPdfs = pdfs;
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

    // TODO: MbsProSongAudio
}

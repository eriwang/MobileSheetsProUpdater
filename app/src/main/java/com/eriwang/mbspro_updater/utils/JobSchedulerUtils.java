package com.eriwang.mbspro_updater.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;

public class JobSchedulerUtils
{
    // Note that API 24 has a getPendingJob function, which this is basically mimicking.
    public static boolean isJobScheduled(Context context, int jobId)
    {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs())
        {
            if (jobInfo.getId() == jobId)
            {
                return true;
            }
        }
        return false;
    }
}

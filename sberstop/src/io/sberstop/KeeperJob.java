package io.sberstop;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Периодическое фоновое задание (Android JobScheduler).
 * Срабатывает каждые ~15 минут: удаляет мусор Сбера, очищает кэш всех приложений и сбрасывает ОЗУ.
 */
public class KeeperJob extends JobService {
    private static final int JOB_ID = 1001;

    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                String result = Blacklist.apply(this);
                SharedPreferences p = getSharedPreferences("state", MODE_PRIVATE);
                p.edit().putString("result", result).putLong("time", System.currentTimeMillis()).apply();
            } finally {
                jobFinished(params, false);
            }
        }, "sberstop-job").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return false; }

    /** Ставит периодическое задание раз в 15 минут. */
    static void schedulePeriodic(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, KeeperJob.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        js.schedule(info);
    }

    /** Разовый запуск через задержку delayMs. */
    static void scheduleOnce(Context c, long delayMs) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo info = new JobInfo.Builder(JOB_ID + 1, new ComponentName(c, KeeperJob.class))
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + 10_000L)
                .build();
        js.schedule(info);
    }
}

package io.sberstop;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/**
 * Фоновый проход без «переднего плана»: с Android 12 службу с уведомлением нельзя запускать из фона (Android
 * отвечает «Background started FGS: Disallowed»), а JobScheduler — можно и нужно. Задание будит приложение, делает
 * один проход по списку и ставит следующее задание через 2 минуты; раз в 15 минут есть ещё периодическое задание
 * как страховка. Задания переживают перезагрузку (setPersisted).
 */
public class KeeperJob extends JobService {
    static final int JOB_ONE = 1, JOB_PERIODIC = 2;
    private static final long NEXT_MS = 120_000L;

    @Override public boolean onStartJob(JobParameters p) {
        new Thread(() -> {
            String result = Blacklist.apply(this);
            getSharedPreferences("state", MODE_PRIVATE).edit()
                    .putString("result", result).putLong("time", System.currentTimeMillis()).apply();
            jobFinished(p, false);
            if (p.getJobId() == JOB_ONE) scheduleOnce(this, NEXT_MS);   // следующая проверка
        }, "sberstop-job").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters p) { return false; }

    /** Один проход примерно через delayMs (и не позже delayMs + минута). */
    static void scheduleOnce(Context c, long delayMs) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        JobInfo info = new JobInfo.Builder(JOB_ONE, new ComponentName(c, KeeperJob.class))
                .setMinimumLatency(Math.max(0, delayMs))
                .setOverrideDeadline(Math.max(0, delayMs) + 60_000L)
                .setPersisted(true)
                .build();
        js.schedule(info);
    }

    /** Страховочное задание раз в 15 минут (минимум, который разрешает JobScheduler). */
    static void schedulePeriodic(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        JobInfo info = new JobInfo.Builder(JOB_PERIODIC, new ComponentName(c, KeeperJob.class))
                .setPeriodic(15 * 60_000L)
                .setPersisted(true)
                .build();
        js.schedule(info);
    }
}

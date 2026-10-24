package io.sberstop;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Ставит фоновые задания: сразу после загрузки (телевизор Сбера возвращает свой лаунчер), после обновления
 * «Сбер-Стопа» и при установке/включении приложения (когда система доставляет такое сообщение) — один проход в
 * ближайшие секунды, дальше задания идут цепочкой каждые 2 минуты. Плюс страховочное периодическое задание.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // 45 секунд: к этому времени Сбер уже вернул свой лаунчер, но пользователь ещё не долго видит чужой экран.
            KeeperJob.scheduleOnce(c, 45_000L);
            KeeperJob.schedulePeriodic(c);
            return;
        }
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
            if (System.currentTimeMillis() - KeeperService.lastRun(c) < 60_000L) return;
            KeeperJob.scheduleOnce(c, 3_000L);
        }
    }
}

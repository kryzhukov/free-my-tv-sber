package io.sberstop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

/** Немедленный проход по кнопке «Проверить сейчас» (когда приложение на экране — службу запускать можно). */
public class KeeperService extends Service {
    private static final String CHANNEL = "keeper";
    private static final int ID = 42;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Сбер-Стоп", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Сбер-Стоп")
                .setContentText("Проверяю приложения Сбера…")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        try {
            startForeground(ID, n);
        } catch (Exception ignored) {
            // Android 12+ может запретить передний план из фона: проход всё равно сделаем, без уведомления.
        }
        new Thread(() -> {
            String result = Blacklist.apply(this);
            SharedPreferences p = getSharedPreferences("state", MODE_PRIVATE);
            p.edit().putString("result", result).putLong("time", System.currentTimeMillis()).apply();
            KeeperJob.scheduleOnce(this, 60_000L);
            try { stopForeground(true); } catch (Exception ignored) {}
            stopSelf();
        }, "sberstop").start();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    /** Когда последний раз проходили по списку (мс), 0 — ещё ни разу. */
    static long lastRun(Context c) {
        return c.getSharedPreferences("state", Context.MODE_PRIVATE).getLong("time", 0);
    }
}

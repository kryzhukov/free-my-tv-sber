package io.sberstop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Простой экран: что и когда заблокировано, и кнопка «Проверить сейчас». */
public class MainActivity extends Activity {
    private TextView status;
    private long pressedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            show();
            handler.postDelayed(this, 1500);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = Math.round(48 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Сбер-Стоп");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        root.addView(title);

        TextView what = new TextView(this);
        what.setText("Сторож мусора Сбера. Проверяет после каждой загрузки и примерно каждые две минуты: удаляет "
                + "вернувшееся приложение Сбера (если удалить нельзя — выключает), автообновления прошивки и "
                + "приложений, рекламу поверх экрана. Если Сбер вернул свой лаунчер главным экраном, возвращает "
                + "Free My TV.");
        what.setTextColor(0xFFB0B0B0);
        what.setTextSize(15);
        what.setPadding(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, pad);
        root.addView(what);

        status = new TextView(this);
        status.setTextColor(0xFF80C080);
        status.setTextSize(17);
        root.addView(status);

        Button run = new Button(this);
        run.setText("Проверить сейчас");
        run.setOnClickListener(v -> {
            pressedAt = System.currentTimeMillis();
            status.setText("Проверяю…");
            startForegroundService(new Intent(this, KeeperService.class));
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad;
        root.addView(run, lp);

        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    /** Показывает последний результат; «Проверяю…» держится, пока не запишется новый. */
    private void show() {
        String result = getSharedPreferences("state", MODE_PRIVATE).getString("result", null);
        long time = getSharedPreferences("state", MODE_PRIVATE).getLong("time", 0);
        if (pressedAt != 0 && time < pressedAt) {
            status.setText("Проверяю…");
            return;
        }
        if (result == null) {
            status.setText("Ещё не запускалось. Нажмите «Проверить сейчас» и разрешите отладку (галочка "
                    + "«Всегда разрешать»), если телевизор спросит один раз.");
            return;
        }
        String when = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
        status.setText("Последняя проверка: " + when + " — " + result);
    }
}

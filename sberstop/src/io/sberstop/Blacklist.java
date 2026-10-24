package io.sberstop;

import android.content.Context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Приложения Сбера, которые телевизор возвращает или включает сам. Каждый проход (после загрузки, при установке,
 * обновлении или включении приложения, и раз в полторы минуты) список проверяется заново: всё, что Сбер вернул,
 * <b>удаляется для пользователя</b> (pm uninstall --user 0: приложение пропадает из системы, но его APK остаётся в
 * разделе system — на заводской сброс или на «cmd package install-existing»). Если удалить не дают — хотя бы
 * выключается (pm disable-user). Дополнительно выключаются автообновления прошивки и приложений, закрывается
 * рекламный оверлей Сбера, а если Сбер вернул свой лаунчер главным экраном — главным экраном снова становится
 * Free My TV.
 */
final class Blacklist {
    private Blacklist() {}

    static final String[] PACKAGES = {
            "ru.sberdevices.starcleaner",        // «Очистка памяти» Сбера
            "ru.sberdevices.smarthome.promo",    // промо «Умного дома»
            "ru.sberdevices.starbrowser",        // браузер Сбера
            "ru.sberdevices.openbeta",           // бета-программа (канал обновлений)
            "ru.sberdevices.survey",             // опросы
            "ru.sberdevices.starmarket",         // магазин приложений Сбера
            "ru.sberdevices.videos",             // «Видео»
            "ru.sberdevices.music",              // «Музыка»
            "ru.sberdevices.sberplay",           // SberPlay
            "ru.sberdevices.starprofile",        // профиль
            "ru.sberdevices.subscriptions",      // подписки
            "ru.sberdevices.parentcontrol",      // родительский контроль
            "ru.sberdevices.starkidsroom.prod",  // детская комната
            "ru.sberdevices.bugreporter",        // баг-репортер
            "ru.sberdevices.app.setup",          // установщик приложений Сбера (TvSetupApp)
            "ru.sberdevices.starlauncher",       // лаунчер Сбера
            "ru.sberdevices.starscreensaver",    // заставка Сбера
            "com.salute.star",                   // «Умный дом»
            "com.sberbank.settings",             // настройки СберБанка
            "com.spocky.projengmenu",            // Projectivy Launcher (другой лаунчер)
    };

    private static final String HOME = "io.freemytv/.HomeActivity";

    /** Один проход: удалить всё включённое из списка, обновления, рекламу и вернуть себе главный экран. */
    static String apply(Context c) {
        int gone = 0, off = 0;
        boolean homeStolen = false;
        try (Adb adb = Adb.connect(c, 20_000, null)) {
            Set<String> present = packages(adb, "");
            for (String pkg : PACKAGES) {
                if (!present.contains(pkg)) continue;      // не установлен (или уже удалён для пользователя)
                String out = adb.shell("pm uninstall --user 0 " + pkg);
                if (out.contains("Success")) {
                    gone++;
                } else {
                    // Не дали удалить (защищённое системное приложение) — хотя бы выключаем.
                    String d = adb.shell("pm disable-user --user 0 " + pkg);
                    if (d.contains("disabled-user")) off++;
                }
            }
            adb.shell("settings put global ota_disable_automatic_update 1");
            adb.shell("settings put global auto_update_apps 0");
            adb.shell("appops set ru.sberdevices.services SYSTEM_ALERT_WINDOW deny");
            // Главный экран: Сбер после загрузки возвращает свой лаунчер и забирает Home себе (или сбрасывает выбор).
            String holder = homeHolder(adb);
            if (!"io.freemytv".equals(holder)) {
                adb.shell("cmd package set-home-activity " + HOME);
                homeStolen = true;
            }
            if (gone == 0 && off == 0 && !homeStolen) return "Всё на месте: приложений Сбера нет";
            StringBuilder s = new StringBuilder();
            if (gone > 0) s.append("удалено: ").append(gone).append(" прил.");
            if (off > 0) s.append(s.length() > 0 ? ", " : "").append("выключено: ").append(off).append(" прил.");
            if (homeStolen) s.append(s.length() > 0 ? " + " : "").append("главный экран");
            return s.toString();
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    /** Пакет, который сейчас главный экран ("" — система без выбранного лаунчера, например показывает выбор). */
    private static String homeHolder(Adb adb) throws IOException {
        String out = adb.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN"
                + " -c android.intent.category.HOME").trim();
        String[] lines = out.split("\n");
        String component = lines.length == 0 ? "" : lines[lines.length - 1].trim();
        int slash = component.indexOf('/');
        String pkg = slash > 0 ? component.substring(0, slash) : "";
        return pkg.startsWith("android/") || pkg.equals("android") ? "" : pkg;
    }

    /** Пакеты, как их видит оболочка ("" — установленные для пользователя, "-d" — только выключенные). */
    private static Set<String> packages(Adb adb, String flags) throws IOException {
        Set<String> out = new HashSet<>();
        for (String line : adb.shell("pm list packages " + flags).split("\n")) {
            line = line.trim();
            if (line.startsWith("package:")) out.add(line.substring(8));
        }
        return out;
    }
}

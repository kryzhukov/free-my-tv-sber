# Резервная копия конфигурации ТВ (Salute TV / CVTE MT5867)

Снято: 31 октября 2026 г.

## Состав резервной копии

- `installed_packages.txt` — полный список всех системных и пользовательских пакетов на ТВ после чистки.
- `system_properties.txt` — все системные свойства (`getprop`).
- `mounts.txt` — карта монтирования разделов (`/proc/mounts`), включая USB-накопитель.
- `hosts.txt` — файл `/system/etc/hosts` с блокировкой серверов автообновлений (OTA) CVTE и Сбера.
- `mount_usb_apps.sh` — скрипт автоматического монтирования флешки под приложения (`/data/app`).
- `init.usb_apps.rc` — сервис автозагрузки Android init для монтирования флешки при старте ТВ.

---

## Текущее состояние системы

1. **Загрузчик:** разблокирован (`ro.boot.verifiedbootstate=orange`, `sys.unlocked=1`).
2. **Root-доступ:** установлен нативный `su` в `/system/bin/su` и `/system/xbin/su` через сервисный шлюз `at_sudo`.
3. **Хранилище приложений:** USB 2.0 Flash 64GB смонтирована в `/data/app` (файловая система ext4). Доступно ~55 ГБ под установку приложений.
4. **Удалённый мусор Сбера:**
   - Автоустановщик `/vendor/priv-app/TvSetupApp/TvSetupApp.apk` деактивирован (`.disabled`).
   - Удалены: `starscreensaver`, `openbeta`, `music`, `parentcontrol`, `starcleaner`, `salute.star`, `smarthome.promo`, `subscriptions`, `starmarket`, `survey`.
5. **Сохранённые службы:**
   - Голосовой помощник: `ru.sberdevices.assistant`
   - Экранная клавиатура: `ru.sberdevices.app.starkeyboard`
   - Браузер: `ru.sberdevices.starbrowser`
   - Управление пультом и со смартфона: `ru.sberdevices.services`, `ru.sberdevices.services.core`, `ru.sberdevices.starprofile`, `com.cvte.sber.onekey`
   - СберTube: `ru.sberdevices.startube`
   - Телеканалы: `ru.sberdevices.starlivetv`

# Скрипт восстановления системных файлов на ТВ при сбросе
param(
    [string]$IP = "192.168.1.130"
)

$adb = "adb"
if (Test-Path "$env:USERPROFILE\dsh-tv-build\platform-tools\adb.exe") {
    $adb = "$env:USERPROFILE\dsh-tv-build\platform-tools\adb.exe"
}

Write-Host "Подключение к ТВ $IP..."
& $adb connect "$IP`:5555"

Write-Host "Восстановление скриптов монтирования флешки..."
& $adb push (Join-Path $PSScriptRoot "mount_usb_apps.sh") /data/local/tmp/mount_usb_apps.sh
& $adb push (Join-Path $PSScriptRoot "init.usb_apps.rc") /data/local/tmp/init.usb_apps.rc
& $adb push (Join-Path $PSScriptRoot "hosts.txt") /data/local/tmp/hosts_new

& $adb shell "su -c '
mount -o remount,rw /
cp /data/local/tmp/mount_usb_apps.sh /system/bin/mount_usb_apps.sh
chmod 755 /system/bin/mount_usb_apps.sh
cp /data/local/tmp/init.usb_apps.rc /system/etc/init/init.usb_apps.rc
chmod 644 /system/etc/init/init.usb_apps.rc
cp /data/local/tmp/hosts_new /system/etc/hosts
chmod 644 /system/etc/hosts
mount -o remount,ro /
rm /data/local/tmp/mount_usb_apps.sh /data/local/tmp/init.usb_apps.rc /data/local/tmp/hosts_new
'"

Write-Host "Готово! Все конфигурации восстановлены."

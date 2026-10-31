#!/system/bin/sh
# Auto-mount USB apps storage to /data/app
if grep -q " /data/app " /proc/mounts; then
    exit 0
fi

DEV=""
for i in 1 2 3 4 5; do
    for d in /dev/block/sd[a-z][0-9]; do
        if [ -b "$d" ]; then
            DEV="$d"
            break 2
        fi
    done
    sleep 1
done

if [ -n "$DEV" ]; then
    mkdir -p /mnt/usb_apps
    mount -t ext4 -o rw,noatime,nosuid,nodev "$DEV" /mnt/usb_apps
    if [ $? -eq 0 ]; then
        mkdir -p /mnt/usb_apps/app
        chown system:system /mnt/usb_apps/app
        chmod 0771 /mnt/usb_apps/app
        mount -o bind /mnt/usb_apps/app /data/app
    fi
fi

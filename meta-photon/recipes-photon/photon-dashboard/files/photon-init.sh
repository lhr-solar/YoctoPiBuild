#!/bin/sh
# Photon fast-init: start X11 + Dashboard BEFORE systemd boots.
# Kernel boots with init=/opt/photon-init.sh

# ── Mount essential virtual filesystems ──
mount -t proc     proc     /proc
mount -t sysfs    sysfs    /sys
mount -t devtmpfs devtmpfs /dev
mount -t tmpfs    tmpfs    /tmp
mkdir -p /dev/pts /dev/shm /run
mount -t devpts   devpts   /dev/pts
mount -t tmpfs    tmpfs    /dev/shm
mount -t tmpfs    tmpfs    /run

# ── Wait for GPU (DRM) to appear — usually <200ms on CM5 ──
TRIES=0
while [ ! -e /dev/dri/card0 ] && [ $TRIES -lt 50 ]; do
    TRIES=$((TRIES + 1))
    usleep 20000  # 20ms
done

# ── Launch X11 + Dashboard in background ──
export HOME=/root
export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/broadcom_icd.aarch64.json

/usr/bin/xinit /root/.xinitrc -- :0 vt1 -nolisten tcp -nocursor -s 0 -dpms &

# ── Hand off to systemd as PID 1 for remaining services ──
exec /lib/systemd/systemd

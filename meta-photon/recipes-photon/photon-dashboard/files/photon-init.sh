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
# See photon-dashboard.service for rationale on these flags.
export LD_BIND_NOW=1 MALLOC_ARENA_MAX=2 MESA_NO_ERROR=1 vblank_mode=0

# Page-cache prewarm: pull DashboardOnly + its libs + font into RAM in the
# background so xinit/Xorg init runs in parallel with the disk read.
( cat /usr/bin/DashboardOnly /usr/share/fonts/Satoshi-Medium.ttf > /dev/null 2>&1 ) &

/usr/bin/xinit /root/.xinitrc -- :0 vt1 -nolisten tcp -nocursor -s 0 -dpms &

# ── Hand off to systemd as PID 1 for remaining services ──
exec /lib/systemd/systemd

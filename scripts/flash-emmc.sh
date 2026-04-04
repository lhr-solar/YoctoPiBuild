#!/bin/bash
# Flash photon-image to CM5 eMMC over USB-C
#
# Prerequisites:
#   1. Install rpiboot:  sudo apt install rpiboot
#      OR build from source:
#        git clone --depth=1 https://github.com/raspberrypi/usbboot
#        cd usbboot && make && sudo make install
#   2. Place jumper on J2 (nRPIBOOT) on the CM5 IO Board
#   3. Connect USB-C slave port (J11) to this host PC
#   4. Power the CM5 IO Board

set -euo pipefail

IMAGE="${1:-build/tmp/deploy/images/raspberrypi-cm5-io-board/photon-image-raspberrypi-cm5-io-board.rootfs.wic.bz2}"
DEVICE="${2:-}"

if [ ! -f "$IMAGE" ]; then
    echo "ERROR: Image not found: $IMAGE"
    echo "Usage: $0 [image.wic.bz2] [/dev/sdX]"
    exit 1
fi

# Step 1: rpiboot — exposes eMMC as USB mass storage
echo ">>> Starting rpiboot (CM5 eMMC will appear as /dev/sdX)..."
sudo rpiboot

# Step 2: Wait for device
if [ -z "$DEVICE" ]; then
    echo ">>> Waiting for eMMC block device..."
    sleep 3
    echo ""
    echo "Available block devices:"
    lsblk -d -o NAME,SIZE,MODEL | grep -v loop
    echo ""
    read -rp "Enter device (e.g. /dev/sda): " DEVICE
fi

[ -b "$DEVICE" ] || { echo "ERROR: $DEVICE is not a block device"; exit 1; }

# Step 3: Safety check
echo ""
echo ">>> WARNING: ALL data on $DEVICE will be erased!"
read -rp "Type 'yes' to continue: " CONFIRM
[ "$CONFIRM" = "yes" ] || { echo "Aborted."; exit 1; }

# Step 4: Flash
echo ">>> Flashing $IMAGE -> $DEVICE ..."
bzcat "$IMAGE" | sudo dd of="$DEVICE" bs=4M status=progress conv=fsync
sync

echo ""
echo ">>> Done!"
echo "    1. Remove J2 jumper"
echo "    2. Disconnect USB-C"
echo "    3. Power cycle the CM5 IO Board"

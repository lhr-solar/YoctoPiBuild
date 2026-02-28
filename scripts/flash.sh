#!/usr/bin/env bash
# Flash the built .wic image to an SD card or CM4 eMMC (via usbboot).
# Usage: ./scripts/flash.sh /dev/sdX
set -euo pipefail

IMAGE_DIR="build/tmp/deploy/images/raspberrypicm4-io"
IMAGE=$(ls "${IMAGE_DIR}"/*.wic.bz2 | tail -1)
TARGET="${1:?Usage: flash.sh /dev/sdX}"

echo "Flashing ${IMAGE} to ${TARGET} ..."
echo "WARNING: This will ERASE all data on ${TARGET}"
read -p "Continue? [y/N] " confirm
[[ "${confirm}" == "y" ]] || exit 1

bzcat "${IMAGE}" | sudo dd of="${TARGET}" bs=4M status=progress conv=fsync
sync
echo "Done. Eject and insert into CM4."

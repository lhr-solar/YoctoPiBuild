#!/bin/bash
# Apply Photon fast-boot rpi-eeprom bootloader config to the CM5 SPI flash.
#
# Run this ON TARGET (the CM5 itself) after the image has booted once.
# Settings persist in SPI flash and survive eMMC reflashes, so this is
# a one-time operation per module.
#
# Steps:
#   1. Flash and boot the Photon image normally.
#   2. SSH in (or use a local console).
#   3. Run this script.
#   4. Reboot. New boot timing takes effect from the next power cycle.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CFG="${SCRIPT_DIR}/eeprom-config.txt"

if [ ! -f "$CFG" ]; then
    echo "ERROR: $CFG not found next to this script" >&2
    exit 1
fi

if ! command -v rpi-eeprom-config >/dev/null 2>&1; then
    echo "ERROR: rpi-eeprom-config not available on this system." >&2
    echo "       Install the rpi-eeprom package or run on a Raspberry Pi target." >&2
    exit 1
fi

echo ">>> Current EEPROM bootloader config:"
sudo rpi-eeprom-config
echo ""
echo ">>> Applying fast-boot config from: $CFG"
sudo rpi-eeprom-config --apply "$CFG"
echo ""
echo ">>> New EEPROM config staged. Reboot to take effect:"
echo "    sudo reboot"

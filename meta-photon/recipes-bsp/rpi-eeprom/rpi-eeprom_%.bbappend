# Widen COMPATIBLE_MACHINE so rpi-eeprom builds for raspberrypi-cm5-io-board.
#
# meta-raspberrypi's rpi-eeprom_git.bb declares:
#   COMPATIBLE_MACHINE = "raspberrypi4|raspberrypi4-64|raspberrypi5"
#
# That list was never updated when raspberrypi-cm5-io-board was added, even
# though the recipe already ships the BCM2712 bootloader firmware
# (firmware-2712/) which is exactly what the CM5 SPI EEPROM needs. Without
# this append, bitbake skips the recipe entirely for our machine and
# `rpi-eeprom-config` is not available on target, so we cannot tune the
# pre-kernel bootloader settings (BOOT_UART, BOOT_ORDER, WAKE_ON_GPIO, etc.).

COMPATIBLE_MACHINE:append = "|raspberrypi-cm5-io-board"

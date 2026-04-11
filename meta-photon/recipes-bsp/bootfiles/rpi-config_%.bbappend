# Split PHOTON_EXTRA_CONFIG into per-directive lines in config.txt.
#
# Background: meta-raspberrypi's rpi-config_git.bb writes RPI_EXTRA_CONFIG to
# config.txt via a single `printf "${RPI_EXTRA_CONFIG}\n"`, which produces one
# very long space-separated line. The BCM2712 firmware on Raspberry Pi 5 / CM5
# corrupts config.txt when any line exceeds 80 characters, and the base recipe
# itself `bbwarn`s about this (see raspberrypi/firmware#1848 and
# Evilpaul/RPi-config#9, linked from the base recipe).
#
# Strategy: kas.yml sets `RPI_EXTRA_CONFIG = ""` to suppress the unsafe append
# and defines `PHOTON_EXTRA_CONFIG` with the full list of directives. This
# bbappend runs after the base do_deploy and writes each directive to a
# separate line in config.txt, staying well within the 80-char budget.

do_deploy:append() {
    CONFIG=${DEPLOYDIR}/${BOOTFILES_DIR_NAME}/config.txt
    if [ -n "${PHOTON_EXTRA_CONFIG}" ]; then
        echo ""                                >> $CONFIG
        echo "# Photon kiosk config (per-line, added by rpi-config bbappend)" >> $CONFIG
        for directive in ${PHOTON_EXTRA_CONFIG}; do
            echo "$directive" >> $CONFIG
        done
    fi
}

SUMMARY = "Photon Dashboard kiosk image for Raspberry Pi"

inherit core-image testimage

# Keep developer conveniences opt-in so the default image boots leaner.
PHOTON_IMAGE_FEATURES ?= ""
IMAGE_FEATURES += " ${PHOTON_IMAGE_FEATURES}"

# Core packages (stripped for fastest boot)
IMAGE_INSTALL:append = " \
    packagegroup-core-boot \
    openssh-sshd \
    vulkan-loader \
    mesa \
    mesa-vulkan-drivers \
    libgl-mesa \
    \
    xserver-xorg \
    xserver-xorg-module-libwfb \
    xf86-video-modesetting \
    xf86-input-evdev \
    libxcb \
    libx11 \
    libxext \
    xinit \
    xauth \
    \
    can-utils \
    \
    iproute2 \
    wpa-supplicant \
    linux-firmware-rpidistro-bcm43455 \
    \
    systemd-analyze \
    \
    photon-dashboard \
    photon-can-dbc \
"

# rpi-eeprom was added for one-time SPI bootloader config flashing, then
# removed: the EEPROM config persists in SPI flash on the CM5 module and
# is unaffected by image reflashes. Keeping the rpi-eeprom package (plus
# its Python + pycryptodomex runtime chain) in the rootfs just bloats the
# image ~160MB raw / ~30MB bz2. Re-add to IMAGE_INSTALL temporarily if you
# ever need to change EEPROM settings again.
# The meta-photon/recipes-bsp/rpi-eeprom/rpi-eeprom_%.bbappend that widens
# COMPATIBLE_MACHINE to include raspberrypi-cm5-io-board is kept — it has
# zero image-size cost and unblocks re-enabling the package later.

# USB HID is typically built into linux-raspberrypi (no kernel-module-usbhid IPK);
# opkg fails rootfs if IMAGE_INSTALL names a non-existent module package.

# Image format: .wic for direct SD/eMMC flash
IMAGE_FSTYPES = "wic wic.bz2"
WKS_FILE = "sdimage-raspberrypi.wks"

# Static IP on eth0 for SSH over direct ethernet cable
# Laptop should use 192.168.1.1/24, Pi will be at 192.168.1.100
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

ethernet_static_ip() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nName=eth0\n\n[Network]\nAddress=192.168.1.100/24\nDHCP=yes\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/20-eth0-static.network
}

ROOTFS_POSTPROCESS_COMMAND += "ethernet_static_ip;"

# Bring up the MCP2515 CAN interface at 250 kbps (car bus rate) on boot.
setup_can0() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nName=can0\n\n[CAN]\nBitRate=250K\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/40-can0.network
}

ROOTFS_POSTPROCESS_COMMAND += "setup_can0;"


setup_wifi() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant
    printf 'ctrl_interface=/var/run/wpa_supplicant\nctrl_interface_group=0\nupdate_config=1\ncountry=US\n\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf
    printf 'network={\n    ssid="Texan-MyCampusNet-2G"\n    psk="Orange-Woodland-50$"\n    key_mgmt=WPA-PSK\n    priority=1\n}\n\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf

    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nName=wlan0\n\n[Network]\nDHCP=yes\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/30-wlan0-dhcp.network

    # Keep Wi-Fi available, but let it start later than the early kiosk path.
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/multi-user.target.wants
    ln -sf /lib/systemd/system/wpa_supplicant@.service \
        ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/multi-user.target.wants/wpa_supplicant@wlan0.service
}

ROOTFS_POSTPROCESS_COMMAND += "setup_wifi;"

disable_suspend() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/sleep.target
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/suspend.target
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/hibernate.target
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/hybrid-sleep.target
}

ROOTFS_POSTPROCESS_COMMAND += "disable_suspend;"

disable_wait_online() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-networkd-wait-online.service
}

ROOTFS_POSTPROCESS_COMMAND += "disable_wait_online;"

disable_slow_services() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system
    # Bluetooth — not used by the dashboard
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/bluetooth.service
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/bluetooth.target
    # Avahi (mDNS) — not needed for kiosk
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/avahi-daemon.service
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/avahi-daemon.socket
    # (USB gadget serial getty@ttyGS0 intentionally left enabled for debug access)
    # Journal flush — delays boot waiting for persistent journal
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-journal-flush.service
    # Login tracking — not needed for kiosk
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-update-utmp.service
    # Periodic cleanup timer — not needed at boot
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-tmpfiles-clean.timer
    # SSH daemon — use socket activation instead (starts on first connection)
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/sshd.service
    # getty@tty1 intentionally left enabled so a login prompt appears on HDMI
    # whenever photon-dashboard is not running. The dashboard service has
    # Conflicts=getty@tty1.service so systemd will stop the getty as soon as
    # the dashboard takes over. When the dashboard fails or is masked you
    # get a login prompt instead of a black screen.
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/autovt@tty1.service
    # Telephony — not needed
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/ofono.service
    # WiFi — keep installed but defer off boot path (start manually when needed)
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/wpa_supplicant@wlan0.service
    # ldconfig regenerates /etc/ld.so.cache. On a read-only embedded rootfs
    # the cache is already correct from build time. 164ms on the critical chain.
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/ldconfig.service
    # sysusers creates system users from /usr/lib/sysusers.d. All accounts are
    # baked in at image build time so this is pure overhead. ~45ms critical chain.
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-sysusers.service
    # update-done touches /etc/.updated and /var/.updated after package updates
    # and is what pulls ldconfig into the boot chain. We do not do live package
    # updates, so it is unnecessary.
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/systemd-update-done.service
}

ROOTFS_POSTPROCESS_COMMAND += "disable_slow_services;"

# SSH socket activation — zero boot cost, starts sshd on first connection
setup_ssh_socket() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/sockets.target.wants
    ln -sf /lib/systemd/system/sshd.socket \
        ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/sockets.target.wants/sshd.socket
}

ROOTFS_POSTPROCESS_COMMAND += "setup_ssh_socket;"

# USB-C gadget serial console (ttyGS0) — lets the dev laptop pull a getty shell
# over the same USB-C cable used for rpiboot. Requires dtoverlay=dwc2 (set in
# kas.yml PHOTON_EXTRA_CONFIG) and the g_serial module auto-loaded at boot.
# Enabling the getty service explicitly is required because systemd's
# serial-getty-generator only auto-spawns for hardware serial ports listed on
# the kernel `console=` cmdline, not for USB gadget serial.
setup_usb_gadget_serial() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/modules-load.d
    echo "g_serial" > ${IMAGE_ROOTFS}${sysconfdir}/modules-load.d/usb-gadget-serial.conf
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/getty.target.wants
    ln -sf /lib/systemd/system/serial-getty@.service \
        ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/getty.target.wants/serial-getty@ttyGS0.service
}

ROOTFS_POSTPROCESS_COMMAND += "setup_usb_gadget_serial;"

# Explicitly enable getty on tty1 (the autovt@tty1 mask is still in place to
# avoid VT race with the dashboard, so we need a direct getty@tty1 instance).
setup_tty1_getty() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/getty.target.wants
    ln -sf /lib/systemd/system/getty@.service \
        ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/getty.target.wants/getty@tty1.service
}

ROOTFS_POSTPROCESS_COMMAND += "setup_tty1_getty;"

# Match Xorg to vc4 by driver name, not card number. The Pi exposes two DRM
# devices (vc4 = display, v3d = render-only) and their card0/card1 ordering
# can flip between kernels. MatchDriver binds this OutputClass to whichever
# /dev/dri/card* is the vc4 driver, and PrimaryGPU forces it as primary so
# Xorg does not auto-elect v3d (which has no CRTCs -> "no screens found").
setup_xorg_modesetting() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf.d
    cat > ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf.d/10-modesetting.conf <<'EOF'
Section "OutputClass"
    Identifier "vc4"
    MatchDriver "vc4"
    Driver "modesetting"
    Option "PrimaryGPU" "true"
EndSection
EOF
}

ROOTFS_POSTPROCESS_COMMAND += "setup_xorg_modesetting;"

# Disable MAC Randomization so the apartment network allows the connection
setup_journald_volatile() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd
    printf '[Journal]\nStorage=volatile\nRuntimeMaxUse=8M\nForwardToSyslog=no\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/journald.conf
}

ROOTFS_POSTPROCESS_COMMAND += "setup_journald_volatile;"

setup_persistent_mac() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nOriginalName=*\n\n[Link]\nMACAddressPolicy=none\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/99-default.link
}
ROOTFS_POSTPROCESS_COMMAND += "setup_persistent_mac;"

# Pre-configure WiFi so we can SSH immediately after flash
setup_auto_wifi() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant
    printf 'ctrl_interface=/var/run/wpa_supplicant\nupdate_config=1\nnetwork={\n    ssid="Texan-MyCampusNet-5G"\n    psk="Orange-Woodland-50$"\n}\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf
    # Ensure systemd auto-starts wlan0
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/multi-user.target.wants
    ln -sf /lib/systemd/system/wpa_supplicant@.service ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/multi-user.target.wants/wpa_supplicant@wlan0.service
    
    # Force systemd-networkd to ask for an IPv4 address
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nName=wlan*\n\n[Network]\nDHCP=ipv4\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/80-wifi-dhcp.network
}
ROOTFS_POSTPROCESS_COMMAND += "setup_auto_wifi;"


# Cleanup legacy boot files to shrink image and speed up firmware scanning.
# Only CM5 specific files are kept.
cleanup_boot_partition() {
    # If the boot files are staged in the rootfs /boot directory
    if [ -d ${IMAGE_ROOTFS}/boot/overlays ]; then
        # Remove legacy Pi 3/4 firmware
        rm -f ${IMAGE_ROOTFS}/boot/start*.elf
        rm -f ${IMAGE_ROOTFS}/boot/fixup*.dat
        rm -f ${IMAGE_ROOTFS}/boot/bootcode.bin
        
        # Remove unused DTBs (keeping only CM5 variants)
        # We keep cm5 variants, remove the others
        find ${IMAGE_ROOTFS}/boot/ -name "bcm2711-*.dtb" -delete
        find ${IMAGE_ROOTFS}/boot/ -name "bcm28*.dtb" -delete
        find ${IMAGE_ROOTFS}/boot/ -name "bcm270*.dtb" -delete
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "cleanup_boot_partition;"

# Runtime testing
TEST_SUITES:append = " photon"

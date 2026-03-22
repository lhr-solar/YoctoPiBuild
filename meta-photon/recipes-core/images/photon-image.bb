SUMMARY = "Photon Dashboard kiosk image for Raspberry Pi"

inherit core-image testimage

# Base features
IMAGE_FEATURES += " \
    ssh-server-openssh \
    debug-tweaks \
"

# Core packages
IMAGE_INSTALL:append = " \
    packagegroup-core-boot \
    packagegroup-base \
    kernel-modules \
    \
    vulkan-loader \
    mesa \
    mesa-vulkan-drivers \
    libgl-mesa \
    \
    xserver-xorg \
    xf86-video-modesetting \
    xf86-input-evdev \
    libxcb \
    libx11 \
    libxext \
    xinit \
    xauth \
    xterm \
    xset \
    xrandr \
    \
    v4l-utils \
    \
    can-utils \
    \
    vulkan-tools \
    \
    iproute2 \
    wpa-supplicant \
    avahi-daemon \
    linux-firmware-rpidistro-bcm43455 \
    \
    photon-dashboard \
"

# Image format: .wic for direct SD/eMMC flash
IMAGE_FSTYPES = "wic wic.bz2 ext4"
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

create_xorg_conf() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/X11
    printf 'Section "ServerFlags"\n    Option "AIGLX" "off"\nEndSection\n\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf
    printf 'Section "Device"\n    Identifier "Card0"\n    Driver "modesetting"\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf
    printf '    Option "DRI" "3"\nEndSection\n\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf
    printf 'Section "Screen"\n    Identifier "Screen0"\n    Device "Card0"\nEndSection\n\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf
    printf 'Section "ServerLayout"\n    Identifier "Layout0"\n    Screen "Screen0"\nEndSection\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/X11/xorg.conf
}

ROOTFS_POSTPROCESS_COMMAND += "create_xorg_conf;"

setup_wifi() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant
    printf 'ctrl_interface=/var/run/wpa_supplicant\nctrl_interface_group=0\nupdate_config=1\ncountry=US\n\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf
    printf 'network={\n    ssid="Texan-MyCampusNet-2G"\n    psk="Orange-Woodland-50$"\n    key_mgmt=WPA-PSK\n    priority=1\n}\n\n' \
        >> ${IMAGE_ROOTFS}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf

    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/network
    printf '[Match]\nName=wlan0\n\n[Network]\nDHCP=yes\n' \
        > ${IMAGE_ROOTFS}${sysconfdir}/systemd/network/30-wlan0-dhcp.network

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


# Runtime testing
TEST_SUITES:append = " photon"

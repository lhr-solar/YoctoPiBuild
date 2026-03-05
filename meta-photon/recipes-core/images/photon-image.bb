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
    \
    xserver-xorg \
    xf86-video-modesetting \
    libxcb \
    libx11 \
    libxext \
    xinit \
    xterm \
    \
    v4l-utils \
    \
    can-utils \
    \
    iproute2 \
    wpa-supplicant \
    \
    photon-dashboard \
"

# Image format: .wic for direct SD/eMMC flash
IMAGE_FSTYPES = "wic wic.bz2 ext4"
WKS_FILE = "sdimage-raspberrypi.wks"

# Runtime testing
TEST_SUITES:append = " photon"

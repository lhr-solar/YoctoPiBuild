SUMMARY = "Photon DashboardOnly application"
LICENSE = "CLOSED"

# Source: https://github.com/lhr-solar/Photon  branch: driver-dash
SRC_URI = "git://github.com/lhr-solar/Photon.git;branch=driver-dash;protocol=https"
# AUTOREV tracks HEAD of driver-dash. For reproducible releases, pin to a specific commit:
# SRCREV = "abc123..."
SRCREV = "${AUTOREV}"
PV = "0.1+git${SRCPV}"
S = "${WORKDIR}/git"

inherit cmake systemd

# Confirmed from repo analysis:
#   - Target name:      DashboardOnly  (built in photon/ subdirectory)
#   - No CMake -D flags needed; DashboardOnly is always built alongside Photon
#   - No install() directives in CMakeLists.txt — manual install below
#   - glslangValidator invoked via custom compile_shader() in kernels/CMakeLists.txt
#   - Python3 used to convert SPIR-V .spv -> C++ headers (spv_to_header.py)
#   - find_package(XCB REQUIRED) on Linux
#   - Vulkan found via find_library() -- needs libvulkan.so in sysroot

DEPENDS = " \
    cmake-native \
    python3-native \
    glslang-native \
    libxcb \
    libx11 \
    vulkan-loader \
    vulkan-headers \
    v4l-utils \
    jpeg \
"

# Platform flags: non-Windows path sets VK_USE_PLATFORM_XCB_KHR automatically
# in root CMakeLists.txt (checked via if(NOT WIN32)), so no extra flag needed.
EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON \
"

# Systemd service
SYSTEMD_SERVICE:${PN} = "photon-dashboard.service"
SYSTEMD_AUTO_ENABLE = "enable"

SRC_URI += " \
    file://photon-dashboard.service \
    file://xinitrc \
    file://photon-init.sh \
"



do_install() {
    # Binary is at <build>/photon/DashboardOnly (CMake subdir output)
    install -d ${D}/usr/bin
    install -m 0755 ${B}/photon/DashboardOnly ${D}/usr/bin/DashboardOnly

    # Satoshi font for dashboard UI
    install -d ${D}/usr/share/fonts
    install -m 0644 ${S}/fonts/Satoshi-Medium.ttf ${D}/usr/share/fonts/

    # Systemd unit
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/photon-dashboard.service \
        ${D}${systemd_system_unitdir}/

    # X11 kiosk launcher
    install -d ${D}/root
    install -m 0755 ${WORKDIR}/xinitrc ${D}/root/.xinitrc

    # Custom fast-init script (kernel boots with init=/opt/photon-init.sh)
    install -d ${D}/opt
    install -m 0755 ${WORKDIR}/photon-init.sh ${D}/opt/photon-init.sh
}

FILES:${PN} += " \
    /usr/bin/DashboardOnly \
    /usr/share/fonts/Satoshi-Medium.ttf \
    ${systemd_system_unitdir}/photon-dashboard.service \
    /root/.xinitrc \
    /opt/photon-init.sh \
"

# NOTE: glslang-native may not exist in meta-oe scarthgap.
# If missing, create:
#   meta-photon/recipes-devtools/glslang/glslang_<ver>.bb
#   with BBCLASSEXTEND = "native" and inherit cmake.

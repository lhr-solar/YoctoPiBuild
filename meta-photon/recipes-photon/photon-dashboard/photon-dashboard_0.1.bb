SUMMARY = "Photon application (driver dashboard view)"
LICENSE = "CLOSED"

# driver-dash is no longer maintained; the dashboard now lives as a tab in
# the main Photon app on staging, selected at boot via PHOTON_DASHBOARD.
# Point this at whatever branch/tag carries the merged work once pushed.
SRC_URI = "git://github.com/lhr-solar/Photon.git;branch=dash2staging;protocol=https"
# Keep flash images reproducible. Update this deliberately after validating a
# dashboard revision on the kart.
SRCREV = "b2bcad583c7b66162a3943ea35140052994b146f"
PV = "0.1+git${SRCPV}"
S = "${WORKDIR}/git"

inherit cmake pkgconfig systemd

# Updated from repo analysis (staging):
#   - Target name:      Photon  (built in photon/ subdirectory; no separate
#     DashboardOnly target anymore — dashboard is a tab, picked via
#     PHOTON_DASHBOARD env var, see xinitrc)
#   - No install() directives in CMakeLists.txt — manual install below
#   - glslangValidator invoked via custom compile_shader() in kernels/CMakeLists.txt
#   - Python3 used to convert SPIR-V .spv -> C++ headers (spv_to_header.py)
#   - find_package(XCB REQUIRED) and find_package(SDL3) on Linux
#   - Vulkan found via find_library() -- needs libvulkan.so in sysroot
#
# KNOWN GAP: staging requires SDL3, which has no recipe yet in meta-photon or
# the configured poky/meta-openembedded (scarthgap) layers. This build will
# not configure until an SDL3 recipe is added (see meta-photon/recipes-graphics).

DEPENDS = " \
    cmake-native \
    python3-native \
    glslang-native \
    libxcb \
    libx11 \
    libsdl3 \
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
    -DPHOTON_DASHBOARD_ONLY=ON \
"

# Systemd service
SYSTEMD_SERVICE:${PN} = "photon-dashboard.service"
SYSTEMD_AUTO_ENABLE = "enable"

SRC_URI += " \
    file://photon-dashboard.service \
    file://xinitrc \
    file://photon-init.sh \
    file://50-photon-cams.rules \
"



do_install() {
    # Binary lands in <build>/bin (CMAKE_RUNTIME_OUTPUT_DIRECTORY in the root
    # CMakeLists). The dashboard font (Inter) is embedded in the binary, so no
    # separate font install.
    install -d ${D}/usr/bin
    install -m 0755 ${B}/bin/Photon ${D}/usr/bin/Photon

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

    # udev rule: stable /dev/cam-{left,right,rear} symlinks per USB port
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/50-photon-cams.rules \
        ${D}${sysconfdir}/udev/rules.d/
}

FILES:${PN} += " \
    /usr/bin/Photon \
    ${systemd_system_unitdir}/photon-dashboard.service \
    /root/.xinitrc \
    /opt/photon-init.sh \
    ${sysconfdir}/udev/rules.d/50-photon-cams.rules \
"

# NOTE: glslang-native may not exist in meta-oe scarthgap.
# If missing, create:
#   meta-photon/recipes-devtools/glslang/glslang_<ver>.bb
#   with BBCLASSEXTEND = "native" and inherit cmake.

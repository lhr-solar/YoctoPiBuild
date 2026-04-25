SUMMARY = "Super Mario Kart easter-egg game launched by photon-dashboard"
DESCRIPTION = "vmbatlle/super-mario-kart with CAN-over-UDP input, spawned by \
the dashboard when both bottom corners are clicked. Steering wheel, accelerator \
and brake pedal drive the kart via 127.0.0.1:48655."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

# Upstream is vmbatlle/super-mario-kart. Our patches (cross-platform CMakeLists,
# C++17 portability fixes, IPC input layer, std::random_shuffle replacement)
# live in files/ and are applied via SRC_URI patches. If/when this is forked
# under lhr-solar, switch to the fork URL and drop the patches.
SRC_URI = " \
    git://github.com/vmbatlle/super-mario-kart.git;branch=master;protocol=https \
    file://0001-add-cross-platform-cmakelists.patch \
    file://0002-cxx17-portability-fixes.patch \
    file://0003-ipc-input-udp-listener.patch \
    file://photon-mariokart-prewarm.service \
"
SRCREV = "${AUTOREV}"
PV = "0.1+git${SRCPV}"
S = "${WORKDIR}/git"

inherit cmake pkgconfig systemd

SYSTEMD_SERVICE:${PN} = "photon-mariokart-prewarm.service"
SYSTEMD_AUTO_ENABLE = "enable"

# CMakeLists lives under src/ in the upstream repo.
OECMAKE_SOURCEPATH = "${S}/src"

DEPENDS = " \
    cmake-native \
    sfml \
    virtual/libgl \
    openal-soft \
"

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON \
"

# Strip release binary aggressively.
TARGET_CFLAGS:append   = " -ffunction-sections -fdata-sections -O3"
TARGET_CXXFLAGS:append = " -ffunction-sections -fdata-sections -O3"
TARGET_LDFLAGS:append  = " -Wl,--gc-sections -Wl,-s"

# rm_work safe; only one binary, small asset tree.
INSANE_SKIP:${PN} += "ldflags"

# CMakeLists install rules already place the binary under /opt/super-mario-kart/
# and copy the assets/ tree there. PHOTON_SMK_DIR in photon-dashboard defaults
# to that path, so no extra config is needed.
do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/photon-mariokart-prewarm.service \
        ${D}${systemd_system_unitdir}/
}

FILES:${PN} += " \
    /opt/super-mario-kart \
    /opt/super-mario-kart/assets \
    ${systemd_system_unitdir}/photon-mariokart-prewarm.service \
"

# Photon dashboard pulls this in as a runtime dependency so launching the
# easter-egg actually finds the binary on the image.
RDEPENDS:${PN} = "sfml openal-soft"

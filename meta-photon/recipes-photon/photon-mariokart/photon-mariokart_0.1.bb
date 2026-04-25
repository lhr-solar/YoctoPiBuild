SUMMARY = "Super Mario Kart easter-egg game launched by photon-dashboard"
DESCRIPTION = "vmbatlle/super-mario-kart with CAN-over-UDP input, spawned by \
the dashboard when both bottom corners are clicked. Steering wheel, accelerator \
and brake pedal drive the kart via 127.0.0.1:48655."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e62637ea8a114355b985fd86c9ffbd6e"

# Upstream is vmbatlle/super-mario-kart. Our patches (cross-platform CMakeLists,
# C++17 portability fixes, IPC input layer, std::random_shuffle replacement)
# live in files/ and are applied via SRC_URI patches. If/when this is forked
# under lhr-solar, switch to the fork URL and drop the patches.
SRC_URI = " \
    git://github.com/vmbatlle/super-mario-kart.git;branch=master;protocol=https \
    file://0001-add-cross-platform-cmakelists.patch \
    file://0002-cxx17-portability-fixes.patch \
    file://0003-ipc-input-udp-listener.patch \
"
SRCREV = "${AUTOREV}"
PV = "0.1+git${SRCPV}"
S = "${WORKDIR}/git"

inherit cmake pkgconfig

# CMakeLists lives under src/ in the upstream repo.
OECMAKE_SOURCEPATH = "${S}/src"

DEPENDS = " \
    cmake-native \
    sfml \
    libgl \
    openal-soft \
"

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
"

# CMakeLists install rules already place the binary under /opt/super-mario-kart/
# and copy the assets/ tree there. PHOTON_SMK_DIR in photon-dashboard defaults
# to that path, so no extra config is needed.
FILES:${PN} += " \
    /opt/super-mario-kart \
    /opt/super-mario-kart/assets \
"

# Photon dashboard pulls this in as a runtime dependency so launching the
# easter-egg actually finds the binary on the image.
RDEPENDS:${PN} = "sfml openal-soft"

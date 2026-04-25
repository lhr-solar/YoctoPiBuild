SUMMARY = "Simple and Fast Multimedia Library"
DESCRIPTION = "SFML provides a simple interface to system, window, graphics, audio and network."
HOMEPAGE = "https://www.sfml-dev.org/"
LICENSE = "Zlib"
LIC_FILES_CHKSUM = "file://license.md;md5=3ddaca891aa952cf42b3ba95d3b853e7"

SRC_URI = "git://github.com/SFML/SFML.git;branch=2.6.x;protocol=https"
SRCREV = "5383d2b3948f805af55c9f8a4587ac72ec5981d1"
S = "${WORKDIR}/git"

inherit cmake pkgconfig

DEPENDS = " \
    flac \
    libogg \
    libvorbis \
    openal-soft \
    freetype \
    libx11 \
    libxrandr \
    libxcursor \
    libxi \
    virtual/libgl \
    virtual/egl \
    udev \
"

EXTRA_OECMAKE = " \
    -DSFML_BUILD_EXAMPLES=OFF \
    -DSFML_BUILD_DOC=OFF \
    -DSFML_BUILD_TEST_SUITE=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
"

FILES:${PN} += " \
    ${libdir}/libsfml-*.so.* \
"

FILES:${PN}-dev += " \
    ${libdir}/libsfml-*.so \
    ${libdir}/cmake/SFML \
"

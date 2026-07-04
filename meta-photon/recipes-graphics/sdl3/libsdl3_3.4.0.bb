SUMMARY = "Simple DirectMedia Layer 3 - window/input/Vulkan-surface abstraction"
HOMEPAGE = "https://www.libsdl.org/"
LICENSE = "Zlib"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=cbf0e3161523f9a9315b6b915c5c4457"

SRC_URI = "git://github.com/libsdl-org/SDL.git;tag=release-3.4.0;branch=main;protocol=https"
PV = "3.4.0"
S = "${WORKDIR}/git"

inherit cmake pkgconfig

# Photon only needs windowing + Vulkan surface creation over X11 (the image
# boots into Xorg via xinit — see photon-dashboard recipe). Other backends
# (Wayland, KMSDRM, audio/haptic/joystick subsystems) are left at their
# upstream defaults off/on as appropriate for a headless-ish kiosk target;
# trim further here if image size becomes a concern.
DEPENDS = " \
    libx11 \
    libxext \
    libxrandr \
    libxcursor \
    libxi \
    libxfixes \
    libxss \
"

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DSDL_STATIC=ON \
    -DSDL_SHARED=ON \
    -DSDL_VULKAN=ON \
    -DSDL_X11=ON \
    -DSDL_X11_XTEST=OFF \
    -DSDL_WAYLAND=OFF \
    -DSDL_KMSDRM=OFF \
    -DSDL_TESTS=OFF \
    -DSDL_EXAMPLES=OFF \
"

FILES:${PN} += " \
    ${libdir}/libSDL3.so.* \
    ${datadir}/licenses/SDL3 \
"

FILES:${PN}-dev += " \
    ${libdir}/libSDL3.so \
    ${libdir}/cmake/SDL3 \
    ${libdir}/pkgconfig/sdl3.pc \
"

BBCLASSEXTEND = "native nativesdk"

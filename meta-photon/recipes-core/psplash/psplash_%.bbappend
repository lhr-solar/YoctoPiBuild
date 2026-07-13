FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Replace the default Poky logo with the Photon boot splash photo.
# The base psplash recipe converts the PNG to psplash's compiled-in .h
# format at build time (via make-image-header.sh + gdk-pixbuf-native),
# so a plain PNG here is all that's needed. psplash draws it centered
# at native resolution on /dev/fb0 (requires CONFIG_DRM_FBDEV_EMULATION,
# re-enabled in the fast-boot kernel fragment).
SPLASH_IMAGES = "file://psplash-photon-img.png;outsuffix=default"

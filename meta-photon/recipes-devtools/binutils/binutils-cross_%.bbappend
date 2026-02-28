# Disable gold linker — gold fails to build on Ubuntu 22.04 host due to
# DT_TEXTREL-in-PIE being treated as a hard error by the host linker.
# Gold is not needed for aarch64 cross-compilation.
EXTRA_OECONF:remove = "--enable-gold"
EXTRA_OECONF:append = " --disable-gold"

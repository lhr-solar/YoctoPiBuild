# GCC 13 on Ubuntu 24.04 enables PIE by default, which breaks icu-native's
# genccode link step. Disable PIE for the native host build only.
BUILD_CFLAGS:append = " -fno-pie"
BUILD_CXXFLAGS:append = " -fno-pie"
BUILD_LDFLAGS:append = " -no-pie"

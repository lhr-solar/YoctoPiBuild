#!/bin/bash
# Cross-compile Photon (the sibling repo) for the CM5 using a real Yocto SDK
# sysroot (built once via scripts/build-sdk.sh + scripts/install-sdk.sh), so
# the resulting binary links against the exact glibc/Mesa/Vulkan/X11 versions
# actually on the Pi's rootfs — not generic host arm64 libs. Run inside WSL:
#   wsl bash scripts/build-arm64.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

PHOTON_DIR="${PHOTON_DIR:-${REPO_DIR}/../Photon}"
if [ ! -d "$PHOTON_DIR" ]; then
    echo "ERROR: Photon repo not found at ${PHOTON_DIR}." >&2
    echo "       Point PHOTON_DIR at your Photon checkout (expected as a sibling of YoctoPiBuild/)." >&2
    exit 1
fi
PHOTON_DIR="$(cd "${PHOTON_DIR}" && pwd)"

SDK_DIR="${PHOTON_SDK_DIR:-${REPO_DIR}/sdk}"
ENV_SETUP="$(find "${SDK_DIR}" -maxdepth 1 -name 'environment-setup-*' 2>/dev/null | head -n1)"
if [ -z "$ENV_SETUP" ]; then
    echo "ERROR: no Yocto SDK found at ${SDK_DIR}." >&2
    echo "       Build one once with: scripts/build-sdk.sh, then scripts/install-sdk.sh <installer>.sh" >&2
    echo "       Or point PHOTON_SDK_DIR at an existing SDK install." >&2
    exit 1
fi

echo ">>> Sourcing SDK environment: ${ENV_SETUP}"
# shellcheck disable=SC1090
source "${ENV_SETUP}"

# Shader compilation runs on the host (glslangValidator produces
# target-independent SPIR-V) — the SDK doesn't provide this, install natively.
if ! command -v glslangValidator >/dev/null 2>&1; then
    echo "ERROR: glslangValidator not found. Install it (e.g. 'sudo apt-get install glslang-tools')." >&2
    exit 1
fi

if [ -z "${OECORE_NATIVE_SYSROOT:-}" ]; then
    echo "ERROR: OECORE_NATIVE_SYSROOT not set — the SDK environment-setup script didn't source cleanly." >&2
    exit 1
fi
OE_TOOLCHAIN_FILE="${OECORE_NATIVE_SYSROOT}/usr/share/cmake/OEToolchainConfig.cmake"
if [ ! -f "$OE_TOOLCHAIN_FILE" ]; then
    echo "ERROR: expected OE CMake toolchain file not found: ${OE_TOOLCHAIN_FILE}" >&2
    exit 1
fi

BUILD_DIR="${REPO_DIR}/artifacts-arm64"
# The OE toolchain file sets CMAKE_SYSROOT/CMAKE_FIND_ROOT_PATH so
# find_library/find_package (e.g. Vulkan, XCB) resolve inside the SDK
# sysroot instead of failing or picking up host libs. -S points at Photon's
# source tree; -B keeps all build output out of the Photon repo entirely.
cmake -S "${PHOTON_DIR}" -B "${BUILD_DIR}" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="${OE_TOOLCHAIN_FILE}" \
    -DCMAKE_BUILD_TYPE=Release

cmake --build "${BUILD_DIR}" --target Photon -j"$(nproc)"

echo ">>> Built: ${BUILD_DIR}/photon/Photon"

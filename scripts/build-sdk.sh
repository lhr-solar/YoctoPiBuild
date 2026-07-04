#!/bin/bash
# One-time: build and install a Yocto SDK (cross-toolchain + matching
# sysroot) for photon-image. Run this once; Photon/scripts/build-arm64.sh
# then cross-compiles against it for every subsequent "update Photon on the
# Pi" without touching Yocto/kas/bitbake again.
#
# Unlike a generic Ubuntu aarch64 cross-compiler, this sysroot is built from
# the exact same recipe versions (glibc, Mesa, Vulkan, X11...) that end up on
# the Pi's rootfs, so binaries built against it are guaranteed ABI-compatible
# with the target.
#
# Usage:
#   scripts/build-sdk.sh                # build + install to ./sdk
#   scripts/build-sdk.sh /opt/photon-sdk  # install elsewhere

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_DIR}"

SDK_DEST="${1:-${REPO_DIR}/sdk}"

echo ">>> Building SDK for photon-image (this only needs to happen once per toolchain/library bump)..."
./kas-container shell kas-cm5.yml -c "bitbake photon-image -c populate_sdk"

INSTALLER="$(find build/tmp/deploy/sdk -maxdepth 1 -name '*-toolchain-*.sh' 2>/dev/null | head -n1)"
if [ -z "$INSTALLER" ]; then
    echo "ERROR: no SDK installer found under build/tmp/deploy/sdk." >&2
    exit 1
fi
echo "    using installer: ${INSTALLER}"

echo ">>> Installing SDK to ${SDK_DEST}..."
sh "${INSTALLER}" -d "${SDK_DEST}" -y

ENV_SETUP="$(find "${SDK_DEST}" -maxdepth 1 -name 'environment-setup-*' | head -n1)"
if [ -z "$ENV_SETUP" ]; then
    echo "ERROR: SDK installed but no environment-setup-* script found in ${SDK_DEST}." >&2
    exit 1
fi

echo ">>> Done. SDK environment script: ${ENV_SETUP}"
echo "    Point Photon/scripts/build-arm64.sh at it via PHOTON_SDK_DIR=${SDK_DEST}"
echo "    (or copy/symlink it to Photon/sdk, the default location the build script checks)."

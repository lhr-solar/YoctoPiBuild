#!/bin/bash
# Desktop-side, one-time(-ish): build a Yocto SDK installer (cross-toolchain +
# matching sysroot) for photon-image. Needs kas-container/Docker, so this runs
# wherever the Yocto build itself runs — NOT necessarily the same machine you
# deploy Photon updates from.
#
# Unlike a generic Ubuntu aarch64 cross-compiler, this sysroot is built from
# the exact same recipe versions (glibc, Mesa, Vulkan, X11...) that end up on
# the Pi's rootfs, so binaries built against it are guaranteed ABI-compatible
# with the target. rm_work is enabled in kas.yml, so the in-tree build sysroot
# gets deleted right after each recipe builds — populate_sdk is the one task
# that exports something that survives that cleanup.
#
# Output is a single self-extracting installer (build/tmp/deploy/sdk/*.sh).
# Copy that ONE FILE to whatever machine builds/deploys Photon (e.g. a
# laptop with no Docker/kas at all) and run scripts/install-sdk.sh there —
# the installer needs no bitbake/Docker/kas to run, only to build.
#
# Usage:
#   scripts/build-sdk.sh                # build only, print installer path
#   scripts/build-sdk.sh /some/dir       # also copy the installer there

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_DIR}"

echo ">>> Building SDK installer for photon-image (this only needs to happen once per toolchain/library bump)..."
./kas-container shell kas-cm5.yml -c "bitbake photon-image -c populate_sdk"

INSTALLER="$(find build/tmp/deploy/sdk -maxdepth 1 -name '*-toolchain-*.sh' 2>/dev/null | head -n1)"
if [ -z "$INSTALLER" ]; then
    echo "ERROR: no SDK installer found under build/tmp/deploy/sdk." >&2
    exit 1
fi
echo ">>> Installer built: ${INSTALLER}"

if [ $# -ge 1 ]; then
    DEST_DIR="$1"
    mkdir -p "$DEST_DIR"
    cp "$INSTALLER" "$DEST_DIR/"
    echo ">>> Copied installer to: ${DEST_DIR}/$(basename "$INSTALLER")"
fi

echo ">>> Copy that installer .sh to wherever you build/deploy Photon from, then run:"
echo "      scripts/install-sdk.sh <path-to-installer>.sh"

#!/bin/bash
# Laptop-side, one-time(-ish): install a Yocto SDK installer that was built
# elsewhere (see build-sdk.sh, which needs Docker/kas and typically runs on a
# different machine). This script only runs a self-extracting shell
# installer — no Docker, no bitbake, no kas required.
#
# Usage:
#   scripts/install-sdk.sh <path-to-installer>.sh [install-dir]
#   (install-dir defaults to YoctoPiBuild/sdk, the location build-arm64.sh checks)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

INSTALLER="${1:-}"
if [ -z "$INSTALLER" ] || [ ! -f "$INSTALLER" ]; then
    echo "ERROR: pass the path to the SDK installer .sh (built via scripts/build-sdk.sh)." >&2
    echo "Usage: $0 <path-to-installer>.sh [install-dir]" >&2
    exit 1
fi

SDK_DEST="${2:-${REPO_DIR}/sdk}"

echo ">>> Installing SDK from ${INSTALLER} to ${SDK_DEST}..."
sh "${INSTALLER}" -d "${SDK_DEST}" -y

ENV_SETUP="$(find "${SDK_DEST}" -maxdepth 1 -name 'environment-setup-*' | head -n1)"
if [ -z "$ENV_SETUP" ]; then
    echo "ERROR: SDK installed but no environment-setup-* script found in ${SDK_DEST}." >&2
    exit 1
fi

echo ">>> Done. SDK environment script: ${ENV_SETUP}"
echo "    scripts/build-arm64.sh will find it automatically at ${SDK_DEST}."

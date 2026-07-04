#!/bin/bash
# Update the Photon binary on the CM5 over its Wi-Fi AP — does NOT touch the
# OS image. Cross-compiles the sibling Photon repo locally against a real
# Yocto SDK sysroot (so it's ABI-matched to the Pi's actual glibc/Mesa/Vulkan/
# X11, not generic host libs), no kas/bitbake/Docker on the per-update path,
# and pushes just the binary.
#
# One-time setup (SDK build and Photon deploy can be different machines —
# the SDK installer is a single portable file, no Docker/kas needed to use it):
#   1. On whichever machine builds the Yocto image (needs Docker/kas-container):
#        scripts/build-sdk.sh
#      Copy the resulting build/tmp/deploy/sdk/*.sh installer to this machine.
#   2. Here (no Docker/kas needed): scripts/install-sdk.sh <path-to-installer>.sh
#
# Then, to connect and deploy:
#   1. Join the Pi's Wi-Fi AP: SSID "Photon-CM5", password "photon-dashboard"
#      (Pi is at 192.168.4.1; root login is passwordless)
#   2. From WSL:  scripts/deploy-to-pi.sh
#
# Run this INSIDE WSL — cross-compiling and ssh/scp both need it.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

PI_HOST="${PI_HOST:-192.168.4.1}"
PI_USER="${PI_USER:-root}"
SKIP_BUILD=0

for arg in "$@"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=1 ;;
        *)
            echo "ERROR: unknown argument: $arg" >&2
            echo "Usage: $0 [--skip-build]" >&2
            exit 1
            ;;
    esac
done

echo ">>> Checking the Pi is reachable at ${PI_USER}@${PI_HOST} (are you on the Photon-CM5 AP?)..."
if ! ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=accept-new "${PI_USER}@${PI_HOST}" true; then
    echo "ERROR: could not reach ${PI_USER}@${PI_HOST} over ssh." >&2
    echo "       Join the 'Photon-CM5' Wi-Fi AP (password: photon-dashboard) and retry." >&2
    exit 1
fi

if [ "$SKIP_BUILD" -eq 0 ]; then
    ./build-arm64.sh
fi

BINARY="../artifacts-arm64/photon/Photon"
if [ ! -f "$BINARY" ]; then
    echo "ERROR: $BINARY not found. Run without --skip-build first." >&2
    exit 1
fi

echo ">>> Copying binary to ${PI_USER}@${PI_HOST}:/usr/bin/Photon..."
scp -o StrictHostKeyChecking=accept-new "${BINARY}" "${PI_USER}@${PI_HOST}:/usr/bin/Photon.new"
ssh "${PI_USER}@${PI_HOST}" 'mv /usr/bin/Photon.new /usr/bin/Photon && chmod 0755 /usr/bin/Photon'

echo ">>> Restarting photon-dashboard.service..."
ssh "${PI_USER}@${PI_HOST}" 'systemctl restart photon-dashboard.service'

echo ">>> Done. Tail logs with: ssh ${PI_USER}@${PI_HOST} journalctl -u photon-dashboard -f"

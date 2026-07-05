#!/bin/sh
# Build the Photon dashboard and push it to the kart over WiFi (or ethernet).
#
# Usage:
#   ./photon-deploy.sh              # rebuild photon-dashboard, then deploy
#   ./photon-deploy.sh --no-build   # deploy the last-built binary as-is
#   PHOTON_HOST=192.168.1.100 ./photon-deploy.sh   # deploy over ethernet
#
# Prereqs: connected to the Pi (WiFi AP "Photon-CM5" -> Pi at 192.168.4.1,
# or direct ethernet -> Pi at 192.168.1.100). The recipe fetches Photon from
# GitHub (AUTOREV), so push your Photon changes to dash2staging first.
#
# Note: this only swaps /usr/bin/Photon. If recipes, dependencies, or system
# config changed, reflash the full image instead.

set -eu

HOST="${PHOTON_HOST:-192.168.4.1}"
USER="${PHOTON_USER:-root}"
SERVICE=photon-dashboard

cd "$(dirname "$0")"

if [ "${1:-}" != "--no-build" ]; then
    echo ">> Building photon-dashboard (fetches latest dash2staging)..."
    ./kas-container shell kas-cm5.yml -c "bitbake photon-dashboard"
fi

# Work dir survives rm_work via RM_WORK_EXCLUDE in kas.yml.
BIN=$(ls -t build/tmp/work/cortexa76-poky-linux/photon-dashboard/*/build/bin/Photon 2>/dev/null | head -1 || true)

if [ -z "$BIN" ]; then
    echo ">> Work dir not found, extracting from ipk..."
    IPK=$(ls -t build/tmp/deploy/ipk/cortexa76/photon-dashboard_*.ipk 2>/dev/null | head -1 || true)
    [ -n "$IPK" ] || { echo "ERROR: no built binary or ipk found — run without --no-build"; exit 1; }
    BIN=$(mktemp)
    ar p "$IPK" data.tar.zst | tar --zstd -xO ./usr/bin/Photon > "$BIN"
fi

echo ">> Deploying $BIN -> $USER@$HOST"
scp "$BIN" "$USER@$HOST:/tmp/Photon.new"
# mv works while the old binary is running; direct overwrite would hit ETXTBSY.
ssh "$USER@$HOST" "systemctl stop $SERVICE \
    && mv /tmp/Photon.new /usr/bin/Photon \
    && chmod 755 /usr/bin/Photon \
    && systemctl start $SERVICE \
    && systemctl --no-pager status $SERVICE | head -5"

echo ">> Done."

#!/bin/sh
set -eu

STAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="${1:-/tmp/photon-boot-${STAMP}}"

mkdir -p "$OUT_DIR"

systemd-analyze > "${OUT_DIR}/systemd-analyze.txt"
systemd-analyze blame > "${OUT_DIR}/blame.txt" || true
systemd-analyze critical-chain > "${OUT_DIR}/critical-chain.txt" || true
systemd-analyze critical-chain photon-dashboard.service > "${OUT_DIR}/critical-chain-photon-dashboard.txt" || true
systemd-analyze plot > "${OUT_DIR}/boot.svg" || true

journalctl -b -u photon-dashboard.service --no-pager > "${OUT_DIR}/photon-dashboard.log" || true
journalctl -b --no-pager | grep -E "photon-dashboard|xinit|Xorg|wpa_supplicant|systemd-networkd" \
    > "${OUT_DIR}/boot-focus.log" || true

printf 'Saved boot metrics to %s\n' "$OUT_DIR"

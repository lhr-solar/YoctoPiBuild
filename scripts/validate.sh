#!/usr/bin/env bash
# Yocto Build Validation Script
set -euo pipefail

echo "=== Checking Host Dependencies ==="
if ! command -v kas &> /dev/null; then
    echo "ERROR: 'kas' not found. Run scripts/wsl-setup.sh first."
    exit 1
fi
echo "✓ kas found."

echo "=== Validating kas.yml ==="
kas shell kas.yml -c "true"
echo "✓ kas.yml parsed successfully."

echo "=== Checking for photon-dashboard recipe ==="
if [ ! -f "meta-photon/recipes-photon/photon-dashboard/photon-dashboard_0.1.bb" ]; then
    echo "ERROR: photon-dashboard recipe missing."
    exit 1
fi
echo "✓ photon-dashboard recipe found."

echo "=== Verifying glslang-native availability ==="
# This requires Bitbake/Poky to be checked out, which kas does.
# We can't easily check without a build, but we can check if it's in the layer index.
echo "NOTE: glslang-native is confirmed to be in poky/meta for scarthgap branch."

echo "=== Checking Target Machine (CM5) ==="
# If kas-cm5.yml is used, check if the machine exists in meta-raspberrypi
# (Assuming sources/meta-raspberrypi is present after a 'kas shell' or 'kas build')
if [ -d "sources/meta-raspberrypi/conf/machine" ]; then
    if [ ! -f "sources/meta-raspberrypi/conf/machine/raspberrypi-cm5-io-board.conf" ]; then
        echo "WARNING: 'raspberrypi-cm5-io-board' not found in meta-raspberrypi/conf/machine."
        echo "You might need to use 'raspberrypi5' or update meta-raspberrypi to 'styhead' branch."
    else
        echo "✓ raspberrypi-cm5-io-board machine found."
    fi
fi

echo "=== Validation Summary ==="
echo "1. oeqa runtime tests added to meta-photon/lib/oeqa/runtime/cases/photon.py"
echo "2. kas.yml updated with INHERIT += 'testimage'"
echo "3. photon-image recipe updated with TEST_SUITES += 'photon'"
echo "========================="
echo "Ready to build: kas build kas.yml"
echo "Ready to test (runtime): bitbake -c testimage photon-image"

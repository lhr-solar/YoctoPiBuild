#!/usr/bin/env bash
set -euo pipefail

echo "=== Installing Yocto host dependencies (Ubuntu 22.04) ==="
sudo apt-get update
sudo apt-get install -y \
    gawk wget git diffstat unzip texinfo gcc build-essential \
    chrpath socat cpio python3 python3-pip python3-pexpect \
    xz-utils debianutils iputils-ping python3-git python3-jinja2 \
    libegl1 libsdl1.2-dev pylint xterm python3-subunit mesa-common-dev \
    zstd liblz4-tool file locales libacl1

sudo locale-gen en_US.UTF-8

echo "=== Installing kas ==="
pip3 install --user kas

echo "=== Done. Add ~/.local/bin to PATH if not already present ==="
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
echo "Run: source ~/.bashrc"

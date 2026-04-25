# photon-mariokart

Yocto recipe for the SMK easter-egg launched by `photon-dashboard`.

## What's in here

- `photon-mariokart_0.1.bb` — bitbake recipe. Fetches vmbatlle/super-mario-kart
  upstream, applies our patches, builds with CMake, installs under
  `/opt/super-mario-kart/`.
- `files/` — patches that turn the upstream Makefile-only repo into something
  that builds with CMake on Linux and accepts CAN inputs over UDP.

## How the dashboard finds it

`kart_launcher.cpp` in Photon defaults to `/opt/super-mario-kart/` on Linux
(override with `PHOTON_SMK_DIR`). When the user clicks both bottom corners of
the dash within ~1 s, the dashboard `fork`/`exec`s
`/opt/super-mario-kart/super_mario_kart` with the install dir as CWD so it
finds `assets/`.

## Patches

The three patches are independent:

1. `0001-add-cross-platform-cmakelists.patch` — adds `src/CMakeLists.txt`
   replacing the MinGW-only Makefile. Uses `find_package(SFML)` on Linux.
2. `0002-cxx17-portability-fixes.patch` — `_USE_MATH_DEFINES`, `<array>`
   include, `std::random_shuffle` → `std::shuffle`, drops redundant out-of-class
   constexpr definitions that MSVC / clang reject.
3. `0003-ipc-input-udp-listener.patch` — adds `src/input/ipc_input.{h,cpp}`,
   wires `Input::held()` to consult IPC state, starts the listener in `main()`.

If this gets forked under lhr-solar, the cleaner play is to push the patched
tree there and have the recipe just clone the fork — drop the three `file://`
patch entries from `SRC_URI` and point at the fork URL.

## Adding to an image

In `meta-photon/recipes-core/images/photon-image*.bb`:

```bitbake
IMAGE_INSTALL:append = " photon-mariokart"
```

Make sure `meta-photon` is in `BBLAYERS` (it already is for photon-dashboard).

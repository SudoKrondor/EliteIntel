#!/usr/bin/env bash
# Cross-builds the Windows HUD overlay on a Linux box and drops it, with the
# DLLs it needs, into distribution/overlays/ so the installer picks it up the
# same way it picks up the TTS/STT models.
#
#   ./overlay/build-windows.sh
#
# Requires docker (or podman) and network access the first time, to build the
# Fedora/mingw image. Debian and Ubuntu ship no mingw cairo/pango, which is why
# this goes through a container rather than plain apt packages.
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$PWD"
OUT="$ROOT/distribution/overlays"
IMAGE="elite-intel-overlay-mingw"

RUNNER=""
if command -v podman >/dev/null 2>&1; then
  RUNNER=podman
elif command -v docker >/dev/null 2>&1; then
  RUNNER=docker
else
  echo "error: neither podman nor docker found" >&2
  exit 1
fi

# A docker install where the user is not in the docker group fails with a
# permission error on the socket rather than anything descriptive.
if ! $RUNNER info >/dev/null 2>&1; then
  echo "error: cannot talk to the $RUNNER daemon." >&2
  echo "  try:  sudo usermod -aG docker \$USER   (then log out and back in)" >&2
  echo "  or:   sudo $RUNNER ..." >&2
  exit 1
fi

echo "==> building image ($RUNNER)"
$RUNNER build -q -t "$IMAGE" -f overlay/Dockerfile.mingw overlay/

echo "==> cross-compiling"
mkdir -p "$OUT"
# Under sudo, id -u is 0 and the build output would land root-owned in the
# working tree. Fall back to the invoking user's ids so the files stay yours.
BUILD_UID="${SUDO_UID:-$(id -u)}"
BUILD_GID="${SUDO_GID:-$(id -g)}"
$RUNNER run --rm \
  -v "$ROOT:/src:z" \
  -u "$BUILD_UID:$BUILD_GID" \
  "$IMAGE" \
  bash -c '
    set -euo pipefail
    cd /src/overlay
    PKG=/usr/bin/x86_64-w64-mingw32-pkg-config
    CC=x86_64-w64-mingw32-gcc
    DEPS="cairo pangocairo"

    # Window icon. Kept in step with the same rule in the Makefile, which is
    # what the MSYS2 and CI builds use.
    mkdir -p build
    x86_64-w64-mingw32-windres -I res res/overlay.rc -o build/overlay_res.o

    $CC -O2 -Wall -Wextra -std=c11 $($PKG --cflags $DEPS) \
        -o /src/distribution/overlays/elite-intel-overlay.exe \
        src/hud_model.c src/hud_render.c src/platform_win32.c build/overlay_res.o \
        $($PKG --libs $DEPS) -lgdi32 -luser32 -lm -mwindows

    # Copy the mingw DLLs the binary actually imports, following transitive
    # imports, so the folder is self-contained on a machine with no MSYS2.
    SYSROOT=/usr/x86_64-w64-mingw32/sys-root/mingw/bin
    copied=""
    queue=$(objdump -p /src/distribution/overlays/elite-intel-overlay.exe \
            | awk "/DLL Name:/ {print \$3}")
    while [ -n "$queue" ]; do
      next=""
      for dll in $queue; do
        case " $copied " in *" $dll "*) continue;; esac
        if [ -f "$SYSROOT/$dll" ]; then
          cp -u "$SYSROOT/$dll" /src/distribution/overlays/
          copied="$copied $dll"
          next="$next $(objdump -p "$SYSROOT/$dll" | awk "/DLL Name:/ {print \$3}")"
        fi
      done
      queue="$next"
    done
    echo "bundled DLLs:$copied"

    # Fedora ships these DLLs unstripped - libstdc++ alone is ~26 MB of debug
    # symbols. They are shipped to every user, in the installer and in every
    # auto-update ZIP, so strip them. Nothing debuggable is lost: this is a
    # third-party runtime we do not step through.
    cd /src/distribution/overlays
    x86_64-w64-mingw32-strip --strip-unneeded *.dll elite-intel-overlay.exe 2>/dev/null || true
    echo "stripped; overlay payload now $(du -sh . | cut -f1)"
  '

echo "==> done"
ls -la "$OUT"

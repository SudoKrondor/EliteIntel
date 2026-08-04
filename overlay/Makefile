# EliteIntel HUD overlay.
#
# Output goes to distribution/overlays/, which the installer packages the same
# way it packages the TTS/STT models.
#
#   make          native build - X11 on Linux, Win32 under MSYS2
#   make clean
#
# The model, the stdin protocol and all drawing are shared between platforms;
# only the window shell differs, so a change to what the overlay displays is
# made once.

OUT_DIR := ../distribution/overlays
# The SteamVR shell is in every build on both platforms: it loads openvr_api at
# runtime rather than linking it, so it adds no build dependency and no DLL a
# flat-screen commander has to have.
CORE    := src/main.c src/hud_model.c src/hud_render.c src/hud_tilt.c src/stdin_pump.c src/platform_openvr.c

ifeq ($(OS),Windows_NT)
  BIN           := elite-intel-overlay.exe
  SRC           := $(CORE) src/platform_win32.c
  DEPS          := cairo pangocairo
  PLATFORM_LIBS := -lgdi32 -luser32 -mwindows
  DEPS_HINT     := pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-cairo mingw-w64-x86_64-pango mingw-w64-x86_64-pkgconf
else
  BIN           := elite-intel-overlay
  SRC           := $(CORE) src/platform_x11.c
  # xrandr is needed to find WHICH monitor the card is on. DisplayWidth/Height
  # describe the whole virtual screen across every display, so on a multi-head
  # desktop the card's position relative to "the screen" is meaningless. Windows
  # needs no equivalent: MonitorFromWindow is in user32, already linked.
  DEPS          := x11 xrandr cairo pangocairo
  PLATFORM_LIBS := -ldl
  DEPS_HINT     := sudo apt install libx11-dev libxrandr-dev libcairo2-dev libpango1.0-dev
endif

CFLAGS  += -O2 -Wall -Wextra -std=c11 -isystem third_party/openvr $(shell pkg-config --cflags $(DEPS))
LDFLAGS += $(shell pkg-config --libs $(DEPS)) $(PLATFORM_LIBS) -lm

.PHONY: all clean check-deps

all: check-deps $(OUT_DIR)/$(BIN)

check-deps:
	@pkg-config --exists $(DEPS) || { \
		echo "missing build deps:"; \
		echo "  $(DEPS_HINT)"; \
		exit 1; }

$(OUT_DIR)/$(BIN): $(SRC) src/hud.h
	@mkdir -p $(OUT_DIR)
	$(CC) $(CFLAGS) -o $@ $(SRC) $(LDFLAGS)
	@echo "built $@"

clean:
	rm -f $(OUT_DIR)/elite-intel-overlay $(OUT_DIR)/elite-intel-overlay.exe

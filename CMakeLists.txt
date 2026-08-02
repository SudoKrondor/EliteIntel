cmake_minimum_required(VERSION 4.3)
project(EliteIntel C)

set(CMAKE_C_STANDARD 11)

include_directories(overlay/src)
# SYSTEM so the generated OpenVR header's own unused-variable warnings stay out
# of ours; see overlay/third_party/openvr/README.md.
include_directories(SYSTEM overlay/third_party/openvr)

add_executable(EliteIntel
        overlay/src/hud.h
        overlay/src/main.c
        overlay/src/hud_model.c
        overlay/src/hud_render.c
        overlay/src/stdin_pump.c
        overlay/src/platform_openvr.c
        overlay/src/platform_win32.c
        overlay/src/platform_x11.c)

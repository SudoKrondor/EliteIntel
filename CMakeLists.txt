cmake_minimum_required(VERSION 4.3)
project(EliteIntel C)

set(CMAKE_C_STANDARD 11)

include_directories(overlay/src)

add_executable(EliteIntel
        overlay/src/hud.h
        overlay/src/hud_model.c
        overlay/src/hud_render.c
        overlay/src/platform_win32.c
        overlay/src/platform_x11.c)

// Win32 shell for the HUD overlay.
//
// Same core as the X11 shell - the model, protocol and every drawn pixel come
// from hud.h/hud_render.c. Only the window and the event loop differ.
//
// Per-pixel alpha uses UpdateLayeredWindow with a 32-bit premultiplied bitmap,
// which is the real translucency path (how Rainmeter/RTSS/Discord draw floating
// text). SetLayeredWindowAttributes is deliberately NOT used: it gives uniform
// whole-window opacity, which dims the text along with the background.
//
// Cairo's CAIRO_FORMAT_ARGB32 is already premultiplied, which is exactly the
// format UpdateLayeredWindow wants with AC_SRC_ALPHA, so the buffer is handed
// over without conversion.

#include "hud.h"

#include <windows.h>
#include <stdio.h>
#include <string.h>

#define TIMER_TICK 1

static HWND    g_win;
static HDC     g_mem_dc;
static HBITMAP g_bitmap;
static void   *g_bits;          // DIB pixels, written by cairo
static cairo_surface_t *g_surface;
static int     g_width  = 760;
static int     g_height = 200;
static int     g_x, g_y;
static int     g_dragging;
static POINT   g_drag_origin;
static int     g_quit;

/// (Re)creates the DIB the size of the window and points cairo at its pixels,
/// so drawing lands straight in the bitmap UpdateLayeredWindow presents.
static void resize_surface(int width, int height) {
    if (g_surface) { cairo_surface_destroy(g_surface); g_surface = NULL; }
    if (g_bitmap)  { DeleteObject(g_bitmap); g_bitmap = NULL; }

    BITMAPINFO bi;
    ZeroMemory(&bi, sizeof(bi));
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biWidth = width;
    bi.bmiHeader.biHeight = -height;      // top-down, matching cairo's row order
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biBitCount = 32;
    bi.bmiHeader.biCompression = BI_RGB;

    g_bitmap = CreateDIBSection(g_mem_dc, &bi, DIB_RGB_COLORS, &g_bits, NULL, 0);
    SelectObject(g_mem_dc, g_bitmap);
    g_surface = cairo_image_surface_create_for_data(
            (unsigned char *) g_bits, CAIRO_FORMAT_ARGB32, width, height, width * 4);
    g_width = width;
    g_height = height;
}

static void present(void) {
    cairo_t *measure = cairo_create(g_surface);
    int needed = hud_render(measure, g_width, 0);
    cairo_destroy(measure);
    if (needed > 0 && needed != g_height) resize_surface(g_width, needed);

    cairo_t *cr = cairo_create(g_surface);
    hud_paint_background(cr);
    hud_render(cr, g_width, 1);
    cairo_destroy(cr);
    cairo_surface_flush(g_surface);

    POINT src = {0, 0};
    POINT dst = {g_x, g_y};
    SIZE size = {g_width, g_height};
    BLENDFUNCTION blend;
    blend.BlendOp = AC_SRC_OVER;
    blend.BlendFlags = 0;
    blend.SourceConstantAlpha = 255;      // per-pixel alpha only, never uniform
    blend.AlphaFormat = AC_SRC_ALPHA;

    HDC screen = GetDC(NULL);
    UpdateLayeredWindow(g_win, screen, &dst, &size, g_mem_dc, &src, 0, &blend, ULW_ALPHA);
    ReleaseDC(NULL, screen);
}

static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
    switch (msg) {
        case WM_LBUTTONDOWN:
            g_dragging = 1;
            GetCursorPos(&g_drag_origin);
            g_drag_origin.x -= g_x;
            g_drag_origin.y -= g_y;
            SetCapture(hwnd);
            return 0;
        case WM_MOUSEMOVE:
            if (g_dragging) {
                POINT p;
                GetCursorPos(&p);
                g_x = p.x - g_drag_origin.x;
                g_y = p.y - g_drag_origin.y;
                present();
            }
            return 0;
        case WM_LBUTTONUP:
            if (g_dragging) hud_report_position(g_x, g_y);
            g_dragging = 0;
            ReleaseCapture();
            return 0;
        case WM_TIMER:
            if (hud_tick_typewriter()) present();
            return 0;
        case WM_DESTROY:
            g_quit = 1;
            PostQuitMessage(0);
            return 0;
        default:
            return DefWindowProc(hwnd, msg, wp, lp);
    }
}

/// Drains whatever is on stdin without blocking the message loop. Returns 1 if
/// the model changed, and sets *eof when the parent closed the pipe.
static int pump_stdin(int *eof) {
    char chunk[4096];
    int dirty = 0;

    HANDLE in = GetStdHandle(STD_INPUT_HANDLE);
    for (;;) {
        DWORD avail = 0;
        if (!PeekNamedPipe(in, NULL, 0, NULL, &avail, NULL)) { *eof = 1; break; }
        if (avail == 0) break;
        if (avail > sizeof(chunk)) avail = sizeof(chunk);

        DWORD got = 0;
        if (!ReadFile(in, chunk, avail, &got, NULL) || got == 0) { *eof = 1; break; }
        if (hud_feed(chunk, (int) got, &g_quit)) dirty = 1;
    }
    return dirty;
}

int main(void) {
    HINSTANCE inst = GetModuleHandle(NULL);

    WNDCLASSEX wc;
    ZeroMemory(&wc, sizeof(wc));
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = wnd_proc;
    wc.hInstance = inst;
    wc.lpszClassName = "EliteIntelHudOverlay";
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    RegisterClassEx(&wc);

    g_x = (GetSystemMetrics(SM_CXSCREEN) - model.width) / 2;
    g_y = (int) (GetSystemMetrics(SM_CYSCREEN) * 0.04);

    // WS_EX_NOACTIVATE is what keeps every keystroke going to the game: the
    // window can be clicked and dragged but never takes keyboard focus.
    g_win = CreateWindowEx(
            WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE,
            wc.lpszClassName, "EliteIntel HUD Overlay", WS_POPUP,
            g_x, g_y, model.width, g_height, NULL, NULL, inst, NULL);
    if (!g_win) { fprintf(stderr, "overlay: CreateWindowEx failed\n"); return 1; }

    HDC screen = GetDC(NULL);
    g_mem_dc = CreateCompatibleDC(screen);
    ReleaseDC(NULL, screen);
    resize_surface(model.width, g_height);

    ShowWindow(g_win, SW_SHOWNOACTIVATE);
    SetTimer(g_win, TIMER_TICK, TYPEWRITER_MS, NULL);

    MSG msg;
    while (!g_quit) {
        while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) { g_quit = 1; break; }
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        if (g_quit) break;

        int eof = 0;
        int dirty = pump_stdin(&eof);

        if (model.want_x >= 0 || model.want_y >= 0) {
            if (model.want_x >= 0) g_x = model.want_x;
            if (model.want_y >= 0) g_y = model.want_y;
            model.want_x = model.want_y = -1;
            dirty = 1;
        }
        if (model.width != g_width && model.width > 0) {
            resize_surface(model.width, g_height);
            dirty = 1;
        }
        if (dirty) present();
        if (eof) break;                  // parent closed the pipe or died

        // Idle briefly rather than spinning; the timer drives the typewriter.
        MsgWaitForMultipleObjects(0, NULL, FALSE, 10, QS_ALLINPUT);
    }

    KillTimer(g_win, TIMER_TICK);
    if (g_surface) cairo_surface_destroy(g_surface);
    if (g_bitmap) DeleteObject(g_bitmap);
    if (g_mem_dc) DeleteDC(g_mem_dc);
    DestroyWindow(g_win);
    return 0;
}

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

/// The rectangle the card is actually sitting on, in desktop coordinates.
///
/// WHY not SM_CXSCREEN: that is the PRIMARY monitor's size, while g_x and g_y are
/// virtual-desktop coordinates covering every monitor. On a two-screen setup with
/// the game on the second one, g_x can exceed the primary width outright, so the
/// lean saturates and stops responding to being dragged - which is exactly the
/// symptom this feature is meant to have. Elite commanders commonly run more than
/// one display, so the monitor under the window is the only sensible reference.
///
/// Falls back to the primary monitor if the lookup fails, which is the old
/// behaviour and correct for the single-monitor case it is really describing.
static void screen_rect(int *ox, int *oy, int *width, int *height) {
    if (g_win) {
        HMONITOR mon = MonitorFromWindow(g_win, MONITOR_DEFAULTTONEAREST);
        MONITORINFO info;
        info.cbSize = sizeof(info);
        if (mon && GetMonitorInfo(mon, &info)) {
            *ox = info.rcMonitor.left;
            *oy = info.rcMonitor.top;
            *width = info.rcMonitor.right - info.rcMonitor.left;
            *height = info.rcMonitor.bottom - info.rcMonitor.top;
            return;
        }
    }
    *ox = 0;
    *oy = 0;
    *width = GetSystemMetrics(SM_CXSCREEN);
    *height = GetSystemMetrics(SM_CYSCREEN);
}

static void present(void) {
    // Measured at the CARD's width, which is not the window's once the card
    // leans: the shear makes the window taller, never wider.
    int card_w = hud_card_width();

    cairo_t *measure = cairo_create(g_surface);
    int card_h = hud_render(measure, card_w, 0);
    cairo_destroy(measure);
    if (card_h < 1) card_h = 1;

    // Measured first, because the lean now depends on where the card's centre
    // sits vertically and that is not known until its height is. Offsets are
    // subtracted so the card's position is relative to ITS monitor, not to the
    // whole desktop.
    int sx, sy, sw, sh;
    screen_rect(&sx, &sy, &sw, &sh);
    // Flat when the window is being captured: the lean is a perspective trick
    // that reads as depth only in the plane of the monitor it was measured
    // against. Pinned in a headset it is just a skewed card, and it costs width.
    double slope = model.capture
                   ? 0.0
                   : hud_tilt_slope(g_x + card_w / 2 - sx, g_y + card_h / 2 - sy, sw, sh);

    int win_h = hud_tilt_height(slope, card_w, card_h);
    if (card_w != g_width || win_h != g_height) resize_surface(card_w, win_h);

    cairo_t *cr = cairo_create(g_surface);
    // Cleared, not painted over: the shear leaves a wedge at the top and another
    // at the bottom that must come out fully transparent, and UpdateLayeredWindow
    // takes them at their word.
    cairo_set_operator(cr, CAIRO_OPERATOR_CLEAR);
    cairo_paint(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_OVER);

    hud_tilt_apply(cr, slope, card_w);
    hud_paint_panel(cr, card_w, card_h);
    hud_render(cr, card_w, 1);
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

int hud_run_desktop(int argc, char **argv) {
    (void) argc;                          // --capture is read in main, into model
    (void) argv;
    HINSTANCE inst = GetModuleHandle(NULL);

    WNDCLASSEX wc;
    ZeroMemory(&wc, sizeof(wc));
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = wnd_proc;
    wc.hInstance = inst;
    wc.lpszClassName = "EliteIntelHudOverlay";
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    RegisterClassEx(&wc);

    // The window does not exist yet, so this is the primary monitor by
    // definition. Once it does, present() re-resolves against whichever monitor
    // the card has been dragged onto.
    g_x = (GetSystemMetrics(SM_CXSCREEN) - hud_card_width()) / 2;
    g_y = (int) (GetSystemMetrics(SM_CYSCREEN) * 0.04);

    // WS_EX_NOACTIVATE is what keeps every keystroke going to the game: the
    // window can be clicked and dragged but never takes keyboard focus. It is
    // the one style capture mode keeps, because a HUD that steals focus from
    // the game is wrong however it is being viewed.
    //
    // WS_EX_TOOLWINDOW is the style that has to go in capture mode, and it is
    // the reason this mode exists at all: a tool window is deliberately hidden
    // from the taskbar, from alt-tab, and from the window pickers Desktop+, OVR
    // Toolkit and Virtual Desktop put in front of the commander. The overlay
    // they cannot find is the overlay they cannot pin. WS_EX_TOPMOST goes with
    // it - a window that is about to become a texture in a headset has no
    // business sitting above everything on the desktop it left behind.
    //
    // WS_EX_APPWINDOW is what makes the pickers actually list it, and dropping
    // the tool window style is not enough on its own. WS_EX_NOACTIVATE also
    // hides a window from the taskbar and from alt-tab, and the pickers read
    // that as "not a real window": Desktop+ rejects a window outright when it
    // has WS_EX_NOACTIVATE without WS_EX_APPWINDOW. WS_EX_APPWINDOW is the
    // documented way to say the window is a real one anyway, so it buys back
    // the taskbar entry and the picker entry without giving up the focus
    // guarantee above. OBS never applied that rule, which is why it could find
    // and capture this window while Desktop+ could not even list it.
    DWORD ex_style = model.capture
                     ? (WS_EX_LAYERED | WS_EX_NOACTIVATE | WS_EX_APPWINDOW)
                     : (WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE);
    g_win = CreateWindowEx(
            ex_style, wc.lpszClassName,
            model.capture ? "EliteIntel HUD (VR capture)" : "EliteIntel HUD Overlay",
            WS_POPUP, g_x, g_y, hud_card_width(), g_height, NULL, NULL, inst, NULL);
    if (!g_win) { fprintf(stderr, "overlay: CreateWindowEx failed\n"); return 1; }

    HDC screen = GetDC(NULL);
    g_mem_dc = CreateCompatibleDC(screen);
    ReleaseDC(NULL, screen);
    resize_surface(hud_card_width(), g_height);

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
        // Only flagged, never resized here: present() sizes the surface itself
        // now, because the height it needs depends on the lean as well as on the
        // content and only it knows both.
        if (hud_card_width() != g_width) dirty = 1;
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

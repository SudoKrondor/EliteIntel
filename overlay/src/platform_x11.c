// X11/Xwayland shell for the HUD overlay.
//
// Owns only the window and the event loop; every pixel comes from hud_render().
// A Win32 shell is the same shape: create a translucent always-on-top window,
// let the user drag it, present a finished ARGB buffer.
//
// The window is override-redirect, which means the compositor leaves it
// unmanaged and stacked above managed windows, and it never takes keyboard
// focus - so every keystroke still reaches the game. It does receive pointer
// events, which is what makes dragging work.

#include "hud.h"

#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <X11/Xutil.h>
#include <cairo/cairo-xlib.h>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>

static long now_ms(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000L + tv.tv_usec / 1000L;
}

int main(int argc, char **argv) {
    int managed = 0;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--managed")) managed = 1;
    }

    Display *dpy = XOpenDisplay(NULL);
    if (!dpy) { fprintf(stderr, "overlay: cannot open display\n"); return 1; }
    int screen = DefaultScreen(dpy);
    Window root = RootWindow(dpy, screen);

    // A 32-bit visual is the whole ballgame: without it there is no alpha
    // channel and the best available would be uniform window opacity.
    XVisualInfo vinfo;
    if (!XMatchVisualInfo(dpy, screen, 32, TrueColor, &vinfo)) {
        fprintf(stderr, "overlay: no 32-bit ARGB visual; per-pixel alpha unavailable\n");
        return 2;
    }

    int width = model.width;
    int height = 200;
    int x = (DisplayWidth(dpy, screen) - width) / 2;
    int y = (int) (DisplayHeight(dpy, screen) * 0.04);

    XSetWindowAttributes attrs;
    attrs.colormap = XCreateColormap(dpy, root, vinfo.visual, AllocNone);
    attrs.background_pixel = 0;
    attrs.border_pixel = 0;
    attrs.override_redirect = managed ? False : True;
    attrs.event_mask = ExposureMask | ButtonPressMask | ButtonReleaseMask | PointerMotionMask;

    Window win = XCreateWindow(dpy, root, x, y, width, height, 0, 32,
                               InputOutput, vinfo.visual,
                               CWColormap | CWBackPixel | CWBorderPixel |
                               CWOverrideRedirect | CWEventMask, &attrs);
    XStoreName(dpy, win, "EliteIntel HUD Overlay");

    Atom wtype = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", False);
    Atom notif = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_NOTIFICATION", False);
    XChangeProperty(dpy, win, wtype, XA_ATOM, 32, PropModeReplace, (unsigned char *) &notif, 1);
    Atom wstate = XInternAtom(dpy, "_NET_WM_STATE", False);
    Atom above = XInternAtom(dpy, "_NET_WM_STATE_ABOVE", False);
    XChangeProperty(dpy, win, wstate, XA_ATOM, 32, PropModeReplace, (unsigned char *) &above, 1);

    XMapWindow(dpy, win);
    XRaiseWindow(dpy, win);

    cairo_surface_t *target = cairo_xlib_surface_create(dpy, win, vinfo.visual, width, height);
    cairo_surface_t *buffer = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);

    int xfd = ConnectionNumber(dpy);
    int quit = 0, dirty = 1, dragging = 0, drag_x = 0, drag_y = 0;
    long last_tick = now_ms();

    while (!quit) {
        while (XPending(dpy)) {
            XEvent ev;
            XNextEvent(dpy, &ev);
            if (ev.type == Expose) dirty = 1;
            else if (ev.type == ButtonPress && ev.xbutton.button == Button1) {
                dragging = 1;
                drag_x = ev.xbutton.x_root - x;
                drag_y = ev.xbutton.y_root - y;
            } else if (ev.type == ButtonRelease && ev.xbutton.button == Button1) {
                if (dragging) hud_report_position(x, y);
                dragging = 0;
            } else if (ev.type == MotionNotify && dragging) {
                x = ev.xmotion.x_root - drag_x;
                y = ev.xmotion.y_root - drag_y;
                XMoveWindow(dpy, win, x, y);
            }
        }

        long t = now_ms();
        if (t - last_tick >= TYPEWRITER_MS) {
            last_tick = t;
            if (hud_tick_typewriter()) dirty = 1;
        }

        if (model.want_x >= 0 || model.want_y >= 0) {
            if (model.want_x >= 0) x = model.want_x;
            if (model.want_y >= 0) y = model.want_y;
            model.want_x = model.want_y = -1;
            XMoveWindow(dpy, win, x, y);
        }
        if (model.width != width && model.width > 0) {
            width = model.width;
            XResizeWindow(dpy, win, width, height);
            cairo_xlib_surface_set_size(target, width, height);
            cairo_surface_destroy(buffer);
            buffer = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
            dirty = 1;
        }

        if (dirty) {
            dirty = 0;
            cairo_t *measure = cairo_create(buffer);
            int needed = hud_render(measure, width, 0);
            cairo_destroy(measure);

            if (needed != height && needed > 0) {
                height = needed;
                XResizeWindow(dpy, win, width, height);
                cairo_xlib_surface_set_size(target, width, height);
                cairo_surface_destroy(buffer);
                buffer = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
            }

            cairo_t *cr = cairo_create(buffer);
            hud_paint_background(cr);
            hud_render(cr, width, 1);
            cairo_destroy(cr);

            // One blit of a finished frame. The window is never cleared, so
            // there is no intermediate state for the compositor to present -
            // this is what AWT does not let you control, and why the Swing
            // overlay strobed on every typewriter tick.
            cairo_t *out = cairo_create(target);
            cairo_set_operator(out, CAIRO_OPERATOR_SOURCE);
            cairo_set_source_surface(out, buffer, 0, 0);
            cairo_paint(out);
            cairo_destroy(out);
            cairo_surface_flush(target);
            XFlush(dpy);
        }

        fd_set fds;
        FD_ZERO(&fds);
        FD_SET(xfd, &fds);
        FD_SET(STDIN_FILENO, &fds);
        int maxfd = xfd > STDIN_FILENO ? xfd : STDIN_FILENO;
        struct timeval tv = {0, TYPEWRITER_MS * 1000};
        if (select(maxfd + 1, &fds, NULL, NULL, &tv) < 0 && errno != EINTR) break;

        if (FD_ISSET(STDIN_FILENO, &fds)) {
            char chunk[4096];
            int got = (int) read(STDIN_FILENO, chunk, sizeof(chunk));
            if (got <= 0) break;         // parent closed the pipe or died
            if (hud_feed(chunk, got, &quit)) dirty = 1;
        }
    }

    cairo_surface_destroy(buffer);
    cairo_surface_destroy(target);
    XDestroyWindow(dpy, win);
    XCloseDisplay(dpy);
    return 0;
}

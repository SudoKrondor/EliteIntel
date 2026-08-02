// Non-blocking stdin drain, for shells that have no event loop to hang one on.
//
// The desktop shells each pump stdin inside their own loop - Win32 around
// PeekNamedPipe, X11 around select on the display fd - because they have window
// messages to interleave with. The VR shell has neither, and needs the same pump
// on both platforms, so it lives here rather than being written twice.
//
// Draining matters even when nothing is being drawn: the app writes protocol
// lines continuously, and a child that stops reading fills the pipe buffer and
// blocks the app's own writer thread mid-poll.

#ifndef _WIN32
#define _POSIX_C_SOURCE 200809L
#endif

#include "hud.h"

#ifdef _WIN32
#include <windows.h>
#else
#include <errno.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>
#endif

int hud_pump_stdin(int timeout_ms, int *eof, int *quit) {
    char chunk[4096];
    int dirty = 0;

#ifdef _WIN32
    HANDLE in = GetStdHandle(STD_INPUT_HANDLE);

    // PeekNamedPipe cannot wait, so the timeout is spent sleeping in slices
    // rather than blocking on the handle: the parent always hands us a pipe, and
    // waiting on a pipe handle signals on the wrong thing.
    const int slice_ms = 5;
    for (int waited = 0; ; waited += slice_ms) {
        for (;;) {
            DWORD available = 0;
            if (!PeekNamedPipe(in, NULL, 0, NULL, &available, NULL)) { *eof = 1; return dirty; }
            if (available == 0) break;
            if (available > sizeof(chunk)) available = sizeof(chunk);

            DWORD got = 0;
            if (!ReadFile(in, chunk, available, &got, NULL) || got == 0) { *eof = 1; return dirty; }
            if (hud_feed(chunk, (int) got, quit)) dirty = 1;
        }
        if (dirty || *quit || waited >= timeout_ms) break;
        Sleep(slice_ms);
    }
#else
    fd_set fds;
    FD_ZERO(&fds);
    FD_SET(STDIN_FILENO, &fds);
    struct timeval tv = {timeout_ms / 1000, (timeout_ms % 1000) * 1000};

    int ready = select(STDIN_FILENO + 1, &fds, NULL, NULL, &tv);
    if (ready < 0) {
        // EINTR is a signal, not a closed pipe: report nothing and let the
        // caller come back round.
        if (errno != EINTR) *eof = 1;
        return dirty;
    }
    if (ready > 0 && FD_ISSET(STDIN_FILENO, &fds)) {
        int got = (int) read(STDIN_FILENO, chunk, sizeof(chunk));
        if (got <= 0) { *eof = 1; return dirty; }
        if (hud_feed(chunk, got, quit)) dirty = 1;
    }
#endif

    return dirty;
}

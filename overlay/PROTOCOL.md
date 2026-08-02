# Overlay wire protocol (v1)

The EliteIntel app spawns the overlay binary as a child process and writes commands to its **stdin
**, one per line. The overlay writes back on **stdout
** only the few things the app cannot derive for itself (see [Reverse channel](#reverse-channel)); everything else it has to say goes to stderr as diagnostics.

Why stdin rather than a socket: no port to pick, nothing listening that a web page or another process can reach, and the overlay's lifetime is tied to the app's by the OS. The app is the only producer and the overlay the only consumer, so a discovery mechanism would be pure overhead.

Lines are **tab-separated**,
`\n`-terminated, UTF-8. Both ends ship from this repo in the same release, so the format is deliberately trivial to parse in C rather than self-describing. The
`V` handshake guards against a stale binary left behind by an older install.

## Commands

```
V <version>
```

Protocol version. Sent once, first. The overlay exits non-zero if it does not recognise the version, so the app can detect a stale binary.

```
CFG <key>=<value> [<key>=<value> ...]
```

Configuration. Any subset, any order, at any time.

| key     | meaning                                    |
|---------|--------------------------------------------|
| `alpha` | background alpha, `0.0`-`1.0`. Text is unaffected. |
| `scale` | font scale, `1.0` is calibrated for 1440p |
| `x`,`y` | window position in screen pixels          |
| `width` | window width in pixels; height follows content |

```
OBJ <title> <subtitle>
ROW <label> <value>
BAR <label> <current> <max> <state>
END
```

Replaces the objective card atomically. `OBJ` opens it, `ROW`/`BAR` add lines in order,
`END` commits and triggers a resize+repaint. An empty `subtitle` is allowed. `state` is one of `normal`, `good`, `warn`,
`critical` and drives the value colour only, never the layout.

```
CLR
```

Clears the objective card. The window shrinks to just the conversation.

```
SAY <speaker> <ai> <text>
```

Appends a conversation line. `ai` is `0` (commander) or
`1` (ship AI) and selects the colour. The overlay owns the typewriter animation - the app sends whole lines and never streams characters, so the animation never depends on pipe timing.

```
QUIT
```

Exits cleanly.

## Command line

```
elite-intel-overlay [--vr[=on|auto|off]] [--managed]
```

| flag         | meaning                                                                     |
|--------------|-----------------------------------------------------------------------------|
| *(none)*     | Desktop overlay. Identical to every version before VR support existed.      |
| `--vr=off`   | Same as passing nothing. SteamVR is never even probed.                      |
| `--vr=auto`  | SteamVR overlay **only when a headset is actually connected**, desktop otherwise. |
| `--vr=on`    | SteamVR overlay whenever the runtime is installed, headset awake or not. `--vr` is a synonym. |
| `--vr=only`  | SteamVR overlay or **nothing** — never a window. Waits and retries for SteamVR for as long as the app is running; exits `3` only when SteamVR is not installed at all. |
| `--managed`  | X11 only: let the window manager manage the window, for debugging.          |

Unknown flags are ignored, for the same reason unknown commands are.

**Every VR failure falls back to the desktop overlay except `--vr=only`
** — no runtime, no headset, a SteamVR that will not start. A commander who asks for VR and cannot have it still gets an overlay, and the reason arrives on the reverse channel.

`--vr=only` is the exception because of the "both at once" setting, where the app runs **two children
**: a desktop overlay and a VR one, fed identical lines. If the VR child fell back there, its window would land exactly on top of the desktop child's — the commander would drag one and watch the other stay put. So it exits instead, and the desktop child is the whole overlay.

## Reverse channel

Lines the overlay writes to **stdout
**, same tab-separated format. The app ignores anything it does not recognise, so an older binary that reports less and a newer one that reports more both work.

```
POS <x> <y>
```

Where the window ended up after a drag. The only state the overlay owns and the app cannot derive, since the commander moves it with the mouse. Never sent in VR mode, where there is no window to drag.

```
MODE <desktop|vr|waiting|none> [<reason>]
```

Which shell actually came up, and why it is not what the app asked for, so a settings UI can explain itself instead of silently showing the wrong thing.

| value | meaning |
|---|---|
| `desktop` | A window. With a `reason` when VR was asked for and could not be had. |
| `vr` | Drawing in the headset. Sent on every attach, so a re-attach after SteamVR restarts is visible. |
| `waiting` | `--vr=only` could not attach and is retrying — e.g. `SteamVR is not running`. A later `vr` line follows if it succeeds. |
| `none` | A `--vr=only` child giving up on its way out. |

Reasons are plain text meant for a commander: `no headset detected`, `SteamVR runtime not installed`,
`SteamVR is not running`, `SteamVR closed`, `SteamVR would not start an overlay`.

## Notes

- Tabs inside `text`/`title`/
  `value` must be replaced with spaces by the producer; the overlay splits on tabs and does not unescape.
- Unknown commands are ignored, so the app may send newer verbs to an older binary without breaking it.
- A line must stay under
  `MAX_LINE` (8192 bytes). The producer bounds spoken text well below that, since the renderer keeps only
  `MAX_TEXT` (1024 bytes)
  per line anyway. A longer line is **dropped whole
  ** and parsing resumes at the next newline: reaching that limit means the producer has a bug, and losing one line beats either exiting or feeding the parser the tail of a command.

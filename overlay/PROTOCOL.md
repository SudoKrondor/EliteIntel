# Overlay wire protocol (v1)

The EliteIntel app spawns the overlay binary as a child process and writes commands to its **stdin
**, one per line. The overlay never writes back except diagnostics on stderr.

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

## Notes

- Tabs inside `text`/`title`/
  `value` must be replaced with spaces by the producer; the overlay splits on tabs and does not unescape.
- Unknown commands are ignored, so the app may send newer verbs to an older binary without breaking it.
- A line must stay under
  `MAX_LINE` (8192 bytes). The producer bounds spoken text well below that, since the renderer keeps only
  `MAX_TEXT` (1024 bytes)
  per line anyway. A longer line is **dropped whole
  ** and parsing resumes at the next newline: reaching that limit means the producer has a bug, and losing one line beats either exiting or feeding the parser the tail of a command.

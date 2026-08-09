# Diagnostics `input.txt` — reference

The file-based diagnostics mode lets you "speak" phrases to the application and feed it game events from a text file, with no microphone and no running game. It is a dev tool for checking command/query routing and the companion's reaction to events.

Sources of truth (update the examples here when the code changes):

- `app/src/main/java/elite/intel/diagnostics/DiagnosticsInputTailer.java` — the line parser.
- `app/src/main/java/elite/intel/diagnostics/DiagnosticsContext.java` — the `@status`/`@visible` contexts.
- `app/src/main/java/elite/intel/diagnostics/DiagnosticsMode.java` — the mode gate and `language.txt`.
- `.claude/skills/elite-intel-diagnostic-run/SKILL.md` — the automated routing run.

## File locations

Everything lives in `%LOCALAPPDATA%\elite-intel\diagnostics\`
(on a typical machine: `C:\Users\<user>\AppData\Local\elite-intel\diagnostics\`):

| File | Role |
|---|---|
| `input.txt` | **The mode gate** plus the input channel. The file's mere presence at startup switches diagnostics on. The application only **reads** it and never creates it — its lifecycle is yours. |
| `language.txt` | The boot language (code: `RU`, `EN`, …). Read at startup, before the companion/reducer are assembled. This is **data**, not a gate. |
| `session.log` | A mirror of the SYSTEM LOG plus the `DIAG` turn markers. |

## Enabling the mode (short version)

1. Create the directory and an **empty** `input.txt` (its presence is the gate) — **before** launching.
2. Write the language code into `language.txt` (e.g. `EN`).
3. Clear `session.log`.
4. Launch: `.\gradlew :app:run` (the window may take up to about a minute to appear).
5. Wait for `DIAG ready` in the log — the only readiness signal.
6. Append lines to `input.txt` one at a time.
7. When finished, **delete `input.txt`**, otherwise the next ordinary launch will go into diagnostics again.

Write the files as **UTF-8 without
BOM**. `Set-Content -Encoding utf8` in PowerShell 5.1 adds a BOM and corrupts the first line; for the initial writes use
`[System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))`. Lines can be appended with `Add-Content -Encoding utf8` (the application strips a leading BOM itself). This matters most for non-ASCII phrases, for example a Russian or Ukrainian run.

## Kinds of line

The application reads only **newly appended** lines, one at a time.

| Line | What it does |
|---|---|
| `plain text` | A spoken phrase → routed like microphone input. Opens a companion turn. |
| `@visible <actionId>` | Puts the game into the first context where the **command/query** `<actionId>` is visible to the router (`isVisibleForLLM`). Commands and queries only. Immediate, no turn. |
| `@status <context>` | Manual context: `main_ship`, `supercruise`, `docked`, `landed`, `srv`, `on_foot`. Sets the `Status` flags. Immediate, no turn. |
| `@fighter on` / `@fighter off` | The "fighter deployed" flag (`on`/`true` = deployed). |
| `@lang <CODE>` | Changes the command language at runtime. **Not** for setting the run's language (the reducer fixes the language when it is created — use `language.txt`). |
| `{... "event": ... }` | A JSON line with an `"event"` field → injects a journal game event. |
| `# ...` | A comment, ignored. |
| empty line | Ignored. |

### `@` directives

They apply immediately and **create no companion turn**. Each writes its own line to the log:

- `@visible <id>` → `DIAG visible=<id> state=<ctx>`.
  `state=unknown-action` means a wrong id; `main_ship(fallback)` means the action is visible nowhere.
- `@status <ctx>` → `DIAG status=<ctx>` or `DIAG status unknown=<ctx>`.
- `@fighter on|off` → `DIAG fighter=true|false`.
- `@lang <CODE>` → `DIAG lang=<CODE>` or `DIAG lang unknown=<CODE>`.

The `@status` contexts (flags mirroring `StatusFlags`):
`main_ship`, `supercruise`, `docked`, `landed`, `srv`, `on_foot`.

### Events (JSON) — the important rules

- **One line.** The parser accepts an event only if the line starts with `{`, ends with `}`
  and contains `"event"`. It must contain no line breaks.
- **Do not supply
  a `timestamp`.** `isReplay()` means "timestamp earlier than application start" → the event is discarded (`DIAG event skipped=<type>`). With the field absent, the tailer stamps a fresh `Instant.now()`
  and the event goes through.
- The event is published on the bus exactly as it would be from `JournalParser`, **with no companion
  turn**: the log gets `DIAG event=<type>`, but there will be no `DIAG turn-done` for an event. The reaction (the spoken line) is
  **asynchronous**, on a virtual thread, and appears a moment later. Do not append the next line immediately.
- **Subscribers may have their own state
  gates.** An event is "always visible" and reaches the subscriber unconditionally, but the subscriber itself may decide not to react outside the state it needs. For example, `SAASignalsFoundSubscriber.announce()` speaks the signals only when
  `isInMainShip() && !isLanded() && !isDocked()`. So put `@status supercruise` (or `main_ship`) before such an event — otherwise there will be no reaction even though the event was accepted.

### Phrases

An ordinary line is a spoken phrase. It opens a full turn: the log gets `DIAG input="<phrase>"`, then `DIAG dispatch tool=<id>` (what was recognized), then `DIAG turn-done` once the speech finishes. The language is already set through `language.txt`; `@lang` is not needed for that.

## Markers in `session.log`

`<UTC-timestamp> <marker>`:

- `DIAG ready` — all services are up and the LLM endpoint is reachable. The **only** readiness signal.
- `DIAG log opened` — a fresh instance reopened the (cleared) log.
- `DIAG tailer watching input` — the tailer is running and reading `input.txt`.
- `DIAG input="<phrase>"` — a phrase turn has opened.
- `DIAG dispatch tool=<id>` — the action the companion recognized in this turn.
- `DIAG turn-done` — the phrase turn is finished (after `dispatch` and `speaking=false`).
- `DIAG speaking=true|false` — the TTS speech boundaries.
- `DIAG event=<type>` / `DIAG event skipped=<type>` — event accepted / discarded (replay/expired).
- `DIAG boot-language=<CODE>` — the language applied from `language.txt` at startup.
- `DIAG visible=<id> state=<ctx>` / `DIAG status=<ctx>` / `DIAG fighter=<bool>` / `DIAG lang=<CODE>` — a directive was applied.
- `LOG` / `DBG` / `AI` / `USER` — the mirrored SYSTEM LOG.

## Examples

Command routing (context through `@visible`, then the phrase):
```
@visible enter_super_cruise
Take us into supercruise
```

Manual context:
```
@status supercruise
Cut our speed by half
```

Injecting an event (one line, no `timestamp`) with the ship state it needs:
```
@status supercruise
{"event":"SAASignalsFound","BodyName":"Phylurn IB-O b22-0 1 A Ring","SystemAddress":724374595777,"BodyID":12,"Signals":[{"Type":"Alexandrite","Count":1},{"Type":"Grandidierite","Count":5},{"Type":"LowTemperatureDiamond","Type_Localised":"Low Temp. Diamonds","Count":4},{"Type":"Opal","Type_Localised":"Void Opal","Count":2},{"Type":"Tritium","Count":4},{"Type":"Bromellite","Count":2}],"Genuses":[]}
```

Comments and empty lines are allowed:
```
# scenario: ring scan
@status supercruise
{"event":"SAASignalsFound","BodyName":"Test 1 A Ring","SystemAddress":1,"BodyID":2,"Signals":[{"Type":"Alexandrite","Count":3}],"Genuses":[]}
```

Appending a line from PowerShell (the JSON contains only double quotes, so wrap it in single ones):
```powershell
Add-Content -Path "$env:LOCALAPPDATA\elite-intel\diagnostics\input.txt" -Encoding utf8 `
  -Value '{"event":"SAASignalsFound","BodyName":"Test 1 A Ring","SystemAddress":1,"BodyID":2,"Signals":[{"Type":"Alexandrite","Count":3}],"Genuses":[]}'
```

## Common causes of "no reaction"

- **A single-line event, but with a `timestamp` in the past** → `DIAG event skipped`. Remove the `timestamp`.
- **An event with line breaks** → not recognized as JSON. Collapse it onto one line.
- **The subscriber gates on `Status`** (e.g. `SAASignalsFound` requires being in flight in the ship) → put
  `@status supercruise`/`main_ship` before the event.
- **You are reading an old log** → clear `session.log` before launching and wait for a fresh `DIAG ready`.
- **You appended the next line straight after an event** → the asynchronous speech overlapped; leave a pause.

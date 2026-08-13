# Vega Tab

<img src="images/ai.png" class="inline" height="20" alt="Vega"> The default tab, and the one you
leave open while you fly. It starts and stops the AI stack, shows what Vega heard and said,
reports the health of every subsystem, and opens the in-game overlay.

![Vega tab](images/ui-tab-vega.png)

The tab is laid out in four zones: the **conversation** and **diagnostics** logs down the left,
**Quick Status** and **Shortcuts** in the right sidebar, and the **System Summary** telemetry
strip along the bottom.

---

## Conversation

Everything you said and everything Vega said back, in one stream. Your lines are aligned left,
Vega's replies are aligned right, so a long session stays readable at a glance.

## Diagnostics / System Messages

The technical log — service starts, calibration results, binding warnings, file operations. It
is never spoken; it exists so you can see what the app is doing.

Four buttons sit in the section header:

| Button | What it does |
|--------|--------------|
| **Copy** | Copies the text you have selected in the log to the clipboard. Enabled only when there is a selection. |
| **Save debug bundle** | Writes a timestamped `.zip` containing the system log, the application log, your live journal file and your bindings. **This is what to attach to a bug report.** |
| **Dump Vega's memory** | Writes a JSON snapshot of Vega's working memory for the current session. Only available while services are running. |
| **Clear** | Empties the diagnostics log and its export transcript. |

---

## Quick Status

Six live readouts. Each shows a state and a colour, so a glance tells you whether the stack is
healthy.

| Readout | States |
|---------|--------|
| **STT** | `Standby` (services stopped) · `Sleeping` (ignoring you) · `Listening` |
| **LLM** | `Standby` · `Offline` (could not connect) · or the name of the provider actually answering |
| **TTS** | `Standby` · `Local` (Kokoro) · `Cloud` (Google) |
| **Bindings** | `All OK`, or `N missing` |
| **Commands** | How many custom commands are loaded |
| **Keymap** | `In sync` with the game, or `Modified` — you have an unapplied bindings draft |

The **LLM** readout is worth watching. It does not report what you *configured*, it reports
which provider actually answered the last request.

---

## Shortcuts

| Button | What it does |
|--------|--------------|
| **START / STOP SERVICES** | Toggles the whole AI stack. The button disables itself while starting or stopping so it cannot be double-fired. |
| **SLEEP / WAKE UP** | In *wake* mode Vega listens continuously. In *sleep* mode it ignores you unless you use the `listen` bypass word or say `Wake up!`. Disabled while push-to-talk is active — in PTT mode the button *is* the gate. |
| **DISPLAY / HIDE OVERLAY** | Shows the always-on-top [HUD overlay](UI-HUD-Overlay). If the overlay binary is missing the button stays honest and reports the failure in the log rather than claiming an overlay that is not there. |
| **OVERLAY SETUP** | Opens [HUD overlay settings](UI-HUD-Overlay) — transparency, text size, and where it is drawn (monitor, VR headset, both). |
| **Audio Devices** | Opens the Audio Interface dialog to pick your microphone and speaker. Changes take effect on the next service start. |
| **CALIBRATE AUDIO** | Measures your noise floor and speech level and sets the audio gate. Available only while services are running. Run this once before your first flight, and again if you change microphone or room. |
| **Update** | Appears when a new release is available. |

Between the two button groups sits the **commander block** — your name, your ship, the clock,
and your live credit balance.

---

## System Summary

A six-block telemetry strip along the bottom of the tab:

| Block | Meaning |
|-------|---------|
| **LLM Model** | The model that served the most recent request |
| **Session Time** | Time since services started |
| **Tokens Used** | Prompt + completion + cached, for the session |
| **Tokens / Hour** | A projected rate. Stays blank for the first 10 minutes while it collects data |
| **Cache Saved** | Tokens served from cache. `0` is shown deliberately — it is information, not missing data |
| **Last Speed** | Tokens per second on the last response |

For the full breakdown, see the [Stats tab](UI-Stats-Tab).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

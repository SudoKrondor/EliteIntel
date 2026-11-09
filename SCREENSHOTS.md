# V1.2 screenshot checklist

Drop each file into `images/` with **exactly** the filename below, then re-run `node build.js`.
Until a **New** file exists its page shows a broken-image placeholder. A **Retake** file already
exists but shows the V1.1 interface. Replacing a file updates the pages in every language together.

**General notes**

- Capture the **window only**, not the desktop. PNG, roughly 1200px wide; the site scales them down.
- Use the V1.2 colour scheme, with **services running** and a game session loaded wherever the shot
  involves live data. Empty tables and `Standby` badges make the docs look broken.
- Blank or crop any API key.
- Save the shots with the app in **English**.

---

## New

| File | Page | What to capture |
|------|------|-----------------|
| `ui-bindings-axis.png` | `UI-Bindings-Tab` | The axis dialog for a real control (pitch or throttle), with a controller connected, so Direction, Dead zone and Throttle range are all visible. |
| `ui-bindings-anomalies.png` | `UI-Bindings-Tab` | **Binding Profile → Anomalies** with a few conflicts, and ideally a *Shares key with* hover showing. |
| `ui-bindings-settings.png` | `UI-Bindings-Tab` | **Binding Profile → Settings**, with a group of sensitivity or dead zone sliders. |
| `ui-bindings-install-locations.png` | `UI-Bindings-Tab` | **Binding Management → Game Install Locations**: the folder, the Bindings File row and at least one installation showing *Matches*. |
| `ui-bindings-alias-designer.png` | `UI-Bindings-Tab` | **Alias Designer → My Devices** with several named controllers, some *Attached*, and the gear visible. |
| `ui-bindings-device-editor.png` | `UI-Bindings-Tab` | **Device Editor** on the **Buttons** or **Axes** tab, with your own labels and an input lit or a meter moving. |
| `ui-voice-tuning.png` | `UI-Settings-Tab` | **Voice Tuning…** with a voice selected, so the Speed, Volume and Equalizer controls are visible. |
| `ui-journal-import.png` | `UI-Commander-Tab` | **Import Old Journals** after a finished import, with sources listed and the summary showing. |
| `ui-tab-commander-bookmarks.png` | `UI-Commander-Tab` | **Bookmarks** sub-tab with enough rows to show a page-break line. |

## Retake

| File | Page | What to capture |
|------|------|-----------------|
| `ui-tab-vega.png` | `UI-Vega-Tab` | The whole Vega tab, services **running**, a few lines of real conversation, and the five Quick Status readouts (Keymap is gone). The hero shot for the whole UI section. |
| `ui-tab-commander.png` | `UI-Commander-Tab` | Whole tab on **Fleet Management**, with 3–4 ships. It now shows the Journal Directory and Import Old Journals, and five sub-tabs. |
| `ui-commander-announcements.png` | `UI-Commander-Tab` | **Announcements**, with the two new AI toggles. |
| `ui-ship-settings.png` | `UI-Commander-Tab` | The ⚙ popup, scrolled so both the honk section and some Trade Profile rows are visible. |
| `ui-tab-actions-builtin.png` | `UI-Actions-Tab` | **Built-in Commands**, with the scope picker on a real situation. |
| `ui-tab-actions-custom.png` | `UI-Actions-Tab` | **Custom Commands** with 2–3 examples. |
| `ui-custom-command-editor.png` | `UI-Actions-Tab` | The editor with a name, a couple of phrases, and 2–3 steps of different types. |
| `ui-custom-command-navigation.png` | `UI-Actions-Tab` | The Jameson Memorial example from the page, in the new colours. |
| `ui-tab-bindings-profile.png` | `UI-Bindings-Tab` | **Binding Profile → Game Mode**: the Editing line, section tabs, the AI access column and the bottom row of filters. Ideally with the Apply bar lit, one conflict and one *Vega can't use*. |
| `ui-bindings-assign.png` | `UI-Bindings-Tab` | The **Assign control** dialog with both slot cards and Ctrl (or Shift/Alt) held, so the green and red key map shows. |
| `ui-tab-bindings-management.png` | `UI-Bindings-Tab` | **Binding Management → Player Backups**, with backups of different kinds in the Taken column and Backup Settings visible. |
| `ui-tab-settings-ai.png` | `UI-Settings-Tab` | **Services → AI Services**, with the Language row at the top. Best shot: **Cloud Setup** selected, key blanked, so the dimmed Local side is visible. |
| `ui-tab-settings-audio.png` | `UI-Settings-Tab` | **Audio**, **while speaking**, so the Microphone Monitor shows live signal against FLOOR and GATE. |
| `ui-tab-settings-push-to-talk.png` | `UI-Settings-Tab` | **Push To Talk** with a controller and a labelled button selected. |
| `ui-tab-stats.png` | `UI-Stats-Tab` | After 10+ minutes, so Tokens/Hour is populated. A **cloud** provider shows the cache lines a local model hides. |
| `ui-overlay-settings.png` | `UI-HUD-Overlay` | The settings dialog with **VR headset** or **Monitor and headset** selected, so *Position in headset* and its note are visible. |
| `ui-overlay-ingame.png` | `UI-HUD-Overlay` | **Use the full cockpit frame**; see below. A plotted route card showing the new blue and white star dots would be ideal. |

---

## Never crop the overlay out of its cockpit

The overlay matches the cockpit's geometry, so its slope changes with where it is placed on
screen. Rows are slanted lines, not horizontal ones: at a shear of ~0.094 px/px with the value
column ~520px right of the labels, each value renders about two row-heights below its own label,
so `HIP 37735` appears beside NEXT and `KUSHEN` beside JUMPS. That is correct output.

**Cropped to the card alone it reads as a rendering fault** — the lean has nothing to be relative
to, and the values look mis-paired with their labels. In the full frame it reads as intended,
because the game's own HUD in the same region (the Point Defence readouts, the chaff counter)
carries the identical lean, and the card visibly sits on the cockpit's right-hand surface.

So: capture and ship the **whole frame**. The cockpit is not background here, it is the thing that
makes the shot legible, and it is what sells the feature.

---

## Pre-existing gaps

`AMD-RX-7800XT-LLM-Setup.md` references two images that are not in `images/`:

- `rocm-smi-without-game-running-example.png`
- `rocm-smi-with-game-and-Elite-Intel-running-example.png`

They render as broken images on that page in every locale.

---

## Superseded images

Check these against the localized pages before deleting them:

`v1.0.png` · `V1.2.png` · `tab-ai-buttons.png` · `tab-player.png` · `tab-actions.png` ·
`tab-action-build-in-commands.png` · `tab-actions-custom-commands.png` ·
`tab-settings-ai-services.png` · `tab-settings-audio.png` ·
`tab-settings-push-to-talk.png` · `tab-stats.png` · `popup-ship-properties.png` ·
`popup-custom-action.png` · `select_audio_device.png` · `calibrate-audio.png`

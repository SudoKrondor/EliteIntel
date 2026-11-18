# V1.2 screenshot checklist

Drop each file into `images/` with **exactly** the filename below, then re-run `node build.js`.
Until a **New** file exists its page shows a broken-image placeholder. A **Retake** file already
exists but shows an older interface. Replacing a file updates the pages in every language together.

Last checked against `images/` on 2026-11-17, after the 2026-11-17 retake pass.

**General notes**

- Capture the **window only**, not the desktop. PNG; the 2026-11-17 set is 1720×1080, keep that size.
- Use the V1.2 colour scheme, with **services running** and a game session loaded wherever the shot
  involves live data. Empty tables and `Standby` badges make the docs look broken.
- Blank or crop any API key.
- Save the shots with the app in **English**.
- BindForge's sub-tabs are now **Alias Designer · Bind Editor · Preset Editor · Bind File Management**,
  and the Bind Editor has a **Toggles** tab after Dead Zones & Sensitivity.

---

## Missing (referenced by a page, no file in `images/`)

| File | Page | What to capture |
|------|------|-----------------|
| `ui-tab-commander-massacre-sets.png` | `UI-Commander-Tab` | **Commander → Massacre Sets** with sharing **on**, after a sync, so both sections show numbers (Sets you share, Last shared, Sets from other commanders, Usable here, Last updated, Commanders contributing). |
| `ui-bindings-preset-editor.png` | `UI-Bindings-Tab` | **BindForge → Preset Editor**: the four dropdowns (General, Ship, SRV, On-Foot) and **Set all to same**. Best with the four sections on different files so the "do not all load the same binds file" note shows. |
| `rocm-smi-without-game-running-example.png` | `AMD-RX-7800XT-LLM-Setup` | Pre-existing gap: `rocm-smi` output with the game not running. |
| `rocm-smi-with-game-and-Elite-Intel-running-example.png` | `AMD-RX-7800XT-LLM-Setup` | Pre-existing gap: `rocm-smi` output with the game and Elite Intel running. |

## Retake: not yet retaken

| File | Page | What to capture |
|------|------|-----------------|
| `ui-bindings-assign.png` | `UI-Bindings-Tab` | Last changed 2026-11-05. **Assign control** open on a **controller tab**, so the new **Mouse** tab is visible in the tab row and **With Shift / With Ctrl / With Alt** show under the controller. One box ticked is ideal. |
| `llm-setup.png` | `installing-local-llms` | Last changed 2026-11-04. The LMStudio Setup / Cloud Setup strip, cropped as now, with the **Install LM Studio** (or *Set up LM Studio*) button showing. |
| `ui-overlay-ingame.png` | `UI-HUD-Overlay` | Still the V1.1 shot from August. **Use the full cockpit frame** (see below), with a plotted route card showing the **blue (neutron) and white (white dwarf) star dots**. |

## Retake: taken 2026-11-17 but not showing what the page needs

| File | Problem |
|------|---------|
| `ui-bindings-install-locations.png` | Still shows **Run First-Time Startup Again**. Retake once RA lands and that button is gone. |
| `ui-bindings-anomalies.png` | Shows **No conflicts in this binds file**. The page describes conflicts; capture a file with a few, ideally one **controller button** conflict and a *Shares … with* hover. |
| `ui-tab-bindings-profile.png` | Usable as is. Ideally with the Apply bar lit and one *Vega can't use* row. |

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

## Stray and superseded files

`images/ ui-bindings-anomalies.png` (with a **leading space** in the name) is a byte-identical stray
copy of `ui-bindings-anomalies.png`. Nothing references it; delete it.

No page references these. The first four were re-saved on 2026-11-17 as byte-identical copies or
near-copies of the `ui-tab-*` shots, but that does not bring them back into use:

`tab-actions.png` · `tab-player.png` · `tab-settings-ai-services.png` · `tab-settings-audio.png` ·
`v1.0.png` · `v1.1.png` · `tab-ai-buttons.png` · `tab-action-build-in-commands.png` ·
`tab-actions-custom-commands.png` · `tab-settings-push-to-talk.png` · `tab-stats.png` ·
`popup-ship-properties.png` · `popup-custom-action.png` · `select_audio_device.png` ·
`calibrate-audio.png` · `mic-monitor.png` · `keys-binding.png` · `controller.png`

Check them against the localized pages before deleting them.

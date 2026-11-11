# V1.2 screenshot checklist

Drop each file into `images/` with **exactly** the filename below, then re-run `node build.js`.
Until a **New** file exists its page shows a broken-image placeholder. A **Retake** file already
exists but shows an older interface. Replacing a file updates the pages in every language together.

Last checked against the app on `V1.2` at `7af4046c5` (2026-11-10).

**General notes**

- Capture the **window only**, not the desktop. PNG, roughly 1200px wide; the site scales them down.
- Use the V1.2 colour scheme, with **services running** and a game session loaded wherever the shot
  involves live data. Empty tables and `Standby` badges make the docs look broken.
- Blank or crop any API key.
- Save the shots with the app in **English**.
- The main tab is now **BINDFORGE**, not BINDINGS, and its sub-tabs are **Bind Editor · Preset Editor ·
  Binding Management · Alias Designer**. Every whole-window shot taken before 2026-11-10 shows the old
  labels.

---

## New

| File | Page | What to capture |
|------|------|-----------------|
| `ui-bindings-preset-editor.png` | `UI-Bindings-Tab` | **BindForge → Preset Editor**: the four dropdowns (General, Ship, SRV, On-Foot) and **Set all to same**. Best with the four sections on different files so the "do not all load the same binds file" note shows. |

## Retake: BindForge (blocking: these contradict the page text)

| File | Page | What to capture |
|------|------|-----------------|
| `ui-tab-bindings-profile.png` | `UI-Bindings-Tab` | **Bind Editor**, whole window: the bar on top with the **User Configuration Folder** beside Discard/Apply, the four sub-tabs, the one tab row (All … Anomalies, Dead Zones & Sensitivity), the **Action** column, and the three filters including **Show unassigned**. Ideally with the Apply bar lit and one *Vega can't use* row. |
| `ui-bindings-assign.png` | `UI-Bindings-Tab` | **Assign control** open on a **controller tab**, so the new **Mouse** tab is visible in the tab row and **With Shift / With Ctrl / With Alt** show under the controller. One box ticked is ideal. |
| `ui-bindings-install-locations.png` | `UI-Bindings-Tab` | **Binding Management → Game Install Locations**. The User Configuration Folder row has moved to the top bar, so Bindings File is now at the top. **Take it after RA lands**, so *Run First-Time Startup Again* is gone. |
| `ui-bindings-anomalies.png` | `UI-Bindings-Tab` | **Anomalies** with a few conflicts, including at least one **controller button** conflict if you can arrange it, and a *Shares … with* hover. |
| `ui-bindings-settings.png` | `UI-Bindings-Tab` | **Dead Zones & Sensitivity** (renamed from Settings), with a group of sliders. |
| `ui-tab-bindings-management.png` | `UI-Bindings-Tab` | **Binding Management → Player Backups**, backups of different kinds in the Taken column. Old sub-tab row. |
| `ui-bindings-alias-designer.png` | `UI-Bindings-Tab` | **Alias Designer → My Devices**. Old sub-tab row; same content as before. |

## Retake: LM Studio button

| File | Page | What to capture |
|------|------|-----------------|
| `ui-tab-settings-ai.png` | `UI-Settings-Tab` | **Services → AI Services** with the **Install LM Studio** (or *Set up LM Studio*) button under the LMStudio Setup fields. Key blanked. |
| `llm-setup.png` | `installing-local-llms` | The same LMStudio Setup / Cloud Setup strip, cropped as now, but with the LM Studio button showing. |

## Retake: still outstanding from before

| File | Page | What to capture |
|------|------|-----------------|
| `ui-overlay-ingame.png` | `UI-HUD-Overlay` | Still the V1.1 shot from August. **Use the full cockpit frame** (see below), with a plotted route card showing the **blue (neutron) and white (white dwarf) star dots**. |

## Retake when convenient (only the BINDINGS label is wrong)

These whole-window shots are otherwise current. Each shows **BINDINGS** in the main tab bar:

`V1.2.png` (the home page hero) · `ui-tab-vega.png` · `ui-tab-commander.png` · `ui-tab-commander-bookmarks.png` ·
`ui-tab-exo-mastery.png` · `ui-tab-actions-builtin.png` · `ui-tab-actions-custom.png` ·
`ui-tab-settings-audio.png` · `ui-tab-settings-push-to-talk.png` · `ui-tab-stats.png` · `ui-jukebox.png`

`V1.2.png` is the one that matters: it is the first image on the site.

Dialog-only shots (`ui-bindings-axis.png`, `ui-bindings-device-editor.png`, `ui-voice-tuning.png`, the custom
command editor, the ship settings popup) do not show the tab bar and need no retake.

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

`images/ ui-bindings-anomalies.png` (with a **leading space** in the name) is a byte-identical stray
copy of `ui-bindings-anomalies.png`. Nothing references it; delete it.

---

## Superseded images

Check these against the localized pages before deleting them:

`v1.0.png` · `tab-ai-buttons.png` · `tab-player.png` · `tab-actions.png` ·
`tab-action-build-in-commands.png` · `tab-actions-custom-commands.png` ·
`tab-settings-ai-services.png` · `tab-settings-audio.png` ·
`tab-settings-push-to-talk.png` · `tab-stats.png` · `popup-ship-properties.png` ·
`popup-custom-action.png` · `select_audio_device.png` · `calibrate-audio.png`

# V1.1 screenshot checklist

Drop each file into `images/` with **exactly** the filename below, then re-run `node build.js`.
Until a file exists the page shows a broken-image placeholder, so the list doubles as a
progress tracker.

**Status — 4 of 16 received (12 Aug).** ✅ = in `images/` and rendering. ⚠️ = present but
needs a retake, reason noted in the row. Everything unmarked is still outstanding.

**General notes**

- Window size: the app opens at 1200 x 920. Capture the **window only**, not the desktop.
- Take them with **services running** and a game session loaded wherever the shot involves live
  data — empty tables and `Standby` badges make the docs look broken.
- PNG. Roughly 1200px wide is plenty; the site scales them down.
- Blank or crop any API key that is visible.

---

## Vega tab — `UI-Vega-Tab.md`

| File | What to capture |
|------|-----------------|
| ✅ `ui-tab-vega.png` | The whole Vega tab, services **running**, with a few lines of real conversation in the log and the Quick Status badges green. This is the hero shot for the whole UI section. |

## Commander tab — `UI-Commander-Tab.md`

| File | What to capture |
|------|-----------------|
| `ui-tab-commander.png` | Whole tab with the **Ship Options** sub-tab selected, and at least 3–4 ships in the fleet grid. |
| `ui-commander-announcements.png` | The **Announcements** sub-tab — just the toggle grid is enough. |
| `ui-ship-settings.png` | The ⚙ ship settings popup, scrolled so both the honk section and some Trade Profile rows are visible. |

## Actions tab — `UI-Actions-Tab.md`

| File | What to capture |
|------|-----------------|
| `ui-tab-actions-builtin.png` | **Built-in Commands** sub-tab. Set the scope picker to a real situation (e.g. *In ship — supercruise*) so the Place field is populated and the list is a plausible length. |
| `ui-tab-actions-custom.png` | **Custom Commands** sub-tab with 2–3 example commands defined. |
| `ui-custom-command-editor.png` | The custom command editor with a filled-in example — a name, a couple of phrases, and 2–3 steps of different types. |

> Optional extra, not currently referenced: the command details dialog (double-click a
> built-in command). Say the word and I will add it to the page.

## Bindings tab — `UI-Bindings-Tab.md`

| File | What to capture |
|------|-----------------|
| `ui-tab-bindings-profile.png` | **Binding Profile** sub-tab showing the profile/file fields and both tables. Ideally with at least one missing binding and one conflict visible, so the colours mean something. |
| ⚠️ `ui-bindings-assign.png` | **Retake.** The shot received has no modifier held, so every key renders neutral and the green/red map — the whole reason this image exists — is not visible. Re-capture with Ctrl (or Shift/Alt) held down. |
| `ui-tab-bindings-management.png` | **Binding Management** sub-tab with a few backups listed. |

## Settings tab — `UI-Settings-Tab.md`

| File | What to capture |
|------|-----------------|
| `ui-tab-settings-ai.png` | **AI Services** sub-tab. Best shot: **Cloud Setup** selected for the LLM (key blanked or fake) so the dimmed Local column is visible — it shows the switch behaviour. Include the Common strip at the top. |
| ✅ `ui-tab-settings-audio.png` | **Audio** sub-tab, **while speaking**, so the Microphone Monitor shows a live signal against the FLOOR/GATE rails. |
| `ui-tab-settings-push-to-talk.png` | **Push To Talk** sub-tab with a controller connected and a button selected. |

## Stats tab — `UI-Stats-Tab.md`

| File | What to capture |
|------|-----------------|
| `ui-tab-stats.png` | After a session long enough to have real numbers (10+ minutes, so Tokens/Hour is populated rather than "collecting data"). A **cloud** provider is the better shot — it shows the cache lines a local model hides. |

## HUD overlay — `UI-HUD-Overlay.md`

| File | What to capture |
|------|-----------------|
| ⚠️ `ui-overlay-ingame.png` | **Re-crop, ideally with a second card.** The file currently in `images/` is a tight crop around the card alone. Use the **full cockpit frame** instead — see the note below on why. Still only one card (Plotted Route) where 2+ were asked for, so a frame with a trade or exobiology card alongside it would be better again. |
| `ui-overlay-settings.png` | The OVERLAY SETUP dialog, with **VR headset** or **Monitor and headset** selected so the *Position in headset* control and its placement note are visible. |

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

## Pre-existing gaps (not V1.1, but broken today)

`AMD-RX-7800XT-LLM-Setup.md` references two images that are not in `images/`:

- `rocm-smi-without-game-running-example.png`
- `rocm-smi-with-game-and-Elite-Intel-running-example.png`

They render as broken images on that page in all six locales.

---

## Superseded V1.0 images

These are no longer referenced by any page and can be deleted once the new set is in:

`tab-ai-buttons.png` · `tab-player.png` · `tab-actions.png` ·
`tab-action-build-in-commands.png` · `tab-actions-custom-commands.png` ·
`tab-settings-ai-services.png` · `tab-settings-audio.png` ·
`tab-settings-push-to-talk.png` · `tab-stats.png` · `popup-ship-properties.png` ·
`popup-custom-action.png` · `select_audio_device.png` · `calibrate-audio.png`

They are still referenced by the **localized** pages, so hold off until those are translated.

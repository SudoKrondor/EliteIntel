# Transparent HUD Overlay — Design Notes (V1.2 candidate)

**Status:** Shelved / brainstorm captured. Target: **V1.2**, after V1.1 ships.
**Last discussed:** 2026-07-20.
**Author context:** consultation between the maintainer and Claude; no code written yet.

This is a
*resume-in-a-month* document. It records the problem, the dead ends we already ruled out, and the approach we landed on, so we don't re-derive it later.

---

## 1. What we're building

A **transparent, always-on-top HUD
** that floats arbitrary app-owned data over the running game, with the game visible in the gaps (no black/dimmed block).

The content is **not** "missions". It is any label/value the app wants to surface:
next jump target, trade-route destination, a mining-search result system/body, a timer, etc. We call the unit a **HUD
card**. The overlay is a *dumb renderer*; the Java app decides what a card means.

### Hard requirements (from the maintainer)

- Bundled in the installer for **both Linux and Windows**. No "go install X" step.
- Users span **KDE, GNOME, Hyprland, SteamOS (gamescope), Windows**.
- **Real transparency** — per-pixel alpha, not a faded rectangle.
- Game always runs **borderless-fullscreen
  ** (Frontier's exclusive-fullscreen locks up on both OSes and is never fixed; everyone uses borderless). This is
  *good* news:
  no exclusive scanout, so the compositor composites overlays over the game on Windows/KDE/wlroots. The only environment where this fact doesn't save us is gamescope (a nested compositor that scans the game out directly).

---

## 2. The core reality (why this is not a "pick a language" problem)

Wayland **deliberately
** forbids a normal client from declaring always-on-top or absolute positioning. That policy wall is identical whether the renderer is Swing, C++, or Python — which is why the existing Swing attempt has no transparency and no always-on-top over the game on Wayland (AWT's
`PERPIXEL_TRANSLUCENT` reports unsupported under XWayland; same wall, different face).

Two facts dominate every decision:

1. **The compositor decides everything.** KWin (KDE) and wlroots compositors
   (Sway/Hyprland/river/Wayfire) support `wlr-layer-shell` — the sanctioned overlay mechanism. **GNOME/Mutter does not,
   and never will.** gamescope is nested and won't composite external overlays at all.
2. **Borderless-windowed is mandatory** for any compositor-level overlay. We already have that for free.

### Coverage map of the candidate renderers

| Renderer | Windows | KDE | Hyprland/wlroots | GNOME | SteamOS (gamescope) | Bundled & zero-config |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| Win32 layered top-most window | ✅ | — | — | — | — | ✅ |
| `wlr-layer-shell` surface | — | ✅ | ✅ | ❌ (Mutter) | ❌ (nested) | ✅ |
| EDMC's own window (**shelved**) | ✅ | ✅ | ✅ | ✅ | ⚠️ desktop only | ❌ (separate install) |
| Vulkan layer (Linux) + D3D11 present-hook (Windows) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ but a swamp |

**There is no single bundled renderer that floats over the game on all of those.**
Each mechanism has a hole. So the plan is one contract, several thin renderers.

---

## 3. Chosen approach: one contract, several thin renderers

The Java app stays a **pure data producer
** and never knows which renderer is listening. All renderers consume the same HUD-card JSON over a socket.

Ship order for V1.2:

1. **Linux overlay — gtk4-layer-shell.** Bundled in the Linux installer. Covers
   **KDE + Hyprland + wlroots
   ** (the maintainer's machine + majority of Linux desktop users). Dogfoodable immediately on the maintainer's KDE box.
2. **Windows overlay — Win32 layered window.** Bundled in the Windows installer. Covers *all* Windows users. Rock solid.

Holes intentionally left for later / optional:

- **GNOME** and **SteamOS/Deck**: covered later by the shelved EDMC fallback (see §6), or not at all in V1.2.

### Why a sibling process, NOT JNI

- JNI doesn't solve the hard part (we'd
  *still* write a layer-shell client in native code); it only couples the overlay's lifecycle to the JVM and drags native-lib packaging into the Gradle build. A native segfault would take the whole app down.
- Instead: the installer drops a **small native binary** next to the jar; the Java app
  **spawns it as a sibling process
  ** and feeds it over the socket we already have. Crash isolation, no JVM native packaging pain.
- Both native backends (Win32 layered window + Linux layer-shell) can be **one small Rust binary with two compile
  targets**. Cross-platform GUI toolkits (winit/egui)
  do **not** help — on Wayland they're plain `xdg-toplevel` with no always-on-top, so we must talk
  `wlr-layer-shell` and Win32 layered windows *directly* anyway.

---

## 4. Transparency (the "no black block" requirement)

We want **per-pixel alpha
**: opaque glyphs, alpha-0 background, game visible between the letters. NOT uniform whole-window opacity (that's still a faded rectangle).

- **gtk4-layer-shell (Linux): best case.
  ** Wayland surfaces are natively ARGB; per-pixel alpha is the default. Give the GTK window a transparent background (RGBA visual /
  `background: transparent`), draw only text/shapes, empty input region for click-through. Result: floating glyphs, game visible between them.
- **Win32 layered window (Windows): use `UpdateLayeredWindow`** with a 32-bit premultiplied-alpha bitmap (
  `WS_EX_LAYERED` +
  `WS_EX_TRANSPARENT` for click-through). This is the real per-pixel path (how Rainmeter/RTSS/Discord overlays render frameless floating text).
  **Avoid** `SetLayeredWindowAttributes` — that's uniform opacity or a crude chroma-key.

**Legibility caveat:
** a fully transparent background means text must survive whatever is behind it (bright starport, white loading flash, nebula). The fix is
*not* a background block — it's **per-glyph legibility
**: a soft drop-shadow / 1px outline/glow, or at most a small semi-transparent rounded pill sized *to the
text* (alpha ~0.25), never a full-window rectangle. Both native renderers support this; it's a rendering choice, not a platform limit.

---

## 5. Socket contract (to be nailed down first, before any renderer exists)

### Reuse vs. rebuild

Today's WebSocket on port **7497** (`elite.intel.tools.ws.WebSocketBroadcaster`) is a
**debug firehose
**, not a protocol: it broadcasts raw journal lines, EDSM/Spansh request URLs + full response bodies, and LLM payloads, all as untyped strings.
**Do not make the overlay subscribe to that.** Keep the debug firehose separate; expose the HUD on its **own typed
channel** (separate port, or a typed `type`-tagged channel — decide at implementation time). Consider SSE over
`com.sun.net.httpserver.HttpServer`
(JDK-builtin, zero new deps) instead of WS, since the overlay is one-way server→client.

> **Security note (worth fixing regardless of this feature):** the current WS server
> does no `Origin` check, so any web page the user visits can open `ws://localhost:7497`
> and read their journal stream, EDSM traffic (incl. request headers), and LLM prompts.
> Gate it behind a config toggle (off by default) and validate `Origin` on handshake.

### Envelope + card schema (sketch — not final)

Versioned envelope so app and overlay can ship independently:

```json
{
  "v": 1,
  "type": "hud.card",
  "ts": 1721470000,
  "id": "nav-target",
  "category": "nav",
  "title": "Next Jump",
  "lines": [
    "HIP 21991",
    "312 Ly · 4 jumps"
  ],
  "ttl": 0
}
```

- `v` — schema version; overlay refuses a `v` it doesn't understand.
- `id` — stable key. Re-send same `id` to **update/replace** a card; send an empty payload (or a `hud.clear`) to *
  *dismiss** it.
- `category` — drives color/placement (`nav`, `trade`, `mining`, `timer`, …).
- `ttl` — seconds; auto-expire transient cards (0 = sticky).
- On connect, send a **full snapshot** of current cards (the overlay may attach at any time), then deltas.

Data side is mostly a projection over existing state (`MissionManager`,
`MissionDao`, nav/trade/mining state) — little new plumbing.

### Contract-drift guard (two repos)

The overlay is a **separate repo
** (different language, toolchain, release cadence; compositor-specific; and a small auditable repo is what EDMC-wary Linux users trust). The risk of two repos is silent contract drift. Mitigate deliberately:

- `PROTOCOL.md` + a **golden sample JSON fixture** checked into *both* repos.
- A test in EliteIntel asserting the emitted payload still matches the fixture.
- The `v` field lets an old overlay against a new app degrade gracefully.

---

## 6. Explicitly shelved (do not silently resurrect)

- **EDMC (Elite Dangerous Market Connector) integration.** Legit as a *display*
  (plugins render into EDMC's own window, which sidesteps compositor policy entirely), and it's the natural **fallback
  for the GNOME/Deck holes
  **. But: (a) it's a separate heavyweight install, violating "bundled, no special installation", so it can't be the default; (b) tkinter transparency is poor — only uniform
  `-alpha` or a Windows-only
  `-transparentcolor` chroma-key hack (hard-edged, fringes on anti-aliased text, unreliable on Wayland) — so it gives an
  **opaque panel, not a transparent overlay**. Keep as an optional, separate-repo Python plugin
  *if/when* GNOME/Deck coverage matters. Plugin API: Python 3 + tkinter; `plugin_start3`, `plugin_app`,
  `plugin_stop`; no pip in EDMC's frozen runtime (stdlib + vendored pure-Python only); tkinter thread affinity
  (worker thread → `event_generate` → render on Tk main loop).

- **Vulkan-layer (Linux) + D3D11 present-hook (Windows) injection overlay.** The *only*
  truly universal option — draws inside the game's own frames, so it works on every compositor, inside gamescope, even in real fullscreen; always-on-top by construction; perfect transparency. On Linux, ED under Proton is DXVK→Vulkan, so a MangoHud-style implicit layer reaches SteamOS too. Shelved because it's
  **two native codebases doing per-frame rendering
  ** with ongoing maintenance against DXVK/driver churn — a swamp, and the opposite of the app's clean journal-based architecture. Note it does
  **not**
  violate the "no memory reading / no injection *for
  data*" rule: it reads nothing from the game (data still comes from the journal via the socket); it only draws. Revisit only if SteamOS/Deck becomes a primary target.

---

## 7. Resume checklist (when V1.2 work starts)

1. **Lock the contract first.** Finalize the envelope + `hud.card` / `hud.clear`
   schema; write
   `PROTOCOL.md` + golden fixture; add the fixture-match test in EliteIntel; decide SSE-vs-WS and same-port-typed-channel-vs-separate-port.
2. **Split the channel** so the overlay never touches the debug firehose; add the
   `Origin` check + off-by-default toggle to the existing WS server.
3. **Producer side:** project existing nav/trade/mining/mission state into HUD cards; snapshot-on-connect then deltas.
4. **Linux renderer:
   ** gtk4-layer-shell sibling binary (Rust), transparent bg, per-glyph shadow, click-through. Dogfood on KDE.
5. **Windows renderer:** Win32 `UpdateLayeredWindow` sibling binary (same Rust repo, second target).
6. **Installer:
   ** drop the correct native binary per OS; app spawns it as a sibling process and feeds the socket. No JNI.
7. Defer GNOME/Deck (EDMC fallback) and the injection overlay unless demand appears.

---

## Appendix — key files touched/referenced today

- `app/src/main/java/elite/intel/tools/ws/WebSocketBroadcaster.java` — the existing port-7497 firehose (untyped
  `broadcast(String)`; no `Origin` check).
- Broadcast call sites: `JournalParser`, `EdsmApiClient` (incl. request headers),
  `SpanshClient`, `IntraClient`, `AiEndPoint`, `BaseQueryAnalyzer`.
- Existing data model to project from: `db/managers/MissionManager`,
  `db/dao/MissionDao`, plus nav/trade/mining session state.
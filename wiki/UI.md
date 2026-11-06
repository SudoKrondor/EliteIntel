# The Elite Intel User Interface

Elite Intel is organised into seven tabs across the top of the window. Each one owns a different
part of the system, and most of them hold sub-tabs of their own.

This section walks through every tab, every control, and what it actually does.

---

## The seven tabs

| Tab | What it is for |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | The flight deck. Start and stop services, watch the conversation, read live status, open the in-game HUD overlay. |
| <img src="images/commander.png" class="inline" height="20" alt="Commander"> **[Commander](UI-Commander-Tab)** | Who you are and how your ships behave. The journal folder, fleet voices and personalities, automations, spoken announcements, the Exo-Mastery catalogue, and your location bookmarks. |
| <img src="images/actions.png" class="inline" height="20" alt="Actions"> **[Actions](UI-Actions-Tab)** | Everything Elite Intel can do. Browse the built-in command catalogue, and build your own macros. |
| <img src="images/keyboard.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | **BindForge.** Edit every Elite Dangerous control on any device, name your controllers, and back up all your control files. |
| <img src="images/services.png" class="inline" height="20" alt="Services"> **[Services](UI-Settings-Tab)** | The plumbing. Language, language model, speech engine, audio, push-to-talk, and a backup of your settings and data. |
| <img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> **[Jukebox](UI-Jukebox-Tab)** | Your own music, played underneath Vega and ducked automatically when she speaks. |
| <img src="images/stats.png" class="inline" height="20" alt="Stats"> **[Stats](UI-Stats-Tab)** | Token usage and LLM telemetry for the current session. |

There is also the **[HUD Overlay](UI-HUD-Overlay)** — a separate always-on-top window (and an
optional VR surface) driven from the Vega tab.

---

## If this is your first run

Elite Intel speaks its own setup warnings out loud when services start, so you do not have to
hunt for what is missing. In order of importance:

1. **A language model.** Nothing works without one. Go to
   [Services → AI Services](UI-Settings-Tab) and either pick a cloud provider and paste its API
   key, or point the app at a local model. See [Choose your LLM](installing-local-llms).
2. **The journal folder.** Without it Elite Intel is blind to everything happening around your
   ship. [Commander → Commander Profile](UI-Commander-Tab).
3. **Your controls.** Without a bindings file Elite Intel cannot operate your ship. Open the
   [Bindings tab](UI-Bindings-Tab) once so BindForge can set itself up, then press **Auto-Assign VEGA
   Required Binds** to give Vega the keys she needs. If Vega says your control files have not been
   created yet, complete your commander training, then in the game's *Options → Controls*, set the
   galaxy map UI movement to the arrow keys and apply.
4. **Calibrate audio.** Strongly recommended before your first flight.
   [Vega tab](UI-Vega-Tab) → **CALIBRATE AUDIO**.

> Elite Intel is built for **Elite Dangerous Odyssey**. On Horizons, Vega warns you at startup
> that much of it will not work.

---

## Conventions used everywhere

- **Most controls save the moment you change them.** Toggles, sliders and dropdowns write
  through immediately; there is no Save button to forget.
- **Two exceptions wait for you.** *Services → AI Services* holds your edits until you press
  **Save**, and asks you to *Save*, *Discard* or *Keep editing* if you try to leave with unsaved
  changes. Changes on the Bindings tab are held until you press **Apply**, and everything is backed
  up before they reach Elite Dangerous.
- **Language changes rebuild the window.** Selecting a new language on the *Services* tab
  re-renders every tab in that language immediately, and Vega announces the change.
- **Nine languages are supported:** English, Russian, Ukrainian, German, French, Spanish, Italian,
  Portuguese and Brazilian Portuguese.
- **Several commanders on one PC.** Elite Intel keeps each commander's data separately and
  switches automatically when a different commander loads into the game — the fleet list and
  per-commander settings follow.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈

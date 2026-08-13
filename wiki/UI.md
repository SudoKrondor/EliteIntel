# The Elite Intel User Interface

Elite Intel V1.1 is organised into six tabs across the top of the window. Each one owns a
different part of the system, and most of them hold sub-tabs of their own.

This section walks through every tab, every control, and what it actually does.

---

## The six tabs

| Tab | What it is for |
|-----|----------------|
| <img src="images/ai.png" class="inline" height="20" alt="Vega"> **[Vega](UI-Vega-Tab)** | The flight deck. Start and stop services, watch the conversation, read live status, open the in-game HUD overlay. |
| <img src="images/controller.png" class="inline" height="20" alt="Commander"> **[Commander](UI-Commander-Tab)** | Who you are and how your ships behave. Automations, spoken announcements, and a voice and personality per ship. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> **[Actions](UI-Actions-Tab)** | Everything Elite Intel can do. Browse the built-in command catalogue, and build your own macros. |
| <img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **[Bindings](UI-Bindings-Tab)** | Your Elite Dangerous key bindings. Diagnose gaps and conflicts, edit them, and write them back to the game. |
| <img src="images/settings.png" class="inline" height="20" alt="Settings"> **[Settings](UI-Settings-Tab)** | The plumbing. Language, journal folder, language model, speech engine, audio, and push-to-talk. |
| <img src="images/stats.png" class="inline" height="20" alt="Stats"> **[Stats](UI-Stats-Tab)** | Token usage and LLM telemetry for the current session. |

There is also the **[HUD Overlay](UI-HUD-Overlay)** — a separate always-on-top window (and an
optional VR surface) driven from the Vega tab.

---

## If this is your first run

Elite Intel speaks its own setup warnings out loud when services start, so you do not have to
hunt for what is missing. In order of importance:

1. **A language model.** Nothing works without one. Go to
   [Settings → AI Services](UI-Settings-Tab) and either paste a cloud API key or point the app
   at a local model. See [Choose your LLM](installing-local-llms).
2. **The journal folder.** Without it Elite Intel is blind to everything happening around your
   ship. [Settings → Common](UI-Settings-Tab).
3. **The bindings folder.** Without it Elite Intel cannot operate your ship.
   [Bindings → Binding Profile](UI-Bindings-Tab).
4. **Calibrate audio.** Strongly recommended before your first flight.
   [Vega tab](UI-Vega-Tab) → **CALIBRATE AUDIO**.

---

## Conventions used everywhere

- **The window remembers nothing you have not saved.** Only the *Settings → AI Services* tab
  works on a draft: it shows an **Unsaved changes** hint and will stop you leaving the tab
  without deciding. Every other toggle and slider in the app writes through the moment you
  change it.
- **A separate draft model applies to bindings.** Edits go into a draft first and are written
  to Elite Dangerous only when you press **Apply to Game**.
- **Language changes rebuild the window.** Selecting a new language in *Settings → Common*
  re-renders every tab in that language immediately, and Vega announces the change.
- **Nine languages are supported:** English, Spanish, French, German, Italian, Portuguese,
  Brazilian Portuguese, Ukrainian and Russian.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

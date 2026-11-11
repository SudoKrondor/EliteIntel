# Installation

Elite Intel **V1.2** is the current release.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Download the [👉**installer**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Run the installer and follow the on-screen prompts.
3. Set up a language model. **Elite Intel cannot run without one.** Two options:
   - **Local LLM** (free, offline): see the [**Local LLM guide**](installing-local-llms).
     Requires capable GPU hardware.
   - **Cloud LLM** (paid, and easier to set up): see
     [**Cloud LLM options**](cloud-llm-options) for how to get an API key, then enter it in
     [**Services → AI Services**](UI-Settings-Tab).

Setup complete. Next: [**the user interface, tab by tab**](UI).

### First-run checklist

Elite Intel speaks these warnings out loud when services start, so you will hear about anything
missing — but they are worth doing up front:

| Step | Where |
|------|-------|
| Point it at a language model | [Services → AI Services](UI-Settings-Tab) |
| Check the journal folder | [Commander tab](UI-Commander-Tab) |
| Let BindForge set up, then give Vega the keys she needs | [BindForge tab](UI-Bindings-Tab) |
| Calibrate audio | [Vega tab](UI-Vega-Tab) → **CALIBRATE AUDIO** |

---

### New to Elite Dangerous?

The game creates its control files only after you have changed something, and Elite Intel cannot
operate your ship without them. Complete your commander training, then in the game's *Options →
Controls*, set the galaxy map UI movement to the arrow keys and apply.

### Moving to another computer

[**Backup Settings & Data**](UI-Settings-Tab) on the Services tab puts your settings, your data and
your custom commands in one file you can restore on the new machine.

---

### Uninstall (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
For issues, report on Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈 (new to Matrix? read the [**Matrix How To**](Matrix-How-To))

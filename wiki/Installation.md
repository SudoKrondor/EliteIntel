# Installation

Elite Intel **V1.1** is the current release.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Download the [👉**installer**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Run the installer and follow the on-screen prompts.
3. Set up a language model. Two options:
   - **Local LLM** (free, offline): see the [**Local LLM guide**](installing-local-llms).
     Requires capable GPU hardware.
   - **Cloud LLM** (has a free tier and is easier to set up): see
     [**Cloud LLM options**](cloud-llm-options) for how to get an API key, then enter it in
     [**Settings → AI Services**](UI-Settings-Tab).

Setup complete. Next: [**the user interface, tab by tab**](UI).

### First-run checklist

Elite Intel speaks these warnings out loud when services start, so you will hear about anything
missing — but they are worth doing up front:

| Step | Where |
|------|-------|
| Point it at a language model | [Settings → AI Services](UI-Settings-Tab) |
| Check the journal folder | [Settings → Common](UI-Settings-Tab) |
| Check the bindings folder, and fix any missing bindings | [Bindings tab](UI-Bindings-Tab) |
| Calibrate audio | [Vega tab](UI-Vega-Tab) → **CALIBRATE AUDIO** |

---

### Uninstall (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
For issues, report on Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

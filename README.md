## ⚙️ [INSTALL](https://www.elite-intel.org/Installation.html) | 🎓 [**WIKI**](https://www.elite-intel.org) | 💻[TECH STUFF](https://github.com/stone-alex/EliteIntel/blob/master/TECHNICAL.md)

# Elite Intel
## V1.0 English only - require English game.
## V1.1 Multi lingual in the active development.



--- 

## Linux and Windows Installers

Can be ran 100% off-line or with various cloud services

1. Download the [👉**V1.0 installer**👈](https://github.com/stone-alex/EliteIntel/releases), or Apply for [**V1.1 Beta
   Test**](https://matrix.to/#/#krondor:matrix.org)
2. Run the installer and follow the on-screen prompts.
3. Set up an LLM. Two options are available:
    - **Local LLM** (free, offline): See the [**Local LLM
      **](https://www.elite-intel.org/installing-local-llms.html). Requires capable GPU hardware.
    - **Cloud LLM** (easier to set up): See the [**Cloud LLM**](https://www.elite-intel.org/cloud-llm-options.html).

---

For issues, report on Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

### 👉[Free cloud if you don't have hardware for local LLM](https://console.mistral.ai/home)👈

- You may experience some occasional timeouts or lag, but for the most part it is fine.

### Paid cloud services
- Claude | claude-haiku-4-5
- Grok | grok-4-1-fast-non-reasoning
- Open AI | gpt-5.2
- Google Gemini | gemini-3.1-flash-lite
- Deepseek | deepseek-v4-flash

The cost will very depending on which one you choose.

### Local LLM
- **LMStudio (Local off-line Model matrixportalx/tulu-3.1-8b-supernova)**
- **Ollama (Local off-line Model matrixportalx/tulu-3.1-8b-supernova)**

[Why tulu-3.1-8b Supernova specifically](https://github.com/stone-alex/EliteIntel/wiki/Why-Tulu3.1-supernova)

### Minimum requirements for **running the game **and** the Local LLM on the same GPU**

- NVIDIA RTX 3090 **24GB VRAM** or equivalent.

- RTX 5090 with only **12GB VRAM will not do**. Not enough VRAM.
    - Game takes 8GB (or more)
    - LLM takes 6.5GB
See [**Configure the app**](UI-and-Configuration-Options) for next steps.

- 👉 Video by DawnTreader for [Windows Users on how to setup LMStudio on Windows](https://www.youtube.com/watch?v=F5RgRRePrTo)
- 👉 Video by Sudo Krondor for [Linux Users on how to setup LMStudio on Linux](https://www.youtube.com/watch?v=2HGFmlZGK1g)

---

### Realtek users
Realtek's "enhancements" are a legitimate source of audio problems with Java's javax.sound.sampled.
The auto noise suppression, equalizer, and "audio enhancements" Realtek enables by default can interfere with raw
PCM capture because they process the audio stream at the driver level before Java ever sees it.
That could and often does manifest as degraded STT quality or audio cutting out.

---

## Overview

**✅ The off-line TTS. (Linux and Windows)**
Powered by Kokoro. built-in / included.

**✅ The off-line STT. (Linux and Windows)**
Powered by NVIDIA Parakeet. built-in / included.

**✅ Local off-line private LLM**
[Installing local LLM](https://github.com/stone-alex/EliteIntel/wiki/installing-local-llms)

Elite Intel is your AI-powered, sidekick for *Elite Dangerous*.
It's a Quality of Life (QoL) app that enhances gameplay with voice-activated commands 
**uttered in natural language**,

real-time journal parsing, and smart AI assistance-all while staying fully compliant
with Frontier Developments' Terms of Service. No AFK play, and no unfair advantages;
everything requires your input.

The app uses Speech-to-Text (STT) and Text-to-Speech (TTS) for **natural voice interactions**,
and LLM for handling commands, queries, and chats. It processes game data from journal 
and auxiliary files, pulls in info from EDSM when needed, and keeps things responsive.

---

## Contributing

Fork it and submit pull requests! Keep things modular and event-driven, and test your changes. For dev
details, check out `DEVELOPERS.md` in the root. It covers the architecture (decoupled, multithreaded, event registry via
reflection) and principles like DRY and SRP. All PRs go through review.

## License

The project is open-source under a [Creative Commons license CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.en),
so feel free to check out the code and contribute. (see [DEVELOPERS.md](DEVELOPERS.md) for details)

## Contact Developer / Join the Community
If you run into issues, hit us up on GitHub issues or Feedback and bug reports are super welcome!
To get in touch: 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

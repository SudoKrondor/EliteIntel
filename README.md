# Elite Intel

**AI voice assistant for Elite Dangerous.** Speak naturally, fly better.

[Download](https://github.com/stone-alex/EliteIntel/releases) · [Installation](https://www.elite-intel.org/Installation.html) · [Documentation](https://www.elite-intel.org) · [Community](https://app.element.io/#/room/#krondor:matrix.org) · [Contributing](DEVELOPERS.md)

---

Elite Intel connects your voice to your ship. There are no command lists to memorize and no syntax to learn: say what
you mean, and the AI works out what you want and does it. It handles ship systems, route plotting, trade, exploration,
exobiology, colonisation and pirate massacre missions. It also gives you real analysis, such as a breakdown of your
loadout or a reading of the signals in the system, using what the game journal reports and live data from Spansh, EDSM
and EDDN. It never reads game memory and never injects into the game process.

The whole pipeline can run offline on your own PC: Parakeet speech recognition, a local language model in LM Studio
(which Elite Intel can install and set up for you on a capable GPU), and Supertonic-3 voices. Each ship in your fleet
can have its own voice and personality. You can also mix in cloud services: Anthropic, OpenAI, xAI, Gemini or DeepSeek
for the language model, and Google for speech.

Alongside the assistant you get **BindForge**, which edits and backs up your controls for keyboard, mouse, HOTAS and
pedals from outside the game, plus a HUD overlay, a jukebox for your own music, custom voice commands and color themes
(the default theme is tuned for color-blind commanders).

Elite Intel runs on **Windows and Linux** and supports English, German, French, Spanish, Italian, Portuguese,
Brazilian Portuguese, Russian and Ukrainian.

> ⚠️ **A language model is required.** Elite Intel is the voice and the hands, and the LLM is the brain. Choose a
> [local LLM](https://www.elite-intel.org/installing-local-llms.html) (free, needs a capable GPU) or a
> [cloud LLM](https://www.elite-intel.org/cloud-llm-options.html) (your own API key).

Everything else, including setup guides, every command and query, and a tour of each tab, is on
**[elite-intel.org](https://www.elite-intel.org)**.

## Getting Started

1. Download the installer for your platform from [Releases](https://github.com/stone-alex/EliteIntel/releases).
2. Run it and follow the prompts ([installation guide](https://www.elite-intel.org/Installation.html)).
3. Set up a language model, either local or cloud (see above).

## Contributing

Elite Intel is open source, and bug reports, feature requests and pull requests are welcome. The app is a Java 21,
event-driven, modular codebase. Read [DEVELOPERS.md](DEVELOPERS.md) for the architecture and
[CODING_STANDARD.md](CODING_STANDARD.md) for the conventions. Every PR is reviewed.

## Community and Support

Report bugs on [GitHub Issues](https://github.com/stone-alex/EliteIntel/issues), or join us on
[Matrix](https://app.element.io/#/room/#krondor:matrix.org). If you are new to Matrix, read the
[how-to](https://www.elite-intel.org/Matrix-How-To.html).

## License

[MIT](LICENSE). Third-party components keep their own licenses. See
[THIRD_PARTY_NOTICES.md](distribution/THIRD_PARTY_NOTICES.md).

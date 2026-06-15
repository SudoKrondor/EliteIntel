This guide covers installation and initial configuration for Elite Intel on Windows and Linux.

## Windows 🪟
1. Download the [👉**installer**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Run the installer and follow the on-screen prompts.
   - **Parakeet STT** (local speech recognition) and **Kokoro TTS** (local text-to-speech) are both included. No additional steps or services are required.
3. Set up an LLM. Two options are available:
   - **Local LLM** (free, offline): See the [**Local LLM guide**](installing-local-llms). Requires capable GPU hardware.
   - **Cloud LLM** (easier to set up): See the [**Configure the app**](UI-and-Configuration-Options) guide for API key setup.

---

## Linux 🐧
### Installation (no sudo required)
1. Download the installer script:

```shell
curl -L -o installer.sh https://raw.githubusercontent.com/stone-alex/EliteIntel/refs/heads/master/distribution/installer.sh
```

2. Make the script executable and run it:
```shell
chmod +x installer.sh
./installer.sh
```
The app installs to `~/.var/app/elite.intel.app`.
Both **Parakeet STT** and **Kokoro TTS** are bundled with the app. No additional installation is needed. Enable them in the app via the **Settings tab ☑ Use** checkboxes.

3. Set up an LLM. Two options are available:
   - **Local LLM** (free, offline): See the [**Local LLM guide**](installing-local-llms). Requires capable GPU hardware.
   - **Cloud LLM** (easier to set up): See the [**Configure the app**](UI-and-Configuration-Options) guide for API key setup.

Setup complete. See [**Configure the app**](UI-and-Configuration-Options) for next steps.

---

### Uninstall

Use the `-d` flag to remove the app. The installer will prompt before deleting configuration and API key data.

```shell
bash installer.sh -d
```

---

## First Run Checklist

Locate the Elite Intel icon in your desktop environment. It may appear under Games or Utilities depending on your DE.

Once installed and configured, complete the following before the first session:
1. Start the app and open the **Player tab**. Set the Journal and Bindings directory path. The app requires these to process game data. Restart services after setting the paths.
2. Start Services.
3. Run **Recalibrate Audio**. This calibrates the engine to room noise and microphone characteristics. Recalibrate whenever hardware changes. The oscilloscope on the Settings/Audio tab turns green while speaking and red when silent.

----
For issues, report on Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

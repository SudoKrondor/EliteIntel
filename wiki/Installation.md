### The released version is V1.0 is different from what you see on screenshots.

### If you want version V1.1 join the beta test team. 
### 👉[**Join the Beta Test Team here V1.1**](https://matrix.to/#/#krondor:matrix.org)👈

---

## <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Download the [👉**installer**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Run the installer and follow the on-screen prompts.
   - **Parakeet STT** (local speech recognition) and **Kokoro TTS** (local text-to-speech) are both included. No additional steps or services are required.
3. Set up an LLM. Two options are available:
   - **Local LLM** (free, offline): See the [**Local LLM guide**](installing-local-llms). Requires capable GPU hardware.
   - **Cloud LLM** (easier to set up): See the [**Configure the app**](UI-and-Configuration-Options) guide for API key setup.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux
### Installation (any desktop distro - no sudo required)
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

Setup complete. See [**Configure the app**](Configuration) for next steps.

---

### Uninstall

Use the `-d` flag to remove the app. The installer will prompt before deleting configuration and API key data.

```shell
bash installer.sh -d
```

----
For issues, report on Matrix. Bug reports and pull requests are welcome.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

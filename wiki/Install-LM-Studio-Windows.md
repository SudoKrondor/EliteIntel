## Local LLM - Windows Setup (LM Studio)

Running a local LLM keeps all data private and offline. There are no subscription fees. Hardware and electricity costs apply.

It requires [LM Studio](https://lmstudio.ai) and a capable GPU.

---

### Who this guide is for

This guide sets LM Studio up by hand on a PC that **runs only the model**: a second PC on your network, while
the game runs on your gaming PC, which then does no inference work at all.

> **Game and model on one PC?** You don't need this guide. If your gaming PC has an NVIDIA RTX 3090 (24 GB) or
> better, or two or more cards with about 20 GB between them, Elite Intel installs and sets up LM Studio for
> you: see [Choose your LLM](installing-local-llms).

### Hardware for the LLM PC

- **12 GB of video memory.** The model takes about 6.3 GB, so this leaves headroom.
- **RTX 3080-class processing power or better.** A slower card holds the model but answers too slowly to be
  useful.

---

### Recommended Model

| Model | VRAM Required  | Notes                  |
|---|----------------|------------------------|
| `google/gemma-4-e4b` | ~6.3 GB        | ✅ Required |

> **Why this one?** It supports the function calling the companion needs. A model that cannot emit
> a tool call cannot drive the app, however well it writes. See [Choose your LLM](installing-local-llms).

---

Detailed (very detailed) Video tutorial by @DawnTreaderToolsoftheElite 

> **Note:** The video shows older versions of LM Studio and Elite Intel. Where it differs from the steps below, follow the steps below.

[[youtube:F5RgRRePrTo]]

---

### Step 1 - Install LM Studio

Open **PowerShell** and run:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

This installs the `lms` CLI and **llmster**, LM Studio's background server. It does **not** install the LM Studio desktop app. Open a **new** PowerShell window after installation for the changes to take effect.

Verify it worked:

```powershell
lms --help
```

> **Note:** If the LM Studio desktop app is already installed, the `lms` CLI may already be available. Run `lms --help` before running the install script.

---

### Step 2 - Download the Model

Start the LM Studio background server, then download the model:

```powershell
lms daemon up
lms get google/gemma-4-e4b
```

If the name does not resolve to an exact match, `lms get` lists the candidates it found and you
pick one with the arrow keys and Enter.

[LM Studio has a known bug](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). In some cases the download fails with:
```Error: No staff picks found with the specified search criteria.```

If that occurs, download the GGUF by hand from the model's page on HuggingFace, then import it:

```powershell
lms import C:\path\to\the-model.gguf
```

To list downloaded models:

```powershell
lms ls
```

---

### Step 3 - Start the Server

Load the model and start the inference server:

```powershell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start --bind 0.0.0.0
```

`lms daemon up` does nothing if the background server is already running, so it is safe to include every time. `--bind 0.0.0.0` lets other PCs on your network reach the server, which by default answers only this one.

**NOTE**: `--context-length 8192` is required, and it must be exactly 8192: a smaller window makes the model hallucinate, and a larger one makes every answer too slow.

> **Let the gaming PC in.** The first time the server listens on the network, Windows Defender Firewall may ask
> whether to allow it. Allow it on **private** networks. If it never asks and the gaming PC cannot connect, add an
> inbound rule for TCP port 1234.

Verify it from the **gaming PC** by opening a browser at this address, with the LLM PC's address in place of `LLM_PC_IP`:

```
http://LLM_PC_IP:1234/v1/models
```

You should receive a JSON list of loaded models. The model ID string in that response is what you will enter in Elite Intel's **Model** field.

To stop the server:

```powershell
lms server stop
```

> ⚠️ **Important:** The LM Studio server does **not** survive reboots. Run the three Step 3 commands again after each restart, or use one of the auto-start options below.

---

### Step 4 - (Optional) Auto-Start on Boot

Two options are available to keep the server running across reboots. If you followed Step 1, use **Option A**. Option B is only for people who also have the LM Studio desktop app installed.

#### Option A - Task Scheduler (no desktop app needed)

First, save this script as `start-lmstudio.ps1` in a permanent location (e.g. `C:\Scripts\`):

```powershell
$lms = "$env:USERPROFILE\.lmstudio\bin\lms.exe"
& $lms daemon up
& $lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
& $lms server start --bind 0.0.0.0
```

It starts the background server, loads the model with the settings Elite Intel needs, and starts the inference server: the same three commands as Step 3.

Then create the task:

1. Press **Win + R**, type `taskschd.msc`, press Enter.
2. Click **Create Task** in the right panel.
3. **General tab**: Name it `LM Studio Server`. Leave **"Run with highest privileges"** unchecked: LM Studio does not need administrator rights.
4. **Triggers tab**: Click New → **"At log on"** → OK.
5. **Actions tab**: Click New → **"Start a program"**.
   - Program/script: `powershell.exe`
   - Add arguments: `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\Scripts\start-lmstudio.ps1"`

`-WindowStyle Hidden` keeps a console window from staying open at every login. A window may flash briefly while the script starts.

#### Option B - Desktop App

This option is only available if the LM Studio desktop app is installed. Step 1 does not install it.

1. Open LM Studio and press **Ctrl + ,** to open Settings.
2. Check **"Run LLM server on login"**.
3. In the server settings, turn on **Serve on Local Network**, so the gaming PC can reach it.
4. Closing the app minimizes it to the system tray and keeps the server running. It restores automatically on next login.

> ⚠️ **Context length:** With this option, LM Studio loads the model only when the first request arrives, using the model's **default** settings instead of the `--context-length 8192` from Step 3. Open the model's default settings in the desktop app and set **Context Length** to **8192**. Otherwise the context window may be too small.

---

### Step 5 - Configure Elite Intel

On the **gaming PC**, in Elite Intel, open **Services → AI Services**. Under **Language Model (LLM)**, select **LMStudio Setup**:

- **Address**: `http://LLM_PC_IP:1234/v1/chat/completions`, with `LLM_PC_IP` replaced by the LLM PC's address on your network (`ipconfig` on the LLM PC shows it).
- **Model**: paste in the model ID string from `http://LLM_PC_IP:1234/v1/models`. One model serves both commands and queries.

No API key is needed for a local LLM.

Click **Save**. Elite Intel restarts the language model on its own.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈

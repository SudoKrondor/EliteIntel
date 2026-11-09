## Local LLM - Windows Setup (LM Studio)

Running a local LLM keeps all data private and offline. There are no subscription fees. Hardware and electricity costs apply.

It requires [LM Studio](https://lmstudio.ai) and a capable GPU.

---

### Minimum Hardware

To run Elite Dangerous and the LLM on the **same machine**, a minimum of an **NVIDIA RTX 3060 with 12 GB VRAM** is required.

> **Tip:** Elite Intel can be pointed at an LM Studio instance running on a **separate PC** on your network. If a second machine with a capable GPU is available, the game PC carries no inference load in this configuration.

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
lms server start
```

`lms daemon up` does nothing if the background server is already running, so it is safe to include every time.

**NOTE**: The `--context-length 8192` flag is required. Without it, the context window may be too small, causing prompt truncation, failures, and hallucinations.

Verify it is running by opening a browser or another PowerShell window and navigating to:

```
http://localhost:1234/v1/models
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
& $lms server start
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
3. Closing the app minimizes it to the system tray and keeps the server running. It restores automatically on next login.

> ⚠️ **Context length:** With this option, LM Studio loads the model only when the first request arrives, using the model's **default** settings instead of the `--context-length 8192` from Step 3. Open the model's default settings in the desktop app and set **Context Length** to **8192**. Otherwise the context window may be too small.

---

### Step 5 - Configure Elite Intel

In Elite Intel, open **Services → AI Services**. Under **Language Model (LLM)**, select **LMStudio Setup**:

- **Address**: `http://localhost:1234/v1/chat/completions` (the default). If LM Studio is on another machine, replace `localhost` with that machine's IP.
- **Model**: paste in the model ID string from `http://localhost:1234/v1/models`. One model serves both commands and queries.

No API key is needed for a local LLM.

Click **Save**. Elite Intel restarts the language model on its own.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈

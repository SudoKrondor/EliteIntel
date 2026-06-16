## Local LLM - Windows Setup (LM Studio)

Running a local LLM keeps all data private and offline. There are no subscription fees. Hardware and electricity costs apply.

LM Studio is an alternative to Ollama. It uses the same models and the same OpenAI-compatible API. The choice can be changed in settings at any time.

It requires [LM Studio](https://lmstudio.ai) and a capable GPU.

---

### Minimum Hardware

To run Elite Dangerous and the LLM on the **same machine**, a minimum of an **NVIDIA RTX 3060 with 24 GB VRAM** is required.

> **Tip:** Elite Intel can be pointed at an LM Studio instance running on a **separate PC** on your network. If a second machine with a capable GPU is available, the game PC carries no inference load in this configuration.

---

### Recommended Model

| Model | VRAM Required | Notes |
|---|---|---|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB | ✅ Recommended. Fast, accurate, works great for commands and queries. |
| `tulu-3.1-8b-supernova` Q8_0 | ~8.5 GB | Higher quality, if VRAM headroom is available. |
| `qwen3` 8B | ~8 GB | Experimental. Expect occasional missed commands and hallucinations. |

---

Detailed (very detailed) Video tutorial by @DawnTreaderToolsoftheElite 

[[youtube:F5RgRRePrTo]]

---

### Step 1 - Install LM Studio

Open **PowerShell** and run:

```powershell
irm https://lmstudio.ai/install.ps1 | iex
```

This installs the `lms` CLI and the LM Studio runtime. Open a **new** PowerShell window after installation for the changes to take effect.

Verify it worked:

```powershell
lms --help
```

> **Note:** If the LM Studio desktop app is already installed, the `lms` CLI may already be available. Run `lms --help` before running the install script.

---

### Step 2 - Download the Model

```powershell
lms get matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

or

```powershell
lms get Tulu-3.1
```
and choose the `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF` variant (may be listed as `Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`).

To list downloaded models:

```powershell
lms ls
```

---

### Step 3 - Start the Server

Load the model and start the inference server:

```powershell
lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
lms server start
```

**NOTE**: The `--context-length 8192` flag is required. Without it, the context window may be too small, causing prompt truncation, failures, and hallucinations.

Verify it is running by opening a browser or another PowerShell window and navigating to:

```
http://localhost:1234/v1/models
```

You should receive a JSON list of loaded models. The model ID string in that response is what you will enter in Elite Intel's **LLM Model** field.

To stop the server:

```powershell
lms server stop
```

> ⚠️ **Important:** The LM Studio server does **not** survive reboots. Run `lms server start` again after each restart, or use one of the auto-start options below.

---

### Step 4 - (Optional) Auto-Start on Boot

Two options are available to keep the server running across reboots.

#### Option A - Desktop App

If the LM Studio desktop app is installed, this is the simplest approach:

1. Open LM Studio and press **Ctrl + ,** to open Settings.
2. Check **"Run LLM server on login"**.
3. Closing the app minimizes it to the system tray and keeps the server running. It restores automatically on next login.

#### Option B - Task Scheduler (Headless / No GUI)

1. Press **Win + R**, type `taskschd.msc`, press Enter.
2. Click **Create Task** in the right panel.
3. **General tab**: Name it `LM Studio Server`. Check **"Run with highest privileges"**.
4. **Triggers tab**: Click New → **"At log on"** → OK.
5. **Actions tab**: Click New → **"Start a program"**.
   - Program/script: `%USERPROFILE%\.lmstudio\bin\lms.exe`
   - Add arguments: `server start`

To also load the model automatically, create a batch file instead:

```batch
@echo off
%USERPROFILE%\.lmstudio\bin\lms.exe daemon up
%USERPROFILE%\.lmstudio\bin\lms.exe load tulu-3.1-8b-supernova --yes --context-length 8192 --gpu max
%USERPROFILE%\.lmstudio\bin\lms.exe server start
```

Save it as `start-lmstudio.bat` in a permanent location (e.g. `C:\Scripts\`) and point the Task Scheduler action at that file.

---

### Step 5 - Configure Elite Intel

Open the **Settings tab** in Elite Intel:

- Leave the **LLM Key** field blank (local LM Studio does not require one).
- **LLM Address**: set to `http://localhost:1234/v1/chat/completions`. If LM Studio is on another machine, replace `localhost` with that machine's IP.
- **LLM Model**: paste in the model ID string from `http://localhost:1234/v1/models`.
- **Command LLM**: set to the same model ID.
- **Query LLM**: set to the same model ID.
- Click **Stop** then **Start** on the AI tab to apply changes.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

## Local LLM - Windows Setup (Ollama)

Running a local LLM keeps all data private and offline. There are no subscription fees. Hardware and electricity costs apply.

It requires [Ollama](https://ollama.com) and a capable GPU.

---

### Minimum Hardware

To run Elite Dangerous and the LLM on the **same machine**, a minimum of an **NVIDIA RTX 3060 with 12 GB VRAM** is required. Performance headroom is limited at this specification.

> **Tip:** Elite Intel can be pointed at an Ollama instance running on a **separate PC** on your network. If a second machine with a capable GPU is available, the game PC carries no inference load in this configuration.

---

### Recommended Model

| Model | VRAM Required  | Notes                  |
|---|----------------|------------------------|
| `google/gemma-4-e4b` | ~6.3 GB        | ✅ Required |

> **Why this one?** It supports the function calling the companion needs. A model that cannot emit
> a tool call cannot drive the app, however well it writes. See [Choose your LLM](installing-local-llms).

> **Note:** For the fastest local inference, consider [LM Studio](Install-LM-Studio-Windows). In testing, it is noticeably faster than Ollama on the same hardware with the same model.

---

### Step 1 - Install Ollama

- Go to [https://ollama.com/download](https://ollama.com/download)
- Download and run `OllamaSetup.exe`. No admin rights required.
- Ollama installs and runs in the system tray. It auto-starts on login.

---

### Step 2 - Pull a Model

Open **Command Prompt** or **PowerShell** and run:

```shell
ollama pull google/gemma-4-e4b
```

---

### Step 3 - (Optional) Tune the Configuration

Ollama works without tuning. The following configuration improves VRAM management when running alongside Elite Dangerous.

On Windows, Ollama reads configuration from **user environment variables**.

1. Right-click the Ollama tray icon and select **Quit**.
2. Open **Settings** and search for "environment variables".
3. Click **"Edit environment variables for your account"**.
4. Add each variable below using **New**:

| Variable | Value | Notes |
|---|---|---|
| `OLLAMA_MAX_VRAM` | `14000000000` | 14 GB cap. Adjust based on your GPU and game requirements. |
| `OLLAMA_NUM_PARALLEL` | `3` | Covers Elite Intel's async call patterns without over-allocating. |
| `OLLAMA_MAX_LOADED_MODELS` | `1` | One model in VRAM at a time. |
| `OLLAMA_FLASH_ATTENTION` | `1` | Faster inference. |
| `OLLAMA_KEEP_ALIVE` | `-1` | Keeps model loaded permanently. |

5. Click **OK**. Relaunch Ollama from the Start Menu.

#### What these settings do

**`OLLAMA_MAX_VRAM`**: Hard cap on VRAM Ollama can use, in bytes. Leaves the remainder for Elite Dangerous. Adjust based on your GPU and game requirements.

**`OLLAMA_NUM_PARALLEL`**: Number of requests Ollama handles simultaneously. Elite Intel makes async calls, so setting this too low causes failures. `3` covers the typical command and query overlap without over-allocating.

**`OLLAMA_MAX_LOADED_MODELS`**: Keeps only one model in VRAM at a time.

**`OLLAMA_FLASH_ATTENTION`**: Enables Flash Attention, which reduces memory bandwidth usage during inference. Generally faster, especially for repeated requests.

**`OLLAMA_KEEP_ALIVE=-1`**: Keeps the model loaded in VRAM indefinitely. Without this, Ollama may unload the model after a period of inactivity, incurring a reload penalty on the next request.

---

### Step 4 - Configure Elite Intel

Open the **Settings tab** in Elite Intel:

- Leave the **LLM Key** field blank (local Ollama does not require one).
- **LLM Address** defaults to `http://localhost:11434/api/chat`. If Ollama is on another machine, replace `localhost` with that machine's IP.
- **LLM Model**: set to `google/gemma-4-e4b`.
- **Command LLM**: set to `google/gemma-4-e4b`.
- **Query LLM**: set to `google/gemma-4-e4b`.
- Click **Stop** then **Start** on the AI tab to apply changes.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

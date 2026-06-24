## Local LLM - Linux Setup (Ollama)

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
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB          | ✅ Recommended for V1.0 |
| `google/gemma-4-e4b` | ~6.3 GB        | ✅ Recommended for V1.1 |

> **Which model?** `tulu-3.1-8b-supernova` is the recommended model for **V1.0**. **V1.1** switches to `google/gemma-4-e4b`, which supports the function calling required by the new companion feature. The commands below use the V1.1 model — on V1.0, substitute `tulu-3.1-8b-supernova`.

> **Note:** For the fastest local inference, consider [LM Studio](Install-LM-Studio-Linux) with `matrixportalx/tulu-3.1-8b-supernova`. In testing, it is noticeably faster than Ollama on the same hardware with the same model.

---

### Step 1 - Install Ollama

```shell
curl -fsSL https://ollama.com/install.sh | sh
```

Ollama installs as a systemd service and starts automatically.

---

### Step 2 - Pull a Recommended Model

For **V1.1**, pull `google/gemma-4-e4b`:

```shell
ollama pull google/gemma-4-e4b
```

For **V1.0**, pull `tulu-3.1-8b-supernova`:

```shell
ollama pull hf.co/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF
```

---

### Step 3 - (Optional) Tune the Ollama Service

Ollama works without tuning. The following configuration improves VRAM management when running alongside Elite Dangerous.

```shell
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Paste this in:

```ini
[Service]
Environment="OLLAMA_MAX_VRAM=14000000000"
Environment="OLLAMA_DEBUG=0"
Environment="OLLAMA_NUM_PARALLEL=3"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_FLASH_ATTENTION=1"
Environment="OLLAMA_KEEP_ALIVE=-1"
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=5
```

Then reload and restart:

```shell
sudo systemctl daemon-reload
sudo systemctl restart ollama.service
```

#### What these settings do

**`OLLAMA_MAX_VRAM`**: Hard cap on VRAM Ollama can use, in bytes. `14000000000` = 14 GB. Leaves the remainder for Elite Dangerous. Adjust based on your GPU and game requirements.

**`OLLAMA_NUM_PARALLEL`**: Number of requests Ollama handles simultaneously. Elite Intel makes async calls, so setting this too low causes failures. `3` covers the typical command and query overlap without over-allocating.

**`OLLAMA_MAX_LOADED_MODELS`**: Keeps only one model in VRAM at a time.

**`OLLAMA_FLASH_ATTENTION`**: Enables Flash Attention, which reduces memory bandwidth usage during inference. Generally faster, especially for repeated requests.

**`OLLAMA_KEEP_ALIVE=-1`**: Keeps the model loaded in VRAM indefinitely. Without this, Ollama may unload the model after a period of inactivity, incurring a reload penalty on the next request.

---

### Step 4 - Configure Elite Intel

Open the **Settings tab** in Elite Intel:

- Leave the **LLM Key** field blank (local Ollama does not require one).
- **LLM Address** defaults to `http://localhost:11434/api/chat`. If Ollama is on another machine, replace `localhost` with that machine's IP.
- **Command LLM**: set to `google/gemma-4-e4b` (or the name shown by `ollama ls`).
- **Query LLM**: set to `google/gemma-4-e4b` (or the name shown by `ollama ls`).
- Click **Stop** then **Start** on the AI tab to apply changes.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

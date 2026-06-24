## Local LLM - Linux Setup (LM Studio)

Running a local LLM keeps all data private and offline. There are no subscription fees. Hardware and electricity costs apply.

LM Studio is an alternative to Ollama. It uses the same models and the same OpenAI-compatible API. The choice can be changed in settings at any time.

It requires [LM Studio](https://lmstudio.ai) and a capable GPU.

---

### Minimum Hardware

To run Elite Dangerous and the LLM on the **same machine**, a minimum of an **NVIDIA RTX 3060 with 12 GB VRAM** is required. Performance headroom is limited at this specification.

> **Tip:** Elite Intel can be pointed at an LM Studio instance running on a **separate PC** on your network. If a second machine with a capable GPU is available, the game PC carries no inference load in this configuration.

---

### Recommended Model

| Model | VRAM Required  | Notes                  |
|---|----------------|------------------------|
| `tulu-3.1-8b-supernova` Q4_K_M | ~5 GB          | ✅ Recommended for V1.0 |
| `google/gemma-4-e4b` | ~6.3 GB        | ✅ Recommended for V1.1 |

> **Which model?** `tulu-3.1-8b-supernova` is the recommended model for **V1.0**. **V1.1** switches to `google/gemma-4-e4b`, which supports the function calling required by the new companion feature. The commands below use the V1.1 model — on V1.0, substitute `tulu-3.1-8b-supernova`.

---

[[youtube:2HGFmlZGK1g]]

---

### Step 1 - Install LM Studio

```shell
curl -fsSL https://lmstudio.ai/install.sh | bash
```

The installer drops everything into `~/.lmstudio/` and adds the `lms` CLI tool. After it finishes, add the CLI to your PATH:

```shell
# Add this to your ~/.bashrc
export PATH="$HOME/.lmstudio/bin:$PATH"
```

Then reload your shell:

```shell
source ~/.bashrc
```

Verify it worked:

```shell
lms --help
```

---

### Step 2 - Download the Model

For **V1.1**, download `google/gemma-4-e4b`:

```shell
lms get google/gemma-4-e4b
```

For **V1.0**, download `tulu-3.1-8b-supernova`:

```shell
lms get tulu3.1
Searching for models with the term tulu3.1
No exact match found. Please choose a model from the list below.

? Select a model to download
❯ QuantFactory/Tulu-3.1-8B-SuperNova-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-i1-GGUF
  QuantFactory/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-GGUF
  bunnycore/Tulu-3.1-8B-SuperNova-Smart-IQ4_XS-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-GGUF
  mradermacher/Tulu-3.1-8B-SuperNova-Smart-i1-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_0-GGUF
  matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF

↑↓ navigate • ⏎ select
```
Use the arrow keys to navigate and Enter to select. Select `matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF`.

To list downloaded models:

```shell
lms ls
```

That is the standard path. However, [LM Studio has a known bug](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). In some cases the download fails with:
```Error: No staff picks found with the specified search criteria.```

If that occurs, download the model manually:

```shell
curl -s "https://huggingface.co/api/models/matrixportalx/Tulu-3.1-8B-SuperNova-Q4_K_M-GGUF" | grep -o '"rfilename":"[^"]*\.gguf"'
```
Then import it:

```shell
lms import /path/to/tulu-3.1-8b-supernova-q4_k_m.gguf
```


---

### Step 3 - Start the Server

Load the model and start the inference server:

```shell
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start
```

`--gpu max` offloads inference to the GPU for maximum performance.

Verify it is running:

```shell
curl http://localhost:1234/v1/models
```

You should receive a JSON list of loaded models. The model ID string in that response is what you will enter in Elite Intel's **LLM Model** field.

To stop the server:

```shell
lms server stop
```

> ⚠️ **Important:** The LM Studio server does **not** survive reboots. Run `lms server start` again after each restart, or set up the optional auto-start below.

---

### Step 4 - (Optional) Auto-Start on Boot

To start LM Studio automatically, set it up as a **user** systemd service. This runs under your own session rather than as a system service. It starts after the desktop environment is up. No root access is required.

find your user id. (replace the user name with your actual user name)
```shell
id -u YOUR_USER_NAME
```

Remember this number. You will need it for the config later.

Create the user systemd directory if it does not exist:

```shell
mkdir -p ~/.config/systemd/user
```

Create the service file:

```shell
nano ~/.config/systemd/user/lmstudio.service
```

Paste this in:

```ini
[Unit]
Description=LM Studio Server
After=network.target

[Service]
Type=oneshot
RemainAfterExit=yes
Environment="HOME=/home/YOUR_USERNAME"
Environment="PATH=/home/YOUR_USERNAME/.lmstudio/bin:/usr/local/bin:/usr/bin:/bin"
Environment="XDG_RUNTIME_DIR=/run/user/YOUR_UID"
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192
ExecStart=/home/YOUR_USERNAME/.lmstudio/bin/lms server start --bind 0.0.0.0 --port 1234
ExecStop=/home/YOUR_USERNAME/.lmstudio/bin/lms server stop
ExecStopPost=/home/YOUR_USERNAME/.lmstudio/bin/lms daemon down

[Install]
WantedBy=default.target
```

Replace `YOUR_USERNAME` with your Linux username and `YOUR_UID` with your user ID. To find your UID:

```shell
id -u
```

> ⚠️ **Why `XDG_RUNTIME_DIR`?** User services run in a stripped-down environment that may not include session variables. LM Studio uses `XDG_RUNTIME_DIR` for IPC. Without it, the service can fail silently even when `lms` works correctly from the terminal. This is the most common cause of service failure when manual execution succeeds.

Enable and start it:

```shell
systemctl --user daemon-reload
systemctl --user enable lmstudio.service
systemctl --user start lmstudio.service
```

Verify it is running:

```shell
systemctl --user status lmstudio.service
curl http://localhost:1234/v1/models
```

> **Troubleshooting:** If the service fails, check the journal:
> ```shell
> journalctl --user -xeu lmstudio.service --no-pager | tail -40
> ```
> If it reports "Failed to load model", run `lms ls` and confirm the model name matches exactly what is in the service file.

---

### Step 4b - (Optional) Fix Slow Inference After Boot

Some users experience slow inference responses when LM Studio starts at boot. The issue resolves immediately after a manual service restart. This is caused by a quirk in LM Studio's daemon initialization. The first cold start may leave the inference runtime in a degraded state.

If slow responses appear after a reboot and resolve after a manual restart, this timer automates the fix.

Create a companion service:

```shell
nano ~/.config/systemd/user/lmstudio-restart.service
```

```ini
[Unit]
Description=LM Studio post-boot restart
After=lmstudio.service

[Service]
Type=oneshot
ExecStart=systemctl --user restart lmstudio.service
```

Create the timer:

```shell
nano ~/.config/systemd/user/lmstudio-restart.timer
```

```ini
[Unit]
Description=Restart LM Studio 2 minutes after login

[Timer]
OnBootSec=2min
Unit=lmstudio-restart.service

[Install]
WantedBy=timers.target
```

Enable the timer:

```shell
systemctl --user daemon-reload
systemctl --user enable --now lmstudio-restart.timer
```

The timer waits 2 minutes after login, restarts the LM Studio service once, and then stays inactive. If you do not experience slow inference, this step is not needed.

---

### Disable Ollama Auto-Start (if installed)

Ollama installs itself as an enabled systemd service by default. To run LM Studio instead and start Ollama only on demand:

```shell
sudo systemctl disable ollama.service
sudo systemctl stop ollama.service
```

---

### Step 5 - Configure Elite Intel

Open the **Settings tab** in Elite Intel:

- Leave the **LLM Key** field blank (local LM Studio does not require one).
- **LLM Address**: set to `http://localhost:1234/v1/chat/completions`. If LM Studio is on another machine, replace `localhost` with that machine's IP.
- **LLM Model**: paste in the model ID string from `curl http://localhost:1234/v1/models`.
- **Command LLM**: set to the same model ID.
- **Query LLM**: set to the same model ID.
- Click **Stop** then **Start** on the AI tab to apply changes.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

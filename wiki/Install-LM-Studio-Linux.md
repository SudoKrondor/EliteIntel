## Local LLM - Linux Setup (LM Studio)

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

Start the LM Studio background server, then download the model:

```shell
lms daemon up
lms get google/gemma-4-e4b
```

If the name does not resolve to an exact match, `lms get` lists the candidates it found and you
pick one with the arrow keys and Enter.

To list downloaded models:

```shell
lms ls
```

That is the standard path. However, [LM Studio has a known bug](https://github.com/lmstudio-ai/lmstudio-bug-tracker/issues/917). In some cases the download fails with:
```Error: No staff picks found with the specified search criteria.```

If that occurs, download the GGUF by hand from the model's page on HuggingFace, then import it:

```shell
lms import /path/to/the-model.gguf
```


---

### Step 3 - Start the Server

Load the model and start the inference server:

```shell
lms daemon up
lms load google/gemma-4-e4b --context-length 8192 --gpu max
lms server start --bind 0.0.0.0
```

`--gpu max` offloads inference to the GPU for maximum performance. `--bind 0.0.0.0` lets other PCs on your network reach the server, which by default answers only this one. `lms daemon up` does nothing if the background server is already running, so it is safe to include every time.

**NOTE**: `--context-length 8192` is required, and it must be exactly 8192: a smaller window makes the model hallucinate, and a larger one makes every answer too slow.

> **Let the gaming PC in.** If a firewall runs on this PC, allow TCP port 1234 from your gaming PC. With `ufw`,
> for example: `sudo ufw allow from GAMING_PC_IP to any port 1234 proto tcp`.

Verify it from the **gaming PC**, with the LLM PC's address in place of `LLM_PC_IP`:

```shell
curl http://LLM_PC_IP:1234/v1/models
```

You should receive a JSON list of loaded models. The model ID string in that response is what you will enter in Elite Intel's **Model** field.

To stop the server:

```shell
lms server stop
```

> ⚠️ **Important:** The LM Studio server does **not** survive reboots. Run the three Step 3 commands again after each restart, or set up the optional auto-start below.

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
ExecStartPre=/home/YOUR_USERNAME/.lmstudio/bin/lms load google/gemma-4-e4b --yes --context-length 8192 --gpu max
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

> **Network:** `--bind 0.0.0.0` lets your gaming PC reach the server. Allow TCP port 1234 through this PC's firewall, as in Step 3.

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

Some users experience slow inference responses when LM Studio starts at boot. The issue resolves immediately after a manual service restart. The cause is not confirmed.

First, check that the `lms load` line in your service file ends with `--gpu max`. Older copies of this page left it out. Without it, LM Studio decides for itself how much of the model goes on the GPU, and at boot it may put part of the model on the CPU. If you add it, run `systemctl --user daemon-reload` and reboot to test.

If slow responses still appear after a reboot and resolve after a manual restart, this timer automates the fix.

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
Description=Restart LM Studio 2 minutes after boot

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

The timer waits 2 minutes after boot, restarts the LM Studio service once, and then stays inactive. If you do not experience slow inference, this step is not needed.

---

### Step 5 - Configure Elite Intel

On the **gaming PC**, in Elite Intel, open **Services → AI Services**. Under **Language Model (LLM)**, select **LMStudio Setup**:

- **Address**: `http://LLM_PC_IP:1234/v1/chat/completions`, with `LLM_PC_IP` replaced by the LLM PC's address on your network (`ip addr` on the LLM PC shows it).
- **Model**: paste in the model ID string from `http://LLM_PC_IP:1234/v1/models`. One model serves both commands and queries.

No API key is needed for a local LLM.

Click **Save**. Elite Intel restarts the language model on its own.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈

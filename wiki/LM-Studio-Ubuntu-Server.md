# LM Studio on a Headless Ubuntu Server

> Guide is provided by **Wim**

> **Elite Intel:** this guide builds a dedicated LAN machine that runs LM Studio for Elite Intel. When it is done, set **Address** in the [Services Tab](UI-Settings-Tab) to `http://SERVER_IP:1234/v1/chat/completions` and load the supported model, **`google/gemma-4-e4b`**. The server must listen beyond localhost (`--bind 0.0.0.0`, see Part 2 § 9) and the firewall must allow the gaming PC.

---

# Part 1: Ubuntu Server for a Local AI / LM Studio Host

**Clean baseline installation guide**  
**Recommended baseline:** Ubuntu Server 24.04 LTS (amd64)  
**Document date:** 12 September 2026

## 1. Purpose

This guide prepares a clean Ubuntu Server machine for a headless local-AI workload such as LM Studio / llmster. It deliberately stops before installing LM Studio itself. The goal is a small, understandable server with working networking, SSH, updates, firewalling, and - when applicable - a validated NVIDIA GPU driver.

This is not a recovery or hardware-troubleshooting guide. It assumes the machine is stable at stock CPU/RAM settings and that important data has already been backed up.

## 2. Which Ubuntu version?

For a conservative, long-lived AI server, this guide uses **Ubuntu Server 24.04 LTS (Noble Numbat)**. Canonical provides standard security maintenance for Ubuntu 24.04 LTS through May 2029. Ubuntu 26.04 LTS is also available and is the current newer LTS, but 24.04 remains fully supported and is a mature baseline for systems where stability is more important than having the newest kernel and userspace.

For a brand-new deployment where newer hardware requires the latest kernel/driver stack, Ubuntu 26.04 LTS is also a reasonable choice. The commands in this guide are intentionally conventional and should remain very similar.

Official references:
- https://ubuntu.com/about/release-cycle
- https://documentation.ubuntu.com/release-notes/24.04/
- https://ubuntu.com/server/docs/tutorial/basic-installation/

## 3. Before installation

Have the following ready:

- A 64-bit Intel/AMD machine (amd64) for this guide.
- A USB stick containing the Ubuntu Server ISO.
- Wired Ethernet if possible.
- The intended hostname, username, and a strong password.
- A decision about which disk Ubuntu may erase.
- For GPU inference, a supported NVIDIA GPU installed and powered correctly.

If the machine previously ran overclocked CPU or RAM settings, return it to stable stock settings before commissioning it as a server. AI runtimes and kernel-module compilation can expose instability that lighter workloads do not.

## 4. Install Ubuntu Server

Boot the Ubuntu Server installer and follow the normal installer flow. For a simple dedicated server:

1. Select the language and keyboard layout.
2. Allow the installer to configure wired networking with DHCP initially.
3. Use the intended system disk. **Disk partitioning can destroy existing data**, so verify the selected disk carefully.
4. Create the administrator user and hostname.
5. Select **Install OpenSSH server** when offered. If it was not selected, it can be installed after the first boot.
6. Skip optional server snaps unless you specifically need them.
7. Complete the installation and reboot.

## 5. First login and system update

Log in locally or by SSH, then update the package index and installed packages:

```bash
sudo apt update
sudo apt upgrade -y
```

If Ubuntu reports that a reboot is required:

```bash
sudo reboot
```

After reconnecting, confirm the release and kernel:

```bash
lsb_release -a
uname -r
```

## 6. Install a small administration toolkit

The following packages are useful on a headless AI server:

```bash
sudo apt install -y \
  openssh-server \
  curl \
  ca-certificates \
  jq \
  htop \
  nvtop \
  ufw
```

`curl` is used by the LM Studio installer and API tests, `jq` makes JSON output readable, `htop` monitors CPU/RAM, `nvtop` monitors GPUs, and `ufw` provides a simple host firewall.

Confirm SSH is enabled:

```bash
systemctl status ssh --no-pager
```

## 7. SSH access

Find the server's current address:

```bash
ip -br address
```

From another machine on the LAN:

```bash
ssh YOUR_USER@SERVER_IP
```

For regular use, SSH public-key authentication is preferable to repeatedly entering a password. On the client machine:

```bash
ssh-keygen -t ed25519
ssh-copy-id YOUR_USER@SERVER_IP
```

Test a new SSH session before changing any password-authentication settings. Do not lock yourself out of a remote server.

Official OpenSSH reference:
https://ubuntu.com/server/docs/how-to/security/openssh-server/

## 8. Basic firewall

Before enabling UFW, allow SSH:

```bash
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status verbose
```

For a dedicated home/LAN AI server, it is better to expose application ports only to trusted clients or the trusted LAN rather than to every network interface indiscriminately.

LM Studio's default API port is 1234. **Do not open it yet.** Add the appropriate rule when LM Studio is installed and you know which clients should reach it.

Official firewall reference:
https://ubuntu.com/server/docs/how-to/security/firewalls/

## 9. NVIDIA GPU driver

Skip this section if the server will use CPU inference or a non-NVIDIA accelerator.

First update the system and reboot if necessary. Then let Ubuntu identify suitable NVIDIA compute drivers:

```bash
sudo ubuntu-drivers list --gpgpu
```

For a compute/server workload, Ubuntu documents automatic installation with:

```bash
sudo ubuntu-drivers install --gpgpu
```

Reboot after installation:

```bash
sudo reboot
```

Then validate the GPU:

```bash
nvidia-smi
```

A successful `nvidia-smi` should show the GPU, driver version, temperature, VRAM, and process table.

### Secure Boot note

Secure Boot requires compatible signed kernel modules. Ubuntu's `ubuntu-drivers` tooling is the preferred installation path because it is designed to select Ubuntu-packaged drivers appropriately. If the NVIDIA module does not load, check Secure Boot state before repeatedly reinstalling drivers:

```bash
mokutil --sb-state
```

Do not disable Secure Boot merely as a routine first step. If your particular firmware/driver combination has signing or MOK-enrollment problems, resolve that deliberately and document the BIOS change.

Official NVIDIA-on-Ubuntu reference:
https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

## 10. Final readiness check

Run:

```bash
hostnamectl
ip -br address
systemctl is-active ssh
sudo ufw status
free -h
df -h /
nvidia-smi    # NVIDIA systems only
```

At this point the machine should have:

- a supported Ubuntu LTS installation;
- current package updates;
- working LAN connectivity;
- working SSH access;
- a basic firewall;
- useful administration tools;
- a validated NVIDIA driver, if applicable.

The server is now ready for the LM Studio / llmster installation.

## 11. What is intentionally not covered

This baseline does not configure NAS/Samba storage, Docker, databases, web servers, RAID, custom disk layouts, VPNs, Pi-hole/DNS, desktop environments, CUDA Toolkit, or application-specific software. Those belong to separate deployment guides.

A standalone CUDA Toolkit is **not required merely to run LM Studio with a supported NVIDIA GPU**; use the runtime/driver stack LM Studio supports unless another application specifically requires the CUDA development toolkit.

## 12. Maintenance basics

Periodically apply normal updates:

```bash
sudo apt update
sudo apt upgrade
```

Check whether a reboot is pending:

```bash
if [ -f /run/reboot-required ]; then
  cat /run/reboot-required
fi
```

Before a major Ubuntu release upgrade, read that release's notes and back up the server. Major release upgrades are different from ordinary package updates.

---

## Official sources

- Ubuntu Server documentation: https://ubuntu.com/server/docs/
- Ubuntu release cycle: https://ubuntu.com/about/release-cycle
- Ubuntu 24.04 LTS release notes: https://documentation.ubuntu.com/release-notes/24.04/
- Basic installation: https://ubuntu.com/server/docs/tutorial/basic-installation/
- Package management: https://ubuntu.com/server/docs/how-to/software/package-management/
- OpenSSH: https://ubuntu.com/server/docs/how-to/security/openssh-server/
- UFW/firewall: https://ubuntu.com/server/docs/how-to/security/firewalls/
- NVIDIA drivers: https://ubuntu.com/server/docs/how-to/graphics/install-nvidia-drivers/

---

# Part 2: LM Studio Headless Server and Model Management

**Installation, model choice, quantization, API service, and model switching**  
**Target:** Linux/Ubuntu headless server  
**Document date:** 12 September 2026

## 1. Purpose

This guide starts with a clean Ubuntu server that already has working networking, SSH, and - for NVIDIA acceleration - a validated GPU driver. It installs LM Studio's headless runtime, explains how to choose and download models, exposes the local API when desired, and provides an interactive Bash model switcher.

LM Studio's server-native component is called **llmster**. The command-line program used to control it is **lms**. A graphical desktop is not required.

Official headless documentation:
https://lmstudio.ai/docs/developer/core/headless

## 2. Three names to understand

- **LM Studio** is the overall product and ecosystem.
- **llmster** is the headless/server daemon. It runs the inference service without a GUI.
- **lms** is the CLI used to download, load, unload, inspect, and serve models.

On a dedicated Linux inference server, llmster + lms is normally the cleanest arrangement.

## 3. Install llmster / lms

Install as the normal user who will run the service, not as root:

```bash
curl -fsSL https://lmstudio.ai/install.sh | bash
```

Start the daemon:

```bash
lms daemon up
```

Verify the CLI:

```bash
lms --version
lms --help
```

If the shell cannot find `lms`, start a new login shell or add LM Studio's binary directory to the current shell PATH:

```bash
export PATH="$HOME/.lmstudio/bin:$PATH"
```

To make that explicit for Bash:

```bash
echo 'export PATH="$HOME/.lmstudio/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

## 4. Find and download models

Running `lms get` without a model shows recommendations:

```bash
lms get
```

Download a model by its catalog name:

```bash
lms get publisher/model-name
```

For example, the exact model is deliberately left to the reader rather than hard-coded into this guide.

List downloaded models:

```bash
lms ls
```

Show downloaded variants/quantizations when available:

```bash
lms ls --variants
```

JSON output is useful for scripts:

```bash
lms ls --json | jq
```

LM Studio can download supported models from its catalog/Hugging Face ecosystem. The current CLI also supports specifying a quantization while downloading, for example:

```bash
lms get publisher/model-name@q4_k_m
```

Use the actual quantization names offered for that model; not every model is published in every format.

Official model-download documentation:
https://lmstudio.ai/docs/cli/local-models/get

## 5. Model size versus quantization - a beginner's explanation

A model contains billions of learned numerical values called **weights**. Higher numerical precision stores those values more faithfully but consumes more disk space and RAM/VRAM. **Quantization** compresses the model by representing those values with fewer bits.

A useful mental model is a high-quality audio recording: Q8 preserves more detail, Q6 compresses somewhat more, and Q4 compresses further. They are still versions of the same trained model; Q8 does not magically add new training or knowledge.

Typical interpretation:

| Quantization | Memory use | Fidelity | Practical role |
|---|---|---|---|
| Q4-class | Lower | Good | Excellent starting point; often enough for chat/tool use |
| Q5/Q6-class | Medium | Higher | Strong balance of quality and memory |
| Q8-class | Higher | Very high | Use when the extra fidelity measurably helps |

LM Studio's own guidance is to choose a 4-bit option or higher when the machine can support it.

### The important rule

**Do not choose Q8 simply because 8 is bigger than 4.** Choose the smallest quantization that performs your real workload reliably. Extra precision has value only if it improves the answers, reasoning, tool use, or stability enough to justify its larger permanent memory footprint.

## 6. Model size is not the same as model intelligence

Quantization and model size are different decisions.

A larger, more capable model at moderate quantization can outperform a smaller model at Q8 on difficult reasoning. Conversely, a small Q4 model may be ideal for fast command interpretation.

When selecting a model, consider:

1. **Task:** chat, coding, tool/function calling, vision, data analysis, embeddings, etc.
2. **Model architecture/parameter count:** larger models often have more capability but need more memory and may be slower.
3. **Quantization:** Q4/Q6/Q8 changes the fidelity/memory trade-off of that model.
4. **Context length:** larger context can consume additional memory.
5. **GPU fit:** keeping the model and working context in VRAM generally gives the best GPU inference experience.
6. **Tool-use support:** if an application requires native function/tool calling, choose a model trained and supported for it.

The correct choice is workload-specific. Test real prompts rather than relying only on benchmark scores.

## 7. Estimate memory before loading

LM Studio can estimate resource use without loading the model:

```bash
lms load publisher/model-name \
  --context-length 8192 \
  --gpu max \
  --estimate-only
```

This is an estimate, not a guarantee. Validate the actual loaded model with `lms ps`, `nvidia-smi`, and real inference.

## 8. Load and unload models

Load a downloaded model:

```bash
lms load publisher/model-name --context-length 8192 --gpu max
```

`--gpu max` asks LM Studio to offload as much as possible to the GPU. If omitted, LM Studio can choose automatically.

See what is loaded:

```bash
lms ps
```

Unload a model interactively or by key:

```bash
lms unload publisher/model-name
```

Unload everything:

```bash
lms unload --all
```

Official reference:
https://lmstudio.ai/docs/cli/local-models/load

## 9. Start the HTTP API

For access only from the server itself:

```bash
lms server start --bind 127.0.0.1 --port 1234
```

Verify:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

To listen on all IPv4 interfaces:

```bash
lms server start --bind 0.0.0.0 --port 1234
```

**Security:** binding to `0.0.0.0` exposes the service to reachable networks. LM Studio's documentation recommends authentication when exposing beyond localhost. At minimum, restrict port 1234 with UFW to trusted LAN clients or a trusted subnet. Do not expose an unauthenticated inference API directly to the public Internet.

Example for one trusted client:

```bash
sudo ufw allow from CLIENT_IP to any port 1234 proto tcp
```

Example for a trusted private subnet (replace it with your real subnet):

```bash
sudo ufw allow from 192.168.1.0/24 to any port 1234 proto tcp
```

Official server reference:
https://lmstudio.ai/docs/cli/serve/server-start

## 10. Test an inference request

The OpenAI-compatible API can be tested with `curl`:

```bash
curl -s http://127.0.0.1:1234/v1/chat/completions \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "publisher/model-name",
    "messages": [
      {"role": "user", "content": "Reply with exactly: Local inference works."}
    ],
    "temperature": 0
  }' | jq
```

Replace the model identifier with the identifier shown by `lms ps` or `/v1/models`.

## 11. Start LM Studio automatically at boot

LM Studio documents running llmster as a systemd startup task. Replace `YOUR_USER`, the model key, context length, and network binding with your own values.

Create the service:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Example:

```ini
[Unit]
Description=LM Studio Headless Server
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
User=YOUR_USER
Environment="HOME=/home/YOUR_USER"

ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load publisher/model-name --context-length 8192 --gpu max --yes
ExecStart=/home/YOUR_USER/.lmstudio/bin/lms server start --bind 127.0.0.1 --port 1234
ExecStop=/home/YOUR_USER/.lmstudio/bin/lms daemon down

[Install]
WantedBy=multi-user.target
```

Enable and start it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lmstudio.service
systemctl status lmstudio.service --no-pager
```

Then verify:

```bash
lms ps
curl -s http://127.0.0.1:1234/v1/models | jq
```

### If rapid CLI startup produces `Text file busy`

On some systems a second `lms` invocation immediately after `lms daemon up` may race with daemon startup. If you actually encounter that error, add a short delay between the two `ExecStartPre` lines:

```ini
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms daemon up
ExecStartPre=/bin/sleep 5
ExecStartPre=/home/YOUR_USER/.lmstudio/bin/lms load ...
```

Do not add workarounds pre-emptively when the simpler service works.

Official startup-task reference:
https://lmstudio.ai/docs/developer/core/headless_llmster

## 12. Interactive model switcher

The companion script `lmstudio-model-switcher.sh` (full listing at the end of this page) provides a simple numbered menu of downloaded **model keys**. It:

1. ensures llmster is running;
2. reads downloaded models from `lms ls --json`;
3. asks which model to use;
4. unloads the currently loaded model(s);
5. loads the selected model with a configurable context/GPU policy;
6. leaves/starts the API service as needed;
7. shows `lms ps` and checks `/v1/models`.

Install it:

```bash
mkdir -p ~/bin
cp lmstudio-model-switcher.sh ~/bin/
chmod +x ~/bin/lmstudio-model-switcher.sh
```

Run it with its conservative defaults (8192-token context, GPU max, localhost API):

```bash
~/bin/lmstudio-model-switcher.sh
```

Override settings for one invocation:

```bash
LMS_CONTEXT_LENGTH=16384 \
LMS_GPU=max \
LMS_BIND=0.0.0.0 \
LMS_PORT=1234 \
~/bin/lmstudio-model-switcher.sh
```

### Important: model variants / quantizations

LM Studio distinguishes a **logical model key** from its downloaded quantization variants. Downloading `publisher/model@q4_k_m` chooses a quantization to store, while loading commonly uses the logical model key reported by `lms ls`.

If you keep several quantizations of the same logical model, inspect them with:

```bash
lms ls --variants
lms ls --json | jq
```

Variant-selection behavior has changed across LM Studio CLI releases, so check `lms get --help` / `lms load --help` on the installed version before automating quantization switching. The supplied script intentionally switches **model keys**, which is the stable and understandable operation for a general-purpose guide.

## 13. Changing the boot-time default model

The interactive switcher changes what is loaded **now**. If the systemd service explicitly loads a model at boot, a reboot will load the model written in `/etc/systemd/system/lmstudio.service` again.

To make another model the permanent boot default:

```bash
sudo nano /etc/systemd/system/lmstudio.service
```

Change the `lms load ...` line, then:

```bash
sudo systemctl daemon-reload
sudo systemctl restart lmstudio.service
```

A more advanced deployment can use JIT model loading instead of pinning one model at boot. LM Studio supports JIT loading and automatic eviction; this is useful when applications choose models dynamically.

## 14. Monitor the server

Loaded models:

```bash
lms ps
```

GPU snapshot:

```bash
nvidia-smi
```

Refresh every two seconds:

```bash
watch -n 2 nvidia-smi
```

CPU/RAM:

```bash
htop
```

Interactive GPU monitor:

```bash
nvtop
```

LM Studio logs can also be streamed from the CLI:

```bash
lms log stream
```

## 15. A practical model-selection workflow

A sensible way to choose a model is:

1. Define the real workload first.
2. Identify models with the required capability (for example tool use, coding, vision, or reasoning).
3. Download a Q4-class variant first when available.
4. Use `--estimate-only` before loading if memory is tight.
5. Test real prompts and tool calls.
6. Move to Q5/Q6 only if Q4 loses meaningful reliability or quality.
7. Move to Q8 only if the improvement is visible and worth the extra RAM/VRAM.
8. If even Q8 of a small model is not capable enough, test a **larger/better model at moderate quantization** rather than assuming still more precision will solve the problem.

This keeps model choice empirical rather than numerical: **the best model is the smallest/fastest configuration that reliably meets the application's needs.**

## 16. Troubleshooting checklist

If LM Studio does not behave as expected, check in this order:

```bash
lms --version
lms daemon up
lms ls
lms ps
lms server status
nvidia-smi
free -h
```

For a model that may not fit, estimate first:

```bash
lms load publisher/model-name --context-length 8192 --gpu max --estimate-only
```

For a remote client that cannot connect, verify locally first:

```bash
curl -s http://127.0.0.1:1234/v1/models | jq
```

Then verify the server is listening on the intended address and port:

```bash
ss -ltnp | grep 1234
sudo ufw status numbered
```

If local access works but remote access does not, investigate bind address, firewall rules, routing, and API authentication before changing the model.

## 17. What this guide intentionally leaves open

It does not prescribe one universal model. Hardware, workload, context length, model architecture, and required capabilities differ too much for that. It also does not require a separate CUDA Toolkit, Docker, a desktop GUI, or application-specific integrations.

Once the server is stable, application-specific guides can define their own model, context, API endpoint, authentication, and benchmark requirements.

---

## Official sources

- LM Studio CLI: https://lmstudio.ai/docs/cli
- Headless / llmster: https://lmstudio.ai/docs/developer/core/headless
- Linux startup task: https://lmstudio.ai/docs/developer/core/headless_llmster
- Download models (`lms get`): https://lmstudio.ai/docs/cli/local-models/get
- Load/unload models (`lms load`): https://lmstudio.ai/docs/cli/local-models/load
- Start API server: https://lmstudio.ai/docs/cli/serve/server-start
- Model download/quantization overview: https://lmstudio.ai/docs/app/basics/download-model
- REST API quickstart: https://lmstudio.ai/docs/developer/rest/quickstart

---

## Appendix: `lmstudio-model-switcher.sh`

Save this as `lmstudio-model-switcher.sh`, then install it as shown in Part 2 § 12.

```bash
#!/usr/bin/env bash
set -euo pipefail

# LM Studio interactive model switcher
# Optional environment overrides:
#   LMS_CONTEXT_LENGTH=16384 LMS_GPU=max LMS_BIND=0.0.0.0 LMS_PORT=1234 ./lmstudio-model-switcher.sh

CONTEXT_LENGTH="${LMS_CONTEXT_LENGTH:-8192}"
GPU="${LMS_GPU:-max}"
BIND="${LMS_BIND:-127.0.0.1}"
PORT="${LMS_PORT:-1234}"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Error: '$1' is required but was not found in PATH." >&2
    exit 1
  }
}

need lms
need jq

# Make sure the headless daemon is available.
lms daemon up >/dev/null

mapfile -t MODELS < <(
  lms ls --json | jq -r '
    if type == "array" then .[]
    elif has("models") then .models[]
    else empty end
    | .modelKey // .model_key // empty
  ' | awk 'NF' | sort -u
)

if ((${#MODELS[@]} == 0)); then
  echo "No downloaded models were found."
  echo "Download one first, for example: lms get <publisher/model>"
  exit 1
fi

echo
echo "Downloaded LM Studio models"
echo "---------------------------"
for i in "${!MODELS[@]}"; do
  printf '  %2d) %s\n' "$((i + 1))" "${MODELS[$i]}"
done
echo "   0) Cancel"
echo

while true; do
  read -r -p "Choose a model [0-${#MODELS[@]}]: " CHOICE
  [[ "$CHOICE" =~ ^[0-9]+$ ]] || { echo "Please enter a number."; continue; }
  (( CHOICE == 0 )) && exit 0
  (( CHOICE >= 1 && CHOICE <= ${#MODELS[@]} )) && break
  echo "Choice out of range."
done

MODEL="${MODELS[$((CHOICE - 1))]}"

echo
echo "Selected: $MODEL"
echo "Context : $CONTEXT_LENGTH"
echo "GPU     : $GPU"
echo

read -r -p "Unload the current model and load this one? [y/N]: " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 0; }

echo "Unloading currently loaded models..."
lms unload --all || true

echo "Loading $MODEL..."
lms load "$MODEL" --context-length "$CONTEXT_LENGTH" --gpu "$GPU" --yes

# Start the HTTP server only if it is not already running.
if ! lms server status >/dev/null 2>&1; then
  echo "Starting LM Studio server on $BIND:$PORT..."
  lms server start --bind "$BIND" --port "$PORT"
fi

echo
echo "Current LM Studio state:"
lms ps

echo
echo "API model list:"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://127.0.0.1:${PORT}/v1/models" | jq . || \
    echo "API check failed. If the server uses another port, set LMS_PORT accordingly."
else
  echo "curl is not installed; skipping API check."
fi
```

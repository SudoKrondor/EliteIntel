# Running LLMs on AMD RX 7800 XT (ROCm Guide)

> Guide is provided by **Ian Wirtz**

> **Recommended:** LM Studio (`lms`) tends to give the best results, but Ollama is a viable alternative.

---

## Prerequisites

### Step 1 - Install `rocm-hip-runtime`

Before LM Studio or Ollama can use your GPU via ROCm, the system needs the base user-space HIP libraries to communicate with the kernel driver.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-hip-runtime
```

**Ubuntu / Debian:**
```bash
sudo apt install rocm-hip-runtime
```

**Fedora:**
```bash
sudo dnf install rocm-hip-runtime
```

> **GPU access permissions:** Your user must belong to the `render` and `video` groups. Check with:
> ```bash
> groups
> ```
> If either group is missing, add yourself:
> ```bash
> sudo usermod -aG video,render $USER
> ```
> You must **fully log out and back in** (or reboot) for the group change to take effect.

---

### Step 2 - Install `rocm-smi` *(may be optional)*

On fast-moving distributions like Arch, the management tools are not always pulled in as a strict dependency of `rocm-hip-runtime`. Install them explicitly to avoid library version mismatches.

**Arch Linux / CachyOS:**
```bash
sudo pacman -S rocm-smi-lib
```

**Ubuntu / Debian:**

Debian splits the CLI tool and its runtime library into separate packages.

```bash
# Command-line monitoring tool
sudo apt update && sudo apt install rocm-smi

# Runtime libraries
sudo apt update && sudo apt install librocm-smi64-1
```

> **Tip:** If the exact package name is uncertain, type `sudo apt install librocm-smi64` and press **Tab** to autocomplete the current version suffix.

**Fedora:**
```bash
# CLI tool
sudo dnf install rocm-smi

# C/C++ development libraries and headers (equivalent to rocm-smi-lib on Arch)
sudo dnf install rocm-smi-devel
```

---

## Running a Model

### Step 3 - Load a Model with ROCm Acceleration

When invoking `lms load`, pass the hardware acceleration flags explicitly. The `--gpu max` flag instructs the runtime to load the entire model into VRAM.

```bash
HSA_OVERRIDE_GFX_VERSION=11.0.0 lms load tulu-3.1-8b-supernova --context-length 8192 --gpu max
```

The `HSA_OVERRIDE_GFX_VERSION=11.0.0` prefix tells the ROCm stack to treat the RX 7800 XT as a natively supported compute target, bypassing silent fallback failures.

---

### Step 4 - Make the Configuration Permanent

To avoid prefixing every command with the environment variable, add it to your shell profile.

**Bash:**
```bash
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc
source ~/.bashrc
```

**Fish (CachyOS default) - Option A: Universal Variable (recommended)**

Set it once; Fish persists it automatically across reboots with no further configuration needed:
```fish
set -Ux HSA_OVERRIDE_GFX_VERSION 11.0.0
```

**Fish - Option B: Explicit config file entry**
```fish
echo 'set -gx HSA_OVERRIDE_GFX_VERSION 11.0.0' >> ~/.config/fish/config.fish
source ~/.config/fish/config.fish
```

**Ollama (systemd service):**

Because Ollama runs under its own `ollama` system user, the variable must be injected via a systemd drop-in:

```bash
sudo mkdir -p /etc/systemd/system/ollama.service.d
sudo nano /etc/systemd/system/ollama.service.d/override.conf
```

Paste the following, then save and exit (`Ctrl+O`, `Enter`, `Ctrl+X`):

```ini
[Service]
Environment="HSA_OVERRIDE_GFX_VERSION=11.0.0"
```

Then reload and restart the service:
```bash
sudo systemctl daemon-reload
sudo systemctl restart ollama
```

---

## Verification

### Step 5 - Confirm the Kernel Compute Driver is Loaded

If a command hangs, the kernel compute layer (`amdkfd`) may not be initialised. Check whether the system exposes your GPU as a ROCm compute platform:

```bash
rocminfo
```

Scroll to the top of the output. If you see `Can't open /dev/kfd` or a crash, the Linux kernel is not exposing the compute interface to user space. If you are running a custom or bleeding-edge kernel, try booting into the stock or LTS kernel (`linux-lts`) to rule out a driver regression.

---

### Step 6 - Start the Server and Verify VRAM Usage

**LM Studio:**
```bash
lms server start
```

**Ollama:**
```bash
ollama serve
```

Then confirm the model is loaded into VRAM:
```bash
rocm-smi
```

**Idle (no model loaded):**

![rocm-smi output with no model running](images/rocm-smi-without-game-running-example.png)

At idle the GPU draws minimal power (~9W), clocks are near-floor, and VRAM usage is low (~44%).

**Under load (model + game running simultaneously):**

![rocm-smi output with Elite Dangerous and EliteIntel running](images/rocm-smi-with-game-and-Elite-Intel-running-example.png)

Under combined load you should see VRAM usage climb significantly (71% in this example), GPU utilisation rise, and power draw increase accordingly (~147W). This confirms the model is resident in VRAM and inference is running on the GPU.

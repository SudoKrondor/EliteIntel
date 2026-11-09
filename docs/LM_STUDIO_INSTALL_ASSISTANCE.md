# LM Studio Install Assistance

**Status:** spec for V1.2 beta, branch `V1.2-LMStudio-Install-Assistance`. Written 2026-11-09. The website
guides are updated separately, once beta testers have confirmed the feature works.

One button on **AI Services → LMStudio Setup** installs LM Studio, downloads and loads `google/gemma-4-e4b`,
and fills in **Address** and **Model**, for a commander whose PC can run the game and the model together. It
is an onboarding convenience. The brain and the VEGA pipeline do not change: once the fields are filled, the
existing LM Studio path is used exactly as it is today.

---

## Who sees the button

**Game and model on the same PC need an RTX 3090 (24 GB) or better** (decided 2026-11-08). The game takes
8.5 GB or more and the model about 6 GB, and browsers, OBS and Windows itself also use the GPU. 18 GB does not
hold up in practice, especially on Windows. A 12 GB card is enough only for a PC that runs nothing but the
model, which is the manual second-PC setup in the website guide.

The check reads `nvidia-smi --query-gpu=name,memory.total,compute_cap --format=csv,noheader,nounits`. It
passes when **any one card** reports compute capability **8.6 or higher** and **at least 23,000 MiB** of
memory. A 24 GB card reports a little under 24,576 MiB.

| Card | Result | Why |
|---|---|---|
| RTX 3090 / 3090 Ti / 4090 / 5090, A5000 and up | pass | cc ≥ 8.6, ≥ 24 GB |
| Titan RTX (cc 7.5), Tesla P40 / M40, Quadro P6000 | fail | too old or too slow, though they have 24 GB |
| Any 12 or 16 GB card | fail | game and model do not fit together |
| AMD and Intel, including the 24 GB RX 7900 XTX | fail | `nvidia-smi` cannot see them. Not in this release |
| No `nvidia-smi`, or it hangs (5 s limit) | fail | |

OSHI is not used for this: its VRAM figure is wrong on both platforms (see `HardwareReport.gpus`).

When the check fails, the button is **not shown**. The manual guides remain the path for everyone else.

---

## What the button says

The panel works out what is already on the PC before it shows anything. The rows are checked in order, and the
first that matches decides.

| What the app finds | Button | What it does |
|---|---|---|
| The app's own install, setup unfinished | **Continue setup** | Resumes at the first unfinished step |
| The app's own install, setup finished | **Uninstall LM Studio** | [Uninstall](#uninstall) |
| Card fails the check | *(hidden)* | |
| **Address** points to another machine | *(hidden)* | The commander runs LM Studio elsewhere |
| No LM Studio anywhere | **Install LM Studio** | Full install ([below](#the-install)) |
| LM Studio found that the app did not install | **Set up LM Studio** | Model and fields only. Never installs, manages or uninstalls it |
| LM Studio found that the app did not install, model loaded and fields already right | *(hidden)* | Nothing to do |

**"The app's own install"** means an [install record](#the-install-record) whose folder still holds that install's
marker. It comes before the GPU and Address checks, so an install the app made can always be finished or
removed, even after the card was swapped or the Address changed. A record whose folder has no matching marker is
ignored, not forgotten: a drive that is only unplugged today must still be uninstallable tomorrow.

**"LM Studio found"** means an `lms` binary at `<LM Studio home>/bin/lms(.exe)`. The home is the folder named
in `~/.lmstudio-home-pointer` if that file exists, else `~/.lmstudio`. The desktop app and llmster both put
`lms` there. If no `lms` is found but something answers on the Address, the button is hidden. That
LM Studio is one the app cannot drive.

---

## The install

A modal dialog, `LmStudioInstallDialog`, opened by the button. Each step has a status badge: waiting, running,
done, already done, failed or stopped. The download also gets a progress bar. Everything the steps print goes to
a collapsible details log, and the same lines go to the app log, so they are in a support bundle. The dialog grows
to fit as the download, a result or the log appear. It has no scrolling body.

| # | Step | What happens | Progress shown |
|---|---|---|---|
| 1 | **Check** | Free space on the chosen drive must be at least **10 GB**: LM Studio is 2.4 GB on Windows and 3.4 GB on Linux, and the model is 6 GB | badge |
| 2 | **Install LM Studio** | Creates the home and writes the [install record and its marker](#the-install-record), writes the pointer if a folder was chosen, then downloads LM Studio's official script and runs it with `LMS_NO_MODIFY_PATH=1` | badge, log |
| 3 | **Start LM Studio** | `lms daemon up`, then `lms server start --port 1234 --bind 127.0.0.1` | badge |
| 4 | **Download the model** | `POST /api/v1/models/download` for `google/gemma-4-e4b`, then polls its status once a second | badge and a bar: *"2.1 of 6.0 GB · 48 MB/s · 1 min left"* |
| 5 | **Load the model** | `lms load google/gemma-4-e4b --context-length 8192 --gpu max --yes`. A model already loaded is unloaded first: its settings may not be these, and step 6 must measure this load | badge |
| 6 | **Check the GPU took it** | `nvidia-smi` memory in use must have risen by **at least 4 GB** since before the load | badge |
| 7 | **Test** | One tool-call request in the shape `LmStudioLlmAdapter` sends. A tool call must come back | badge |
| 8 | **Save** | Address `http://localhost:1234/v1/chat/completions`, Model `google/gemma-4-e4b`, saved through the panel's existing `save()`, which restarts only the brain | badge |

**The install folder.** The dialog shows where LM Studio will go and that drive's free space, with
**Change…**.
- **Default:** `~/.lmstudio`, LM Studio's own default. No pointer is written.
- **A chosen folder:** the app creates **its own** `LMStudio` subfolder inside it and points
  `~/.lmstudio-home-pointer` there. Uninstall then deletes only the folder the app made, never the commander's
  chosen parent, which might be `D:\` or a whole games drive.

**The install script** is LM Studio's own, from `https://lmstudio.ai/install.ps1` (Windows) or
`https://lmstudio.ai/install.sh` (Linux). It verifies the archive's SHA-512 itself. Windows runs it with
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File`, and Linux with `bash` (the script needs `curl` or
`wget`). The dialog tells the commander that LM Studio is third-party software, and names its install folder
and size before anything is downloaded.

**Why the CLI loads the model, not REST:** the REST load has no GPU-offload setting, and `--gpu max` is what
the guides have always used. REST does the download, because only REST reports progress.

**A failed step** stops the install. The dialog names the step and shows its log, and the install record keeps
what was done, so the button then says **Continue setup**.

**Every step can run again.** A step whose work is already done is skipped: **Continue setup** skips the space
check, skips the script when `lms` is already in the home, skips the start when the server answers, and skips the
download when LM Studio says the model is already downloaded. Continuing is simply running the install again.

**A download that stops moving** (no change in `downloaded_bytes` for 5 minutes) fails the step.

**Closing the dialog during the download** asks first. For the app's own install it then shuts the daemon down:
LM Studio marks the job `paused` and keeps the `downloading_*.part` files, and asking for the model again resumes
where it stopped (smoke test round 2). An LM Studio the app did not install is not stopped, so its download carries
on.

### "Set up LM Studio" (one the app did not install)

Only steps 4 to 8, against the commander's own LM Studio. If its server is not answering, the dialog runs
`lms daemon up` and `lms server start` once, as the commander would by hand, and nothing is set to start at
login. No install record is written, so the app never starts, stops or uninstalls that LM Studio.

---

## The install record

The table `lm_studio_install` (`12022__lm_studio_install.sql`), read through `LmStudioInstallDao` and
`LmStudioInstallManager`, on the pattern of `bindforge_settings`. At most one row, and no row means the app
installed nothing. It sits in the **shared** database, because it describes the PC, not a commander.

| Column | Meaning |
|---|---|
| `install_id` | A random id, also written into the marker file |
| `home_dir` | The LM Studio home the app installed into |
| `pointer_written` | Whether the app wrote `~/.lmstudio-home-pointer` (so uninstall removes it) |
| `setup_complete` | Whether all eight steps finished |
| `installed_at` | When the install started |

**The marker** (`InstallMarker`) is a file named `.elite-intel-install` that the app writes into `home_dir` when
the install starts. It holds `install_id`, and **the record counts only while that file holds the same id.**
- **Why:** a folder check alone cannot tell this PC's LM Studio from another one at the same path. A backup
  restored onto a PC with the same user name names a `.lmstudio` folder that exists there too, and an uninstall
  driven by that record would delete an LM Studio the commander installed themselves.

**Restoring a backup** (`AppDataRestore`) deletes the row unless its folder on this PC holds the matching marker,
alongside the existing forgetting of the old computer's journal and bindings folders.

**Starting a fresh install over a stale record:** a record whose marker no longer matches is replaced, since
there is one record at most.

---

## Starting and stopping

**Only for an install the app owns** (an install record with `setup_complete`), decided 2026-11-09, and only
while the brain runs on it: the local LLM is selected and the Address is on this PC.

A service of its own, `LOCAL_LLM_HOST` (`LmStudioHost`), is registered **before `VEGA`**, so it starts first and
stops last. Its creator declines with `null` when there is nothing for it to run, and the registry then starts
nothing.
- **Start:** `lms daemon up` and `lms server start` unless the server already answers, then `lms load …` with
  the step 5 flags unless the model is already loaded **with a context window of exactly 8192**. A model loaded
  with any other window, or one LM Studio does not report, is unloaded and loaded again: 8192 was found by
  trial and tested on Linux and Windows, and VEGA hallucinates on a smaller window and runs too slowly on a larger
  one. The server itself takes no context setting. The window belongs to the loaded model. It returns once the model is loaded, so the brain's
  connection check finds it answering. A failure is logged to the app log, not thrown: the other services carry
  on, and the brain's connection check says LM Studio is not answering.
- **Stop:** `lms unload --all`, `lms server stop`, `lms daemon down`, each tried even if one before it failed.
  The model's VRAM is then free whenever the app's services are stopped.
- **App exit:** the app exits without stopping its services, so a running host also leaves a shutdown hook that
  stops LM Studio the same way.
- **Brain restart:** a change to the LLM settings restarts the brain, and the host is brought in line first. It
  starts when the settings now call for it, which is how an install that just finished gets its host, and stops
  when they no longer do, such as a switch to the cloud. It is left alone when nothing changed for it, because a
  restart reloads the model.

Nothing is set to start at login.

---

## Uninstall

Decided 2026-11-09: **uninstall removes everything in the install the app created, including models the
commander added later.** Before anything is deleted, a confirmation names the folder and lists every model file
under its `models` folder, largest first, with sizes.

`LmStudioUninstaller` does it off the event thread while the button says it is busy, then a message says how it
ended. The steps:

1. **Guard.** Nothing happens unless the home holds this install's marker, and the home is neither the user's home
   folder, a folder above it, nor a drive root.
2. **Stop.** The `LOCAL_LLM_HOST` service first (`StopLocalLlmHostEvent`, handled on the publishing thread so it
   has stopped when it returns). Then, whoever started LM Studio (the service, or an install that ran with the
   services stopped): `lms unload --all` and `lms server stop` if the server answers, and `lms daemon down`.
   Each step is tried even if one before it failed.
3. **Delete `home_dir`** (`FileTrees.delete`: deepest first, links not followed, Windows read-only marks
   cleared). Tried three times, two seconds apart, because a process that has just exited can hold its files
   for a moment on Windows. If it still fails, the uninstall stops there and the record is kept, so the button
   still offers **Uninstall LM Studio**.
4. **Delete `~/.lmstudio-home-pointer`** if `pointer_written` and it still names this home. A pointer the
   commander has since changed is left alone.
5. **Windows only:** remove `<home_dir>\bin` from the **user** PATH (`HKEY_CURRENT_USER\Environment`), leaving
   every other entry as it was and writing the value back with the type it had. The first `lms daemon up` adds it
   even with `LMS_NO_MODIFY_PATH=1` (smoke test round 3). A failure here is reported in the app log but does not
   fail the uninstall: the entry would name a folder that no longer exists, which Windows skips. On Linux no
   shell profile is touched.
6. **Delete the install record.**
7. **Clear Model and save**, which restarts the brain. `SetupCheck` then says no model is configured.

Smoke test round 3 found no processes and no files left after these steps, on either OS.

---

## Sources

Every field, control and figure the feature shows, and where it comes from.

| Shown | Where | Source |
|---|---|---|
| Button visible or hidden | Panel | GPU check (`nvidia-smi`), the Address field, the `lms` binary, the install record |
| Button caption | Panel | The table under [What the button says](#what-the-button-says) |
| Install folder | Dialog | Default `~/.lmstudio`, or the commander's choice + `/LMStudio` |
| Free space on that drive | Dialog | `FileStore.getUsableSpace()` of the folder's volume |
| Space needed (10 GB) | Dialog | Constant, from smoke test sizes (2.4 to 3.4 GB + 6 GB + margin) |
| Step checklist and current step | Dialog | The installer's own progress, reported per step |
| Script and command output | Dialog log | stdout/stderr of the script and each `lms` command |
| Download bar, GB done / total, speed, time left | Dialog | `GET /api/v1/models/download/status/{job_id}`: `downloaded_bytes`, `total_size_bytes`, `bytes_per_second`, `estimated_completion` |
| "The GPU took the model" | Dialog | `nvidia-smi --query-gpu=memory.used` before and after the load |
| Test result | Dialog | Whether the test request's reply contains a tool call |
| Models listed before uninstall | Confirm dialog | `*.gguf` files under `home_dir/models`, with sizes from the file system |
| Address and Model after setup | Panel | Constants: `http://localhost:1234/v1/chat/completions`, `google/gemma-4-e4b` (the panel's existing defaults) |

---

## Build slices

1. **GPU check and button visibility.** `nvidia-smi` reading shared with `HardwareReport`, LM Studio
   detection, the button and its captions. No install yet.
2. **Install record.** Migration, DAO, manager, the marker, and restore forgetting a foreign record.
3. **Installer.** `LmStudioInstaller` drives an `LmStudioMachine` (the real one is `LocalLmStudioMachine`: the
   script, `lms`, the REST server, `nvidia-smi`) and reports to an `InstallProgress`. It runs without UI. Tests
   cover the step order, skips, failures and cancel against a fake machine, and the REST and console parsing
   against the answers the smoke tests recorded.
4. **Dialog.** Step badges, download bar, details log, folder chooser, the ask-before-stopping prompt.
5. **Starting and stopping.** The `LOCAL_LLM_HOST` service, its exit hook, and matching it to the settings on a
   brain restart.
6. **Uninstall.** The confirmation, `LmStudioUninstaller` and its seven steps.

Every new UI string goes into all nine `gui*.properties` bundles.

## Not in this release

- AMD and Intel GPUs
- Starting LM Studio at login
- Choosing a different model
- The website guides (a separate task once beta testing confirms the feature)

## Still untested on real hardware

The smoke tests ran on GitHub's GPU-less runners. These need the V1.2 beta:
- `--gpu max` actually placing the model on the GPU
- VRAM use next to the game
- The Windows install and uninstall on a commander's own PC

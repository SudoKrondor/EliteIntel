# BindForge — Feature Specification

**Project:** Elite Intel
**Feature:** BindForge
**Version:** 1.0 Draft
**Status:** In Progress

---

## 1. Overview

BindForge is a feature of Elite Intel that protects Elite Dangerous controller configuration files from loss and provides tools to view, edit, and manage those files.

### The Core Problem

Elite Dangerous stores controller configuration files in two distinct locations:

- The `.binds` file lives in the user's config folder. This is the single most critical file — it contains every game action to input assignment. Loss or corruption means the ship does not fly correctly. The most common cause of loss is the player themselves — accidental deletion, overwriting the wrong file, or a misconfigured edit. Game updates can occasionally erase or overwrite it but this is rare.
- `DeviceMappings.xml` and all `.buttonMap` files live inside the **game installation folder**. These files are cosmetic only — they control how device names and button labels appear in the game UI. Loss of these files does not affect gameplay but does mean the player loses their custom labeling work. Game updates are more likely to touch these files than the `.binds` file but loss is still not guaranteed on every update.

Players who have invested significant time crafting complex multi-device HOTAS configurations can lose their `.binds` file through their own actions or occasionally through a game update. BindForge exists to make that loss recoverable.

### Purpose — In Priority Order

1. **Backup and restore** all controller configuration files safely and reliably
2. **View and edit** bindings within Elite Intel
3. **Manage** device mappings and button names

### What BindForge Is Not

BindForge is not a game launcher or a live injection tool. It does not interact with the running game process. It works exclusively with the configuration files Elite Dangerous reads at startup.

### Relationship to Existing Elite Intel Bindings Work

Elite Intel already contains a bindings viewer and keyboard binding editor built by the Elite Intel team. BindForge absorbs this existing work entirely — it does not live alongside it. The existing bindings UI becomes BindForge's Bind Editor section. BindForge adds backup and restore, dormant mode, device mapping management, profile switching, and completes the Purpose Mode and Search tabs that are currently placeholders in the existing UI.

The existing UI structure — three mode tabs (Game Mode, Purpose Mode, Search), five context buttons (All Controls, General Controls, Ship Controls, SRV Controls, On Foot Controls), and the Binding Editor right panel — was designed from the original BindForge screenshots and is already in place. BindForge completes and extends it.

### Platform Support

BindForge runs on both Windows and Linux. Linux players run Elite Dangerous exclusively through Steam and Proton — there is effectively one game installation on Linux. Windows players may have multiple installations across different stores. Where behavior differs between platforms it is explicitly noted.

### UI Location

BindForge is a top-level tab in Elite Intel's main navigation bar, as defined in KAN-52. Within the BindForge tab there are four sections accessible from a top bar: Device Mapping, Bind Editor, Preset Manager, and Backup.

---

## 2. Managed File Domains

BindForge operates on four distinct file domains. Each is independently managed but logically related.

### 2.1 Bind Domain

| Property | Value |
|---|---|
| File type | `.binds` (XML) |
| Location — Windows | `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\` |
| Location — Linux | `~/.local/share/Steam/steamapps/compatdata/359320/pfx/drive_c/users/steamuser/AppData/Local/Frontier Developments/Elite Dangerous/Options/Bindings/` |
| Vulnerability | Low from game updates — higher risk from player action |
| Content | All game action to input assignments |
| Priority | **Critical — loss means the ship does not fly correctly** |

Multiple `.binds` files may exist simultaneously. Elite Dangerous presents all `.binds` files it finds in the bindings folder as selectable presets in its own controls menu. Elite Intel's existing `BindingsLoader` and `BindingsMonitor` already resolve and watch this folder — BindForge builds on top of that infrastructure.

### 2.2 Device Identity Domain

| Property | Value |
|---|---|
| File type | `DeviceMappings.xml` (XML) |
| Location — Windows | `[GameInstall]\Products\elite-dangerous-odyssey-64\ControlSchemes\` |
| Location — Linux | `[SteamLibrary]\steamapps\common\Elite Dangerous\Products\elite-dangerous-odyssey-64\ControlSchemes\` |
| Vulnerability | Low to moderate — occasionally touched by game updates |
| Content | Device element names mapped to VID and PID values |
| Priority | **Cosmetic — loss does not affect gameplay** |

One `DeviceMappings.xml` exists per game installation. On Windows there may be multiple installations across Steam, Epic, and Frontier Direct. On Linux there is one Steam installation.

### 2.3 Button Map Domain

| Property | Value |
|---|---|
| File type | `.buttonMap` (XML) |
| Location — Windows | `[GameInstall]\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceButtonMaps\` |
| Location — Linux | `[SteamLibrary]\steamapps\common\Elite Dangerous\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceButtonMaps\` |
| Vulnerability | Low to moderate — occasionally touched by game updates |
| Content | Friendly button and axis names per device, used for display in the game UI |
| Priority | **Cosmetic — loss does not affect gameplay** |

One `.buttonMap` file exists per named device. The filename matches the device element name in `DeviceMappings.xml`. Frontier ships a small number of default `.buttonMap` files with the game. Players add their own for custom devices.

### 2.4 Active Preset Domain

| Property | Value |
|---|---|
| File type | `StartPreset.#.start` (plain text) |
| Location — Windows | `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\` |
| Location — Linux | Same as Bind Domain |
| Vulnerability | Low — not touched by game updates but must stay consistent with the active `.binds` files |
| Content | Four lines, one per binding section, each pointing to an active preset name |
| Priority | **Critical — if inconsistent with the active `.binds` files the game loads the wrong presets** |

The four binding sections in order are: General Controls (line 1), Ship Controls (line 2), SRV Controls (line 3), On Foot Controls (line 4). The `#` in the filename is the binding schema major version number.

Each line is independent. All four lines may point to the same `.binds` file — which is the most common configuration — or each line may point to a different `.binds` file. For example a player could have separate `GeneralControls.binds`, `ShipControls.binds`, `SRVControls.binds`, and `OnFootControls.binds` files, each loaded only for its corresponding section. This is an unusual configuration but is fully supported by the game and therefore must be fully supported by BindForge.

**BindForge must handle both cases:**

- **Single preset** — all four lines point to the same `.binds` file. This is the common case.
- **Split preset** — each line points to a different `.binds` file. BindForge treats each line independently.

**The Active Preset Domain is managed in BindForge's dedicated Preset Manager tab.** The Preset Manager shows all four binding sections with a dropdown for each line listing all available `.binds` files in the bindings folder. The user can point each section at any available `.binds` file independently. This is the only place in BindForge where the `.start` file is directly editable.

Whenever BindForge restores a backup it restores the `.start` file as part of the backup package, preserving whatever per-section configuration was in place at backup time. Elite Intel's existing `BindingsLoader` reads this file to determine which `.binds` file is currently active — BindForge builds on top of that rather than replacing it.

---

## 3. Data Integrity Principle

Every file operation BindForge performs either completes fully and correctly, or fails cleanly. There is no partially saved state.

On failure BindForge:
- Rolls back completely
- Leaves all existing files untouched
- Reports the exact reason for failure to the user

This applies to every write operation — backup creation, restore, binding edits, device mapping edits, preset manager changes. Partial success is not acceptable.

**File operation outcomes:**

| Outcome | Result |
|---|---|
| Success | Operation completes, files updated, user informed |
| Validation failure | No files written, errors surfaced to user, working copies intact |
| Write failure | Original files untouched, backups preserved, error surfaced to user |
| Partial domain failure | Failed domain rolled back, other domains unaffected |

**Implementation:**

BindForge uses Java's atomic file operations — write to a temp file, validate, then rename over the target. This is the same Controlled Replace pattern already used by Elite Intel's existing `BindingsWorkingCopyRepository` and `BindingsApplyService`. BindForge extends this pattern to cover the Device Identity, Button Map, and Active Preset domains which those classes do not currently handle.

BindForge never writes directly to a game file without first creating a timestamped backup of the current file. If the backup step fails the write is aborted.

**BindForge data storage:**

BindForge stores its ZIP backup archives in its own folder following Elite Intel's existing `AppPaths` convention. A new `getBindForgeDir()` method will be added to `AppPaths` following the same pattern as the existing `getBindingsWorkingDir()`.

| What | Windows | Linux |
|---|---|---|
| ZIP backup archives | `%LOCALAPPDATA%\elite-intel\bindforge\backups` | `~/.local/share/elite-intel/bindforge/backups` |

The existing `elite-intel\bindings` and `elite-intel\bindings\backups` folders remain owned by Elite Intel's existing bindings infrastructure and are not duplicated. BindForge's ZIP backup archives are a separate concept — they package all four file domains together into a single restorable archive, which the existing bindings infrastructure does not do.

---

## 4. BindForge UI Structure

BindForge occupies a top-level tab in Elite Intel's main navigation bar, as defined in KAN-52. Within the BindForge tab, four sections are accessible from a top bar running across the full width of the content area.

### 4.1 Top Bar

Four equal-width buttons spanning the full content width, following Elite Intel's existing HUD component styling:

```
[ DEVICE MAPPING ]  [ BIND EDITOR ]  [ PRESET MANAGER ]  [ BACKUP ]
```

**Section order reflects natural user workflow:**
1. **Device Mapping** — set up your devices first
2. **Bind Editor** — configure your bindings
3. **Preset Manager** — control which binds file loads for each game section
4. **Backup** — protect your work

Active section: filled with accent color, white text. Inactive sections: muted background, secondary text color. Consistent with Elite Intel's existing HUD tab styling.

### 4.2 Dormant Mode

When Elite Dangerous is running, BindForge enters dormant mode. The game may be actively reading its configuration files and those files must not be modified while the game is running.

In dormant mode:
- All write operations are hard-blocked — backup creation, restore, binding edits, device mapping edits, preset manager changes
- The Backup section button changes its label to **"Elite Dangerous is running"** and is visually greyed out
- A persistent warning banner appears across the top of whichever section is active indicating that BindForge is in read-only mode
- Read-only viewing of all loaded data remains available in all sections
- **Exception:** if auto-backup on game launch is enabled in Settings, the auto-backup runs immediately when the game launches before dormant mode takes effect. Auto-backup is a background operation exempt from the dormant mode block.

When Elite Dangerous closes, dormant mode ends immediately. All operations become available and the warning banner is dismissed.

BindForge does not poll for game state. It subscribes to Elite Intel's existing journal event system — the same `GameStarted` and `GameStopped` events that the rest of Elite Intel already uses.

### 4.3 Dirty State on Exit

If the user closes Elite Intel while BindForge has an unapplied working copy — meaning edits have been made but not yet applied to the game files — Elite Intel presents a modal dialog before closing:

```
┌─────────────────────────────────────────────┐
│  BindForge has unapplied changes            │
│                                             │
│  You have unsaved edits that have not been  │
│  applied to your game files.                │
│                                             │
│  [ Apply to Game ]  [ Keep Draft ]  [ Discard ] │
└─────────────────────────────────────────────┘
```

- **Apply to Game** — applies the working copy to the game files then closes
- **Keep Draft** — closes Elite Intel, working copy remains on disk and is reloaded next session
- **Discard** — deletes the working copy and closes

---

## 5. Device Mapping Section

The Device Mapping section is where the user manages `DeviceMappings.xml` and `.buttonMap` files. This is the first section in the BindForge top bar because setting up device identities makes the Bind Editor more useful — friendly device names appear throughout the binding grid instead of raw VIDPID strings.

### 5.1 Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [ DEVICE MAPPING ◀ active ]  [ BIND EDITOR ]  [ PRESET MANAGER ]  [ BACKUP ] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ▼  GAME INSTALLATIONS  2                                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Steam   E:\SteamLibrary\steamapps\common\Elite Dangerous\...  ☐ Mirror │
│  │  Epic    C:\Program Files\Epic\EliteDangerous\...              ☐ Mirror │
│  └─────────────────────────────────────────────────────────────────┘    │
│  [ Apply Mirror Now ]                                                   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ▼  BUILT-IN DEVICES  51                    ▼  MY DEVICES  2            │
│  ┌──────────────────────────────┐           ┌──────────────────────┐    │
│  │ 🔒  GamePad    045E  028E    │           │  RHVCAP   3344  43F4 │    │
│  │ 🔒  BlackWidow 07B5  0317    │           │  Throttle 3344  83F3 │    │
│  │ 🔒  T16000M    044F  B10A    │           │                      │    │
│  │ ...                          │           │  [ + Add Device ]    │    │
│  └──────────────────────────────┘           └──────────────────────┘    │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  DEVICE EDITOR                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Device Name (Alias): [ RHVCAP Direction              ]         │    │
│  │  VID: [ 3344 ]  PID: [ 43F4 ]  [ Recalibrate ]                 │    │
│  │                                                                  │    │
│  │  Connected Controllers:                                          │    │
│  │  ● R-VPC Stick WarBRD-D                                         │    │
│  │  ○ L-VPC Stick WarBRD-D                                         │    │
│  │  ○ T-Rudder                                                     │    │
│  │                                                                  │    │
│  │  BUTTON & AXIS NAMES                                            │    │
│  │  Joy_XAxis    [ Roll                    ]                       │    │
│  │  Joy_YAxis    [ Pitch                   ]                       │    │
│  │  Joy_ZAxis    [ Twist                   ]                       │    │
│  │  Joy_1        [ Trigger                 ]                       │    │
│  │  Joy_2        [ Thumb Button            ]                       │    │
│  │  ...                                                            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Game Installations

The Game Installations section appears at the top of the Device Mapping section because it controls where changes are mirrored to. It is present in Device Mapping because mirroring is the only operation that writes to game installation folders — no other BindForge section needs to know about installations.

Each detected Elite Dangerous installation is shown with its full path and a Mirror checkbox. Mirror is unchecked by default for all installations. When Mirror is checked for an installation and the user clicks Apply Mirror Now, BindForge copies the current `DeviceMappings.xml` and all `.buttonMap` files from the working copy to that installation atomically, creating a timestamped backup of each file first.

On Linux there is exactly one installation — the Steam installation. The Mirror checkbox and Apply Mirror Now button work identically to Windows.

Apply Mirror Now is disabled in dormant mode.

### 5.3 Device Lists

The device area shows two side-by-side lists:

**Built-in Devices** — devices shipped in Frontier's default `DeviceMappings.xml`. Read-only, shown with a lock icon. Cannot be edited or deleted. Always present regardless of whether the user has any custom devices.

**My Devices** — devices the user has created. Fully editable. Each row shows the device alias name, VID, and PID. Devices that have a matching `.buttonMap` file are shown in the accent color. Devices that are missing their `.buttonMap` file are shown in a warning color.

### 5.4 Creating a New Device Mapping

The Connected Controllers list in the Device Editor is always visible and always pre-populated from SDL3 via `elite.intel.devices.DeviceService`. The user does not need to click anything to see what controllers are connected.

The new device creation flow:

1. User clicks a controller in the Connected Controllers list
2. The Device Name text field becomes active
3. User types a friendly alias name for the device
4. The **Create** button becomes available only when all of the following are true:
   - The alias is at least 3 characters and no more than 50 characters
   - The alias contains only letters, numbers, spaces, and the characters `-_+<>{}[]()^`
   - The alias does not contain a period
   - The alias does not match any existing entry in `DeviceMappings.xml`
5. User clicks Create
6. BindForge simultaneously creates:
   - A new entry in `DeviceMappings.xml` with the alias name and VID/PID from the selected controller
   - A new `.buttonMap` file named after the alias
7. The new device appears in My Devices
8. The Device Editor loads the new device ready for button and axis name editing

**Duplicate VID/PID warning:** If the selected controller shares a VID/PID with another currently connected controller, BindForge shows a warning before allowing the user to proceed: *"Two connected controllers share the same hardware identity. The game cannot distinguish between them by hardware alone. Consider including the USB port in the device name — for example 'VirpilStick-USBPort1' and 'VirpilStick-USBPort2' — so you can tell them apart."* The user can acknowledge and continue or cancel.

### 5.5 Loading and Editing an Existing Device

Clicking a device in My Devices loads it into the Device Editor. The Button and Axis Names list auto-populates from the device's `.buttonMap` file. No Load button is required.

When the user begins editing any button or axis name field, BindForge enters **Edit Mode** for that device:

- A persistent label reads **"Edit Mode — Save your changes"**
- All other clickable elements in the Device Mapping section are disabled — the user cannot click another device, add a device, apply mirror, or navigate to another BindForge section
- Two buttons are available: **Save** and **Discard**
- **Save** — commits changes to the working copy and exits Edit Mode
- **Discard** — abandons all changes since Edit Mode was entered and exits Edit Mode
- Attempting to navigate to another BindForge section while in Edit Mode triggers a warning dialog: *"You have unsaved device changes. Save or discard before continuing."*

Changes saved to the working copy are not written to the game installation until the user clicks Apply Mirror Now in the Game Installations section.

### 5.6 Button and Axis Names

The Button and Axis Names list shows every known input code for the selected device:

Axes: `Joy_XAxis`, `Joy_YAxis`, `Joy_ZAxis`, `Joy_RXAxis`, `Joy_RYAxis`, `Joy_RZAxis`, `Joy_UAxis`, `Joy_VAxis`

POV/Hat: `Joy_POV1Up`, `Joy_POV1Down`, `Joy_POV1Left`, `Joy_POV1Right` and additional POV directions as applicable

Buttons: `Joy_1` through `Joy_N` where N is the button count reported by SDL3 for the selected device

Next to each code is an editable text field for the friendly name. **Live highlighting:** when a button is pressed or an axis is moved on the currently selected connected controller, the corresponding row in the list highlights — allowing the user to press each physical control and immediately see which code it generates. Live highlighting is disabled in dormant mode.

### 5.7 First Run

When BindForge opens for the first time and no working copy of `DeviceMappings.xml` exists, BindForge automatically imports `DeviceMappings.xml` and all `.buttonMap` files from the primary detected game installation into the working copy. No prompt is shown. A status message informs the user: *"Your device configuration has been imported from your game installation. This is your personal copy that BindForge will use for all editing."*

If no game installation is detected the working copy starts with Frontier's built-in devices only and the user builds their own devices from scratch.

### 5.8 Dormant Mode

In dormant mode the Device Editor fields are read-only. Create, Save, Discard, and Apply Mirror Now are all disabled. The device lists and Button and Axis Names list remain visible and scrollable. Live controller highlighting is disabled.

---

## 6. Bind Editor Section

The Bind Editor is where the user views and edits the contents of `.binds` files. It absorbs and extends Elite Intel's existing `BindingsTabPanel` — the existing UI becomes the foundation of this section. BindForge does not rebuild what already exists; it relocates it into the BindForge tab and extends it with the features described here.

### 6.1 Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [ DEVICE MAPPING ]  [ BIND EDITOR ◀ active ]  [ PRESET MANAGER ]  [ BACKUP ] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Bindings File:  [ DualVirpilDawnTreader.4.2.binds  ▾ ]  [ Load ]      │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Game Mode ◀      Purpose Mode ⟶ see separate doc      Type Mode ⟶ see separate doc │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [ ALL CONTROLS ] [ GENERAL CONTROLS ] [ SHIP CONTROLS ] [ SRV CONTROLS ] [ ON FOOT CONTROLS ] │
│                                                                         │
│  Search: [                                                           ]  │
│                                                                         │
├──────────────────────────────────────────────┬──────────────────────────┤
│                                              │                          │
│  Binding Grid (75%)                          │  Binding Editor (25%)    │
│                                              │                          │
│  ▼  SHIP CONTROLS — FLIGHT ROTATION          │  (nothing selected)      │
│  Pitch Up/Down Axis    [JOY Y-AXIS]  —       │                          │
│  Roll Left/Right Axis  [JOY X-AXIS]  —       │  Select a binding        │
│  Yaw Left/Right Axis   —             —       │  to edit                 │
│  ...                                         │                          │
│                                              │                          │
│  ▼  SHIP CONTROLS — FLIGHT THRUST            │                          │
│  Thrust Forward        [JOY Z+]      —       │                          │
│  Thrust Backward       [JOY Z-]      —       │                          │
│  ...                                         │                          │
│                                              │                          │
│  ▼  CONFIGURATION                            │                          │
│  Left Panel Focus   FocusOption_Nothing      │                          │
│  Mute Button Mode   mute_pushToTalk          │                          │
│  Enable Camera Lock  1                       │                          │
│  ...                                         │                          │
│                                              │                          │
├──────────────────────────────────────────────┴──────────────────────────┤
│                                                                         │
│  ● IN SYNC          [ Revert ]   [ Apply ]                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Bindings File Bar

A dropdown lists all `.binds` files found in the bindings folder. The currently active preset as determined by the `.start` file is selected by default on load. The Load button parses and displays the selected file. Switching files reloads the working copy for the newly selected file.

If the current file has an unapplied draft, BindForge warns the user before switching: *"You have unapplied changes to [filename]. Apply, keep draft, or discard before switching files."*

### 6.3 Viewing Modes

The Bind Editor has three mode tabs — Game Mode, Purpose Mode, and Type Mode. Only Game Mode is implemented in v1. Purpose Mode and Type Mode are present as inactive tabs in the UI to indicate planned future capability. Their full specifications are maintained in separate documents:

- `BindForge_PurposeMode_Spec.md`
- `BindForge_TypeMode_Spec.md`

---

**Game Mode**

Bindings are organized to match the in-game controls UI exactly. The game presents its controls in four top-level sections — General Controls, Ship Controls, SRV Controls, On Foot Controls — each containing multiple sub-groups. BindForge Game Mode mirrors this structure precisely.

The four top-level sections match the four context bar buttons. Within each section bindings are organized into collapsible sub-groups that correspond exactly to the sub-groups shown in the game's own controls configuration UI. The exact sub-group names and their binding membership are determined by the Binds File Audit — see `BindForge_BindsFileAudit.md`.

**The current Elite Intel implementation uses 9 arbitrary categories that do not match the game's own organization. This is replaced by the 4-section structure described here.**

To ensure accuracy each sub-group mapping is verified by making a test binding in the game and confirming which XML element changed in the `.binds` file. This verification work is part of the Binds File Audit.

Each binding row in Game Mode shows:
- Action name — matching the label used in the game's controls UI
- Primary binding — formatted as `Device | Modifier + Key`
- Secondary binding — in the same format
- Inverted value where applicable
- Deadzone value where applicable
- ToggleOn value where applicable

The exact placement of Inverted, Deadzone, and ToggleOn — whether inline in the grid row or exclusively in the Binding Editor panel — is a design decision to be finalized during the Binds File Audit.

Game Mode also exposes configuration elements from the `.binds` file in a separate **CONFIGURATION** subsection at the bottom of each section that contains them. Configuration elements are standalone value attributes that live in the `.binds` file alongside binding elements but are not bindings themselves. These are styled and presented to match the in-game editing UI as closely as possible. The full inventory of configuration element types and their edit controls is documented in `BindForge_BindsFileAudit.md`.

Search is a persistent filter bar that appears below the context bar in Game Mode. The user types any text and the binding grid filters in real time to show only rows where the action name or any bound key contains the search text. The search bar clears when the user clears the text field.

---

### 6.4 Context Bar

Five equal-width buttons filter the binding grid by game section. Available in Game Mode only.

```
[ ALL CONTROLS ] [ GENERAL CONTROLS ] [ SHIP CONTROLS ] [ SRV CONTROLS ] [ ON FOOT CONTROLS ]
```

ALL CONTROLS is selected by default when a file loads. The four section buttons correspond directly to the four sections of the game's controls UI and the four lines of the `.start` file.

### 6.5 Binding Grid

A scrollable list of collapsible sub-group sections within the selected context. Each row shows:

- **Binding** — the action name matching the label used in the game's controls UI
- **Primary** — the primary input assignment formatted as `Device | Modifier + Key`
- **Secondary** — the secondary input assignment in the same format
- **Additional attributes** — Inverted, Deadzone, ToggleOn where present

Empty slots render as `—`. Friendly device names from `DeviceMappings.xml` replace raw VIDPID strings where a match exists. If no device mapping exists for a VIDPID the raw string is displayed.

The exact column layout for additional attributes is a design decision pending the Binds File Audit.

### 6.6 Binding Editor Panel

The right panel shows the editor for the currently selected binding row. When nothing is selected it shows "Select a binding to edit."

**Keyboard bindings:**
The existing keyboard binding editor is preserved as built. The user assigns a keyboard key with up to one supported modifier. Conflict detection warns if the chosen combination is already in use elsewhere.

**Controller and axis bindings — to be built:**
When a controller or axis binding row is selected the Binding Editor panel shows:

- Current device name and button or axis assignment
- **Capture** button — listens via SDL3 for the next button press or axis movement on any connected controller and assigns it to the selected slot
- **Clear** button — removes the current assignment
- **Modifier** dropdown — assigns a second controller button as a modifier
- **Inverted** checkbox — where the binding has an Inverted attribute
- **Deadzone** slider — where the binding has a Deadzone attribute, range 0.0 to 1.0
- **ToggleOn** checkbox — where the binding has a ToggleOn attribute, checked means toggle, unchecked means hold

**Configuration elements:**
When a configuration element row is selected the Binding Editor panel shows the appropriate edit control for that element type — dropdown, checkbox, text field, or numeric input depending on the value type. The full inventory of configuration element edit controls is documented in `BindForge_BindsFileAudit.md`.

### 6.7 Draft and Apply Workflow

The existing draft and apply workflow is preserved as built:

- Edits go to the working copy in `%LOCALAPPDATA%\elite-intel\bindings\` on Windows or `~/.local/share/elite-intel/bindings/` on Linux
- The game file is never touched during editing
- The sync badge shows **IN SYNC** or **DRAFT**
- **Apply** validates the working copy, creates a timestamped backup of the current game file, then atomically replaces it with the working copy
- **Revert** discards the working copy and reloads from the current game file after confirmation

Apply and Revert are disabled in dormant mode.

### 6.8 Dormant Mode

In dormant mode the Binding Editor panel is read-only. Capture, Clear, Apply, and Revert are all disabled. The binding grid and configuration elements remain visible and scrollable. The sync badge remains visible.

---

## 7. Preset Manager Section

The Preset Manager is where the user controls which `.binds` file loads for each of the four Elite Dangerous binding sections. It manages the `StartPreset.#.start` file directly.

### 7.1 Purpose

Most players have a single `.binds` file covering all four binding sections and the `.start` file has the same preset name on all four lines. The Preset Manager exists for completeness and correctness — to give the user visibility and control over the `.start` file rather than having it be an invisible system file that BindForge silently manages behind the scenes.

Players who use separate `.binds` files for different sections — an unusual but valid configuration — need the Preset Manager to set up and maintain that configuration correctly.

### 7.2 Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [ DEVICE MAPPING ]  [ BIND EDITOR ]  [ PRESET MANAGER ◀ active ]  [ BACKUP ] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Active Preset Configuration                                            │
│                                                                         │
│  The game reads four binding sections independently. Each section       │
│  can be pointed at a different .binds file, or all four can use        │
│  the same file. Most players use one file for all sections.            │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                  │   │
│  │  General Controls    [ DualVirpilDawnTreader.4.2.binds  ▾ ]    │   │
│  │                                                                  │   │
│  │  Ship Controls       [ DualVirpilDawnTreader.4.2.binds  ▾ ]    │   │
│  │                                                                  │   │
│  │  SRV Controls        [ DualVirpilDawnTreader.4.2.binds  ▾ ]    │   │
│  │                                                                  │   │
│  │  On Foot Controls    [ DualVirpilDawnTreader.4.2.binds  ▾ ]    │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  [ Set All to Same ]  [ Save ]  [ Revert ]                             │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ⚠  Changes made here take effect the next time Elite Dangerous        │
│     loads its controls. They do not affect a currently running game.   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Behavior

Each of the four dropdowns lists all `.binds` files currently found in the bindings folder. The current value for each line is read directly from the `StartPreset.#.start` file when the Preset Manager loads.

**Set All to Same** — opens a single dropdown allowing the user to pick one `.binds` file and sets all four sections to that file in one action. This is the common case shortcut.

**Save** — writes the four selections back to the `StartPreset.#.start` file following the Data Integrity Principle — the existing `.start` file is backed up with a timestamp before the new values are written.

**Revert** — reloads the current values from the `.start` file on disk, discarding any unsaved changes.

If the bindings folder contains no `.binds` files the dropdowns show "No preset files found" and Save is disabled.

### 7.4 Consistency Warning

If the user selects different `.binds` files for different sections BindForge does not block this — it is a valid configuration. However BindForge shows a clear informational notice:

*"You have assigned different .binds files to different sections. This is supported by the game but is an advanced configuration. Make sure each file contains the bindings for its assigned section."*

This notice appears when the four dropdowns do not all show the same value. It dismisses automatically when they are all set to the same file.

### 7.5 Relationship to Bind Editor

The Bind Editor's Bindings File dropdown and the Preset Manager are related but independent:

- The Bind Editor dropdown controls which file is open for viewing and editing
- The Preset Manager controls which file the game actually loads for each section

A player can open any `.binds` file in the Bind Editor regardless of what the Preset Manager says. The Preset Manager only controls what the game sees.

### 7.6 Dormant Mode

In dormant mode the four dropdowns, Set All to Same, and Save are all disabled. Revert remains available. A note reads: *"Elite Dangerous is running. Preset configuration cannot be changed while the game is active."*

---

## 8. Backup Section

The Backup section is BindForge's primary feature and the reason BindForge exists. It provides manual ZIP archive backup and restore of all four file domains in a single operation.

### 8.1 Purpose

The Backup section protects the player's complete controller configuration by packaging all four domains — `.binds` file, `DeviceMappings.xml`, all `.buttonMap` files, and the `StartPreset.#.start` file — into a single timestamped ZIP archive that can be restored in one action.

This is distinct from the per-edit commit backups that the Bind Editor creates automatically on Apply. Those are granular single-file backups of the `.binds` file only. The Backup section creates complete snapshots of everything BindForge manages.

### 8.2 Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [ DEVICE MAPPING ]  [ BIND EDITOR ]  [ PRESET MANAGER ]  [ BACKUP ◀ active ] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  BACKUP                                                          │   │
│  │                                                                  │   │
│  │  Create a complete backup of your controller configuration.      │   │
│  │  Includes: .binds file, DeviceMappings.xml, all .buttonMap      │   │
│  │  files, and your active preset configuration.                    │   │
│  │                                                                  │   │
│  │  [ Create Backup Now ]                                           │   │
│  │                                                                  │   │
│  │  Auto-backup on game launch:  ☐ Enabled                         │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  RESTORE                                                         │   │
│  │                                                                  │   │
│  │  Select a backup to restore:                                     │   │
│  │                                                                  │   │
│  │  ┌───────────────────────────────────────────────────────────┐  │   │
│  │  │ 📦 EDO_Backup_2026-06-13_21-30-00.zip   2.3 MB  Full     │  │   │
│  │  │ 📦 EDO_Backup_2026-06-12_18-15-22.zip   2.3 MB  Full     │  │   │
│  │  │ 📦 EDO_Backup_2026-06-10_09-44-11.zip   2.1 MB  Partial  │  │   │
│  │  │ 📦 EDO_Backup_2026-05-28_14-22-05.zip   2.3 MB  Full     │  │   │
│  │  │ ...                                                       │  │   │
│  │  └───────────────────────────────────────────────────────────┘  │   │
│  │                                                                  │   │
│  │  [ Browse for Backup File ]                                      │   │
│  │                                                                  │   │
│  │  Selected: EDO_Backup_2026-06-13_21-30-00.zip                   │   │
│  │                                                                  │   │
│  │  Contents:                                                       │   │
│  │  ✅ DualVirpilDawnTreader.4.2.binds                              │   │
│  │  ✅ DeviceMappings.xml                                           │   │
│  │  ✅ RHVCAP.buttonMap                                             │   │
│  │  ✅ VPCThrottle.buttonMap                                        │   │
│  │  ✅ StartPreset.4.start                                          │   │
│  │                                                                  │   │
│  │  [ Restore Selected Backup ]                                     │   │
│  │                                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Creating a Backup

The user clicks **Create Backup Now**. BindForge:

1. Reads the active `.binds` file path from `AppPaths` and `BindingsLoader`
2. Reads the `StartPreset.#.start` file path
3. Reads the `DeviceMappings.xml` path from the working copy
4. Reads all `.buttonMap` file paths from the working copy
5. Creates a timestamped ZIP archive containing all files
6. Writes the ZIP to `%LOCALAPPDATA%\elite-intel\bindforge\backups\` on Windows or `~/.local/share/elite-intel/bindforge/backups/` on Linux

**ZIP naming convention:** `EDO_Backup_YYYY-MM-DD_HH-MM-SS.zip`

**Backup is hard-blocked if Elite Dangerous is running.** The Create Backup Now button is disabled in dormant mode with a tooltip: *"Cannot backup while Elite Dangerous is running."*

**Full vs Partial:** A backup is marked Full if it contains all four domains. It is marked Partial if any domain was missing or could not be read at backup time. Partial backups are clearly labelled in the backup list.

### 8.4 Auto-Backup on Game Launch

When Auto-backup on game launch is enabled, BindForge automatically creates a backup the moment Elite Dangerous launches — before dormant mode takes effect. This runs as a background operation and does not block or delay the game from starting. The user is not prompted. A brief status message confirms the backup completed.

Auto-backup setting is persisted in the Elite Intel database.

### 8.5 Restore

The user selects a backup from the list or browses for a ZIP file from another location. BindForge reads the ZIP and displays its contents — each file found inside with a checkmark, each expected file that is missing with a warning icon.

Before restoring BindForge shows a confirmation dialog:

```
┌─────────────────────────────────────────────────────────────────┐
│  Restore Backup                                                  │
│                                                                  │
│  This will replace your current controller configuration        │
│  with the backup from 2026-06-13 at 21:30.                     │
│                                                                  │
│  Files that will be replaced:                                   │
│  • DualVirpilDawnTreader.4.2.binds                              │
│  • DeviceMappings.xml                                           │
│  • RHVCAP.buttonMap                                             │
│  • VPCThrottle.buttonMap                                        │
│  • StartPreset.4.start                                          │
│                                                                  │
│  Your current files will be backed up before restore.           │
│                                                                  │
│  [ Restore ]          [ Cancel ]                                │
└─────────────────────────────────────────────────────────────────┘
```

On confirmation BindForge:

1. Creates an automatic backup of the current state before overwriting anything
2. Extracts each file from the ZIP
3. Writes each file to its correct location following the Data Integrity Principle
4. Updates the `.start` file to match what was in the backup
5. Refreshes the Bind Editor and Preset Manager views
6. Shows a success message identifying the pre-restore backup location

**Restore is hard-blocked if Elite Dangerous is running.**

### 8.6 Backup List

The backup list shows all ZIP archives found in the BindForge backups folder, sorted newest first. Each entry shows:

- Archive filename and embedded timestamp
- File size
- Full or Partial indicator
- A brief contents summary on hover

### 8.7 Browse for Backup File

The Browse for Backup File button opens a file chooser filtered to `.zip` files. This allows the user to restore from a backup stored in a different location — for example a backup copied to an external drive or shared between machines.

### 8.8 Dormant Mode

In dormant mode Create Backup Now and Restore Selected Backup are both disabled. The backup list and contents preview remain visible. A persistent notice reads: *"Elite Dangerous is running. Backup and restore are unavailable while the game is active."*

---

## 9. Game State Integration

BindForge subscribes to Elite Intel's existing journal event system to detect when Elite Dangerous is running. It does not poll for game state — it reacts entirely to events that Elite Intel already publishes.

### 9.1 Events Consumed

BindForge subscribes to the existing journal events that signal game launch and game close. The exact event class names are confirmed during implementation by examining `elite.intel.gameapi.journal.events` and `elite.intel.gameapi.journal.subscribers`. BindForge does not add new journal events — it consumes what already exists.

### 9.2 When Elite Dangerous Launches — Dormant Mode

When Elite Dangerous launches BindForge enters dormant mode immediately. The sequence is:

1. If auto-backup is enabled the backup runs first as a background task before dormant mode takes effect
2. Dormant mode activates
3. All write operations are hard-blocked across all four sections
4. A persistent warning banner appears in whichever section is active
5. All edit, save, apply, capture, and mirror buttons are disabled
6. Live controller highlighting in the Device Mapping section stops
7. All data remains visible and scrollable in read-only mode

### 9.3 When Elite Dangerous Closes

Dormant mode ends immediately:

1. The warning banner is dismissed
2. All write operations become available
3. Live controller highlighting resumes
4. If the game modified the `.binds` file while running BindForge detects this via file watching and notifies the user

### 9.4 External File Change Detection

If a file currently loaded in BindForge changes on disk while BindForge is open, BindForge notifies the user:

*"[filename] has been changed on disk since it was loaded. Would you like to reload from disk or keep your current working copy?"*

- **Reload from disk** — discards the current working copy and reloads from the changed file
- **Keep working copy** — continues with the current working copy

File watching uses Elite Intel's existing file monitoring infrastructure following the pattern established by `BindingsMonitor`.

### 9.5 Shutdown Behavior

When Elite Intel closes with an unapplied BindForge working copy, Elite Intel presents the dirty state dialog before closing:

```
┌─────────────────────────────────────────────────────────────────┐
│  BindForge has unapplied changes                                │
│                                                                  │
│  You have edits that have not been applied to your game files.  │
│                                                                  │
│  [ Apply to Game ]   [ Keep Draft ]   [ Discard ]               │
└─────────────────────────────────────────────────────────────────┘
```

BindForge reports dirty state if a working copy exists for any managed file.

---

## 10. Package Structure

BindForge lives within Elite Intel's existing Java package hierarchy following the established `elite.intel` naming convention.

### 10.1 New Packages

```
elite.intel.bindforge                          ← main service, lifecycle, tab panel
elite.intel.bindforge.backup                   ← ZIP backup and restore
elite.intel.bindforge.devicemapping            ← DeviceMappings.xml and .buttonMap management
elite.intel.bindforge.presetmanager            ← StartPreset.#.start management
elite.intel.bindforge.bindseditor              ← bind editor coordination
elite.intel.bindforge.bindseditor.model        ← binding data models
elite.intel.bindforge.bindseditor.parser       ← .binds file parsing extending existing parsers
elite.intel.bindforge.bindseditor.gamemode     ← Game Mode UI and grouping
elite.intel.bindforge.bindseditor.purposemode  ← deferred — see BindForge_PurposeMode_Spec.md
elite.intel.bindforge.events                   ← BindForge internal events
```

### 10.2 Shared Device Infrastructure

BindForge depends on `elite.intel.devices` for all controller input — device enumeration, button capture, and live controller highlighting in the Device Mapping section. The full specification for this shared infrastructure package is maintained separately.

See `EliteIntel_Devices_Spec.md`.

### 10.3 Existing Packages Extended

BindForge builds on top of these existing Elite Intel packages without modifying their public API contracts:

| Existing Package | How BindForge Uses It |
|---|---|
| `elite.intel.ai.hands` | Reuses `KeyBindingsParser`, `BindingsMonitor`, `BindingsLoader`, `BindingsWorkingCopyRepository`, `BindingsApplyService`, `BindingsBackupService`, `BindingsWriter` |
| `elite.intel.util` | Uses `AppPaths` — new `getBindForgeDir()` and `getBindForgeBackupsDir()` methods added |
| `elite.intel.gameapi` | Subscribes to journal events for game launch and close detection |
| `elite.intel.gameapi.EventBusManager` | Publishes and subscribes to events following existing patterns |
| `elite.intel.session` | Reads `SystemSession` for game state |
| `elite.intel.devices` | Shared device infrastructure — see `EliteIntel_Devices_Spec.md` |
| `elite.intel.db` | Uses existing database infrastructure for purpose group persistence and auto-backup setting |

### 10.4 UI Integration

BindForge adds one new top-level tab to Elite Intel's main navigation following the pattern established by KAN-52. The tab panel class lives at:

```
elite.intel.ui.view.BindForgeTabPanel
```

The four section panels — Device Mapping, Bind Editor, Preset Manager, Backup — are sub-panels within `BindForgeTabPanel`.

### 10.5 AppPaths Addition

Two new methods are added to `elite.intel.util.AppPaths`:

```java
public static Path getBindForgeDir() {
    return getAppDataBase().resolve("elite-intel/bindforge");
}

public static Path getBindForgeBackupsDir() {
    return getAppDataBase().resolve("elite-intel/bindforge/backups");
}
```

---

## 11. Data Flow

### 11.1 Overview

```
Elite Dangerous Config Files (game installation + user config folder)
        ↓
AppPaths (path resolution)
        ↓
BindingsLoader / BindingsMonitor (active preset detection and file watching)
        ↓
BindForge XML Parsing (KeyBindingsParser extended + new domain parsers)
        ↓
Immutable Snapshots (in-memory read-only representations)
        ↓
Working Copy created on disk (elite-intel/bindings/ and elite-intel/bindforge/)
        ↓
User Edits → saved to Working Copy
        ↓
Validation
        ↓
[Apply]
        ↓
Timestamped backup of current game file created first
        ↓
Atomic replace — working copy promoted to game file
        ↓
Updated Config Files on Disk

[Revert] → Working copy deleted, original untouched, snapshot reloaded
```

### 11.2 File Loading Sequence

When BindForge opens or a new file is selected:

1. `AppPaths` resolves the bindings folder path for the current platform
2. `BindingsLoader` reads the `StartPreset.#.start` file to determine the active preset name
3. `BindingsMonitor` locates the active `.binds` file matching the preset name
4. `BindingsWorkingCopyRepository` checks for an existing working copy
5. `KeyBindingsParser.parseReadOnlyBindingSlots()` parses the working copy into an immutable snapshot
6. The binding grid is populated from the snapshot
7. `BindingsMonitor` begins watching the active game file for external changes

### 11.3 Edit Sequence

1. User selects a binding row in the grid
2. Binding Editor panel loads the current values for that slot
3. User makes changes
4. On Save, `BindingsWriter` performs a targeted XML rewrite of only the affected element
5. Sync badge updates to DRAFT
6. No game files are touched

### 11.4 Apply Sequence

1. Working copy is validated as well-formed XML
2. Current game file is backed up with a timestamp
3. Working copy is atomically renamed over the game file
4. Sync badge updates to IN SYNC

### 11.5 Backup Sequence

1. BindForge reads current working copies for all four domains
2. All files are packaged into a ZIP archive
3. ZIP is written atomically to the BindForge backups folder
4. Backup list refreshes

### 11.6 Restore Sequence

1. An automatic pre-restore backup of the current state is created first
2. ZIP is extracted to a temp location
3. Each file is validated
4. Each file is written to its correct location atomically
5. Working copies are refreshed from the restored files
6. All views reload
7. Success message identifies the pre-restore backup location

### 11.7 External Change Detection

If the active game file changes on disk from an external source while BindForge is open, BindForge detects the change and notifies the user with the reload or keep choice.

---

## 12. Roadmap and Deferred Features

### 12.1 Purpose Mode

Full specification: `BindForge_PurposeMode_Spec.md`

### 12.2 Type Mode

Full specification: `BindForge_TypeMode_Spec.md`

### 12.3 Controller Map Mode

An input-first view of the Bind Editor. The user selects a connected device and sees a visual representation showing all currently bound actions next to each button and axis. Future stretch goal: a 3D interactive model of the controller.

### 12.4 Preset Rename and Duplicate

- Rename `.binds` preset files
- Duplicate a preset as a starting point for a new one

### 12.5 Binding Export

Export the current binding layout as a printable graphic or shareable file.

### 12.6 Multi-Installation Sync — Windows Only

When the user has multiple Elite Dangerous installations on Windows, provide tooling to compare and synchronize `.binds` files across installations. Not applicable on Linux.

### 12.7 Binds File Audit

Full specification: `BindForge_BindsFileAudit.md`

---

## 13. Open Questions and Known Unknowns

### 13.1 Binds File Structure — Pending Audit

See `BindForge_BindsFileAudit.md`.

### 13.2 Game Mode Sub-Group Mapping — Pending Audit

See `BindForge_BindsFileAudit.md`.

### 13.3 Additional Binding Attributes

There may be other attributes in the `.binds` file beyond Inverted, Deadzone, and ToggleOn. The Binds File Audit will produce a complete inventory.

### 13.4 Inverted, Deadzone, and ToggleOn Display

Whether these appear inline in the binding grid or exclusively in the Binding Editor panel is an open design decision pending the Binds File Audit.

### 13.5 Journal Events for Game State

The exact event class names that signal Elite Dangerous launching and closing are confirmed during implementation.

### 13.6 Database Schema for Purpose Groups

Determined during Purpose Mode implementation. See `BindForge_PurposeMode_Spec.md`.

### 13.7 Linux Bindings Path Validation

The Linux path documented in Section 2.1 is the standard Steam/Proton path. Individual Linux installations may vary and should be validated during testing.

### 13.8 DeviceMappings.xml Working Copy Location

The exact location within `elite-intel/bindforge/` for device mapping working copies is determined during implementation.

### 13.9 HUD Component Compatibility

BindForge UI must use Elite Intel's HUD component system introduced by Gnevko's V1.1 UI redesign. The exact set of available HUD components is confirmed by examining the existing implementation before building BindForge UI panels.

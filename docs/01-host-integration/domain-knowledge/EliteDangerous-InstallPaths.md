# Elite Dangerous — Installation & File Path Discovery Reference

**Scope:** How to locate an Elite Dangerous installation and its associated config files (`.binds`, `StartPreset.*.start`, `DeviceMappings.xml`, `.buttonMap`) across Steam, Epic, and Frontier-launcher installs on Windows, plus the Linux/Proton path pattern. See `EliteDangerous-BindsFileFormat.md` and `EliteDangerous-DeviceMappings-ButtonMap.md` for what these files contain once located.

**Confidence:** Windows path facts below are confirmed against real, verified machine paths (including a real multi-storefront Windows machine with Epic and Steam installed side by side). The Linux/Proton path is a strong, well-reasoned candidate derived from general Proton/compatdata conventions and Elite Dangerous's Steam AppID — it has **not** been verified against a real Linux installation. Treat it accordingly; see §5.

---

## 1. The Two Different File Families Have Different Multiplicity Rules

This is the single most important fact to internalize before designing any discovery logic — get it backwards and a tool will either miss real per-storefront copies or duplicate work needlessly on a file that's actually shared:

| File family | Location | Shared or per-storefront? |
|---|---|---|
| `.binds`, `StartPreset.*.start` | `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\` (note the space in "Frontier Developments") | **One single shared location**, regardless of storefront (Steam, Epic, Frontier Direct, Oculus), for a given Windows user account. Confirmed, not in dispute. |
| `DeviceMappings.xml`, `.buttonMap` | `[GameInstall]\Products\<product-name>\ControlSchemes\`, inside **each storefront's own install folder** | **Genuinely duplicated per storefront install.** Confirmed real on a machine with Epic and Steam installed side by side — each had its own independent copy. Example real paths: `[Epic install]\Products\elite-dangerous-64\ControlSchemes\DeviceMappings.xml`, `[Steam install]\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceMappings.xml`. |

`DeviceMappings.xml`/`.buttonMap` only work from that install-folder `ControlSchemes` location — placing a copy in the shared `Options\Bindings\` folder alongside `.binds` does not work.

**An earlier research pass hypothesized a single shared AppData location for `ControlSchemes`** (`%LOCALAPPDATA%\Frontier_Developments\Products\<product>\ControlSchemes\`, underscored, distinct from the space-separated `.binds` path above) that all storefronts would share. That hypothesis was **half right, and the earlier retraction of it went too far** — corrected 2026-09-08 against
[Frontier's own installation-locations page](https://customersupport.frontier.co.uk/hc/en-us/articles/4405700513298-Game-installation-and-file-locations).

**Wrong:** that it is *shared across storefronts*. It is not; the per-install table above stands.

**Right:** that the path exists. It is the game folder of the **alternative Frontier-launcher
installation**, which Frontier documents as a first-class layout — so a `ControlSchemes` folder genuinely
does live there, for that one installation type.

**Why the retraction happened, and the lesson in it.** The hypothesis was tested against a machine holding
Steam and Epic installs. Neither puts anything at that path, so the check came back empty and read as a
refutation. **Absence on one machine disproved the wrong claim** — it showed the path is not universal, not
that it is not real.

The practical cost of leaving it retracted would have been real: a discovery routine following the previous
wording would deliberately skip that path and **miss an entire installation type**.

It also explains the space-versus-underscore oddity, which is two different folders rather than a typo:
`Frontier Developments` (space) is the **user configuration** folder holding `.binds`;
`Frontier_Developments` (underscore) is the **alternative install's game folder**.

The `Products\` subfolder name is keyed by product (`elite-dangerous-64`, `elite-dangerous-odyssey-64`, and historically `elite-dangerous-horizons-64`) rather than by storefront. The full, definitive list of possible product names was not independently confirmed beyond those three — enumerate whatever subfolders actually exist under a given install's `Products\` at runtime rather than hardcoding an exhaustive list, since Frontier has changed product naming across expansions before.

### Confirmed 2026-09-07: one installation can hold several products, each with its own `ControlSchemes`

Observed directly on a real machine rather than inferred. A single Steam installation carried **both**
`elite-dangerous-64` and `elite-dangerous-odyssey-64`, and **each had its own `ControlSchemes` folder
holding its own `DeviceMappings.xml`.** They were not copies of one another:

| Product | Device entries | `DeviceButtonMaps` |
|---|---|---|
| `elite-dangerous-64` (Horizons) | 49 — no `VPCPanel`, no `VPCThrottle` | **absent entirely** |
| `elite-dangerous-odyssey-64` | 51 | present |

So the multiplicity rule above is sharper than "per storefront": the device-file family is duplicated
**per product per storefront**. The same machine held three `ControlSchemes` folders across two
installations.

**A missing `DeviceButtonMaps` folder is normal, not a fault.** Horizons ships none at all.

**BindForge targets `elite-dangerous-odyssey-64` only**, so it treats the product as a constant rather
than a dimension — see [File Manager](../../02-features/bindforge/file-manager.md#game-install-locations).
Discovery still has to walk into the right product folder rather than assuming there is one, and a tool
that assumed a single `ControlSchemes` per installation would silently read or write the wrong one.

The shared `Options\Bindings\` folder is genuinely shared across products, but its files are versioned by
preset number — `.4.x` files and `StartPreset.4.start` are Odyssey's, and an older product writes its own
generation into the same folder. So "shared location" does not mean "shared files".

---

## 1b. Installation Locations by Store

**Consolidated 2026-09-09** from the ported `Elite Dangerous Installation Locations.md`, which covered this
ground first and is now merged here. Cross-checked against
[Frontier's own installation-locations article](https://customersupport.frontier.co.uk/hc/en-us/articles/4405700513298-Game-installation-and-file-locations)
(last updated 2026-06); the two agree.

**Five shapes, not three.** The Frontier launcher has *two* documented defaults, and Oculus Home is a
storefront in its own right.

Frontier's article gives Horizons paths and notes that **Odyssey variants sit under
`Products\elite-dangerous-odyssey-64`** — the install root is what varies, the product folder is the
constant pattern below it. BindForge targets the Odyssey product only, so the product is a constant while
all five roots are in scope for discovery.

### Frontier Direct — standard

```
Game install folder:   C:\Program Files (x86)\Frontier\Products\<product>\
NetLog:                C:\Program Files (x86)\Frontier\Products\<product>\logs\
AppConfig.xml:         C:\Program Files (x86)\Frontier\Products\<product>\
TelemetryCache.log:    C:\Users\%username%\AppData\Local\Frontier Developments\Elite Dangerous\
Client.log:            C:\Users\%username%\AppData\Local\Frontier_Developments\logs\
Update.log:            C:\Program Files (x86)\Frontier\logs\
```

### Frontier Direct — alternative

```
Game install folder:   C:\Users\%username%\AppData\Local\Frontier_Developments\Products\<product>\
NetLog:                C:\Users\%username%\AppData\Local\Frontier Developments\Products\<product>\logs\
AppConfig.xml:         C:\Users\%username%\AppData\Local\Frontier_Developments\Products\<product>\
Client.log:            C:\Program Files (x86)\Frontier\EDLaunch\logs\
Update.log:            C:\Program Files (x86)\Frontier\EDLaunch\logs\
```

**This is the underscore path**, and it is a real installation type rather than a mistake — see the
correction in [§1](#1-the-two-different-file-families-have-different-multiplicity-rules). Note the two
spellings sitting side by side in the block above: `Frontier Developments` with a space is the *user
configuration* tree, `Frontier_Developments` with an underscore is the *alternative install*. They are
different folders, not a typo of one another.

### Epic Games (default)

```
Game install folder:   C:\Program Files\Epic Games\EliteDangerous\
NetLog:                C:\Program Files\Epic Games\EliteDangerous\Products\<product>\Logs\
AppConfig.xml:         C:\Program Files\Epic Games\EliteDangerous\Products\<product>\
Client.log:            C:\Program Files\Epic Games\EliteDangerous\logs\
Update.log:            C:\Program Files\Epic Games\EliteDangerous\Products\<product>\Logs\
```

### Steam (default library — may differ with custom Steam libraries)

```
Game install folder:   C:\Program Files (x86)\Steam\steamapps\common\Elite Dangerous\Products\<product>\
NetLog:                C:\Program Files (x86)\Steam\steamapps\common\Elite Dangerous\Products\<product>\Logs\
AppConfig.xml:         C:\Program Files (x86)\Steam\steamapps\common\Elite Dangerous\Products\<product>\
Client.log:            C:\Program Files (x86)\Steam\steamapps\common\Elite Dangerous\logs\
Update.log:            C:\Program Files (x86)\Steam\steamapps\common\Elite Dangerous\logs\
```

A real non-default library, observed on the developer's machine:

```
E:\SteamLibrary\steamapps\common\Elite Dangerous\Products\elite-dangerous-odyssey-64\
```

That one is still found automatically, because `libraryfolders.vdf` names it — see
[§6](#detection-strength-is-not-uniform--recorded-2026-09-08) for why a non-default path is not the same
as an undetectable one.

### Oculus Home (default — may differ with custom install options)

```
Game install folder:   C:\Program Files\Oculus\Software\Software\frontier-developments-plc-elite-dangerous\
NetLog:                C:\Program Files\Oculus\Software\Software\frontier-developments-plc-elite-dangerous\Products\<product>\Logs\
AppConfig.xml:         C:\Program Files\Oculus\Software\Software\frontier-developments-plc-elite-dangerous\Products\<product>\
Client.log:            C:\Program Files\Oculus\Software\Software\frontier-developments-plc-elite-dangerous\logs\
Update.log:            C:\Program Files\Oculus\Software\Software\frontier-developments-plc-elite-dangerous\logs\
```

**`Software\Software` is doubled on purpose.** That is what Frontier publishes and what Oculus actually
does. Anything that "corrects" it will fail to find the install.

### The files BindForge manages, within any install

The `[GameInstall]` root varies by store; everything below it does not.

| File | Path within the install |
|---|---|
| `DeviceMappings.xml` | `[GameInstall]\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceMappings.xml` |
| `.buttonMap` files | `[GameInstall]\Products\elite-dangerous-odyssey-64\ControlSchemes\DeviceButtonMaps\` |

> **Both are erased by game updates and must be restored afterwards.** The game install folder is the only
> valid location for them — putting a copy in the user config bindings folder does not work. This is the
> incident that motivated BindForge.

### Journal file path (Windows)

```
%USERPROFILE%\Saved Games\Frontier Developments\Elite Dangerous\
```

For the Linux/Proton equivalent see [§5](#5-linux--proton-path-pattern--confirmed-journal-path).

## 2. Finding a Steam Install

### Why the registry alone is not enough

Steam's own install location (where `steam.exe` lives — **not** where games are installed) is discoverable via:

```
HKEY_LOCAL_MACHINE\SOFTWARE\Valve\Steam             (32-bit OS)
HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\Valve\Steam  (64-bit OS)
```

using the `InstallPath` value. There's also a per-user fallback at `HKEY_CURRENT_USER\Software\Valve\Steam` (`SteamPath`), useful when the HKLM key is absent (e.g. a non-admin install).

**This only tells you where the Steam client itself lives, not where any individual game is installed.** Steam does not reliably write a registry key for each installed game's path across versions. Legacy per-game `Uninstall` registry entries exist (`HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Steam App <appid>`) but Valve has been inconsistent about populating them — not a dependable primary source. Treat the registry purely as a bootstrap for locating `steamapps\`, never as a source of per-game install paths.

### The reliable mechanism: `libraryfolders.vdf`

Steam keeps two copies, which it reconciles at startup:

```
<Steam install dir>\steamapps\libraryfolders.vdf
<Steam install dir>\config\libraryfolders.vdf   (source of truth Steam copies from on launch)
```

Format — Valve's KeyValues (VDF) text format:

```vdf
"libraryfolders"
{
    "0"
    {
        "path"  "C:\\Program Files (x86)\\Steam"
        "label" ""
        "contentid"     "..."
        "totalsize"     "0"
        "apps"
        {
            "359320"    "84934656512"
        }
    }
    "1"
    {
        "path"  "D:\\SteamLibrary"
        ...
    }
}
```

Each numbered block is a library folder; its `apps` map lists installed AppIDs with on-disk size. **Elite Dangerous's Steam AppID is `359320`.** Checking for that key under any library's `apps` block confirms an ED install exists there; the install directory is `<path>\steamapps\common\Elite Dangerous\`.

Parsing this format is straightforward (recursive-descent or regex; no escaping beyond `\\` for backslashes). **Caveat:** older Steam versions used a flatter, unquoted-path-list format with no `apps` sub-block at all — a parser should tolerate both shapes, or fall back to scanning each library's `steamapps\common\Elite Dangerous\` directly rather than depending on the `apps` map being present.

---

## 3. Finding an Epic Install

Epic does not register individual game install paths in the registry in any way documented as stable for third-party tools. The reliable mechanism is manifest files:

```
C:\ProgramData\Epic\EpicGamesLauncher\Data\Manifests\*.item
```

One `.item` file per installed game, plain JSON, with fields including `InstallLocation`, `DisplayName`, `AppName`/`CatalogItemId`, and `LaunchExecutable`. Discovery approach: enumerate all `*.item` files, parse each as JSON, match `DisplayName` (or `AppName`) against a known Elite Dangerous identifier, and read `InstallLocation` for the install path. This is a stable, documented mechanism used by several community relinking/migration tools — more reliable than any registry-based approach for Epic.

---

## 4. Why Registry Detection Isn't a Reliable Primary Source (Summary)

- **Steam:** `HKLM\SOFTWARE\(Wow6432Node\)Valve\Steam\InstallPath` gives Steam's own client folder, not any game's install path. Legacy per-game `Uninstall` entries exist but are inconsistently populated across Steam versions. Use the registry only to bootstrap finding `steamapps\`; use `libraryfolders.vdf` (§2) for actual game paths.
- **Epic:** no documented, stable per-game registry path exists at all. Use the `.item` manifest files (§3) instead.
- **Frontier launcher:** no separate manifest/registry database analogous to Steam's `libraryfolders.vdf` or Epic's `.item` manifests was found. **But there are two documented default locations to probe** before giving up and asking — see [§1b](#1b-installation-locations-by-store). A Frontier-launched install's data lands in the same per-product `Products\<name>\` structure described in §1 — there's no Frontier-specific discovery step needed beyond knowing the install folder itself (which, for a Frontier-launched install, is wherever the user pointed EDLaunch's configurable install path).

---

## 5. Linux / Proton Path Pattern — Confirmed (Journal Path)

Elite Dangerous is not natively ported to Linux; it typically runs under Steam Proton. When it does, the entire Windows-style AppData tree lives inside a per-app Wine prefix under Steam's `compatdata` directory:

```
~/.steam/steam/steamapps/compatdata/359320/pfx/drive_c/users/steamuser/AppData/Local/Frontier Developments/Elite Dangerous/Options/Bindings/
```

(`359320` is ED's Steam AppID — the same value used in `libraryfolders.vdf`'s `apps` map on Windows, §2.) `pfx` is the Wine prefix root; everything below `drive_c/users/steamuser/AppData/Local/...` mirrors the Windows path structure from §1, including the same `Frontier Developments` spacing.

**The journal half of this path (`Saved Games/Frontier Developments/Elite Dangerous`) is confirmed as of 2026-07-22** — it exactly matches what EDMarketConnector's own official wiki instructs Linux users to configure, independent community precedent from a tool actively used on real Proton installs, not just our own analogy from general conventions. The bindings half (`Options/Bindings`) uses the same `AppData/Local` structure one level over and is very likely correct by the same logic, but wasn't independently spelled out by that source — treat it as strongly supported, not yet independently confirmed the same way.

Additional considerations if/when this is implemented:

- **`compatdata` can live in any Steam library, not just the default one.** Since `compatdata` lives under `<library>/steamapps/compatdata/<appid>/`, constructing this path correctly requires parsing `libraryfolders.vdf` on the Linux side too (§2) — there is no registry to fall back on, so on Linux, Steam-library discovery is not optional the way it is on Windows.
- The Wine prefix is tied to the Linux user who installed/ran the game; multi-user Linux machines complicate "find every install," though this is an unlikely edge case for a single-user desktop companion app.
- Frontier-launcher-direct and Epic installs are not meaningfully supported on Linux outside of community Wine setups — Proton via Steam is the realistic Linux path to design for.
- The same per-storefront-vs-shared multiplicity split from §1 presumably still applies once inside the Proton prefix (i.e. `.binds`/`StartPreset` shared, `DeviceMappings.xml`/`.buttonMap` per-install), but this has not been separately verified for the Linux/Proton case — it's an inference from the Windows behavior, not an independent confirmation.

---

## 6. Design Rule: Automatic Detection Is a Convenience, Not a Guarantee

Given the caveats throughout this document — the unverified Linux/Proton path, VDF format drift across Steam versions, an unconfirmed full list of Frontier `Products\` names, and no authoritative Frontier manifest of any kind — **treat automatic install/path detection as a convenience, never as the only mechanism.** A manual override must always be available and persisted:

### Detection strength is not uniform — recorded 2026-09-08

Two things decide whether an install can be found automatically: a **documented default path**, and an
**authoritative manifest** that records where it actually went. Only the second survives the user moving it.

| Storefront | Default path | Manifest | Non-default install findable? |
|---|---|---|---|
| Steam | yes | `libraryfolders.vdf` | **yes** |
| Epic | yes | `.item` manifests | **yes** |
| Frontier — standard | yes | **none** | **no** |
| Frontier — alternative | yes | **none** | **no** |
| Oculus Home | yes | unknown | unknown |

**Worked example, from the developer's own machine.** The Steam install sits at
`E:\SteamLibrary\steamapps\common\Elite Dangerous\` — a different drive from Frontier's published default —
and is still found, because `libraryfolders.vdf` names it. **A non-default location is not the same as an
undetectable one, where a manifest exists.**

**Which is why manual add is not a uniform fallback.** For Steam and Epic it covers a corrupt manifest or an
exotic setup — rare, and worth having anyway. **For the Frontier launcher it is the only mechanism**, because
EDLaunch's install path is user-configurable and nothing records the choice anywhere BindForge can read. A
Frontier install moved off both documented defaults is invisible to detection, permanently, by construction.

**Open:** whether Oculus Home exposes a manifest of installed titles. Not investigated. Treat as *no* until
someone checks, which costs nothing — the manual path exists regardless.

1. **Automatic detection, where implemented,** should scan/enumerate using the mechanisms in §2–§3 (and, for Linux, §5) purely to *suggest* a default. It should never be the sole path to a working configuration.
2. **A manual override — a folder-picker style "browse and select" flow — must always be present, not just a fallback of last resort.** Whatever path the user selects should be persisted so they are never asked twice for the same install. This is the right default behavior for: Linux/Proton users where automatic `compatdata` discovery fails (non-default Steam library, unusual Wine setup, or the path pattern in §5 simply turning out to be wrong once checked against a real install); any future Frontier path-naming change that breaks a hardcoded assumption; and any storefront-specific quirk not yet accounted for here.
3. **If more than one product folder is found** (e.g. both a legacy and a current product folder from an old pre-merge install), surface all of them to the user as named choices rather than silently picking one — avoids silently pointing a tool at the wrong product's files.

**Standing flag:** the journal half of the Linux/Proton path in §5 is confirmed via independent community precedent (EDMarketConnector); the bindings half is strongly supported by the same structure but not independently spelled out. Hands-on testing against a real Proton install is still the eventual gold-standard check, and the manual-override path must stay fully functional on Linux regardless.

---

*Path blocks in §1b originate from `Elite Dangerous Installation Locations.md`, ported from the EDO
StellarCore set (sources: `2026-05-04 Originals/all elite dangerous paths system and user.txt`, the
StellarCore Application Specification §5.8/§7, and the BindForge Plugin Specification §2.2–§2.4). That
document was merged into this one on 2026-09-09 and deleted: the two covered one topic from two
directions, nothing linked to it, and its contents were consequently re-derived from Frontier's support
site rather than read. One document per topic is the fix.*

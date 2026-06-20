# Elite Dangerous Installation Discovery — Research Report

**Scope:** Research and reporting only. No code written.
**Goal:** Determine how BindForge can reliably locate the `ControlSchemes` folder (containing
`DeviceMappings.xml` and the `DeviceButtonMaps` subfolder) for every Elite Dangerous
installation on a user's machine.

---

## Key finding that reframes the whole problem

Before going point-by-point: the premise that there are "N installs → N separate
`ControlSchemes` folders" is **not quite accurate**, and this changes the design significantly.

`ControlSchemes`, `DeviceMappings.xml`, and the user's `.binds` files are **not stored inside
the storefront install directory at all.** They live in a per-**Product** AppData location:

```
%LOCALAPPDATA%\Frontier_Developments\Products\elite-dangerous-64\ControlSchemes\
%LOCALAPPDATA%\Frontier_Developments\Products\elite-dangerous-odyssey-64\ControlSchemes\
```

This path is keyed by the **product name** (`elite-dangerous-64`,
`elite-dangerous-odyssey-64` — and historically `elite-dangerous-horizons-64`), not by which
storefront launched the game. According to community reports, **Steam and Frontier-launcher
installs of the same product share this same AppData location** — settings, keybinds, and
journals are identical across them because they write to the same path. Epic has been reported
in some cases to maintain a separate copy due to how its installer lays out the `Products`
folder, but this is based on forum testimony rather than an authoritative source — see the
caveat under §3.

**Practical consequence:** for the common case (Steam + Frontier-launcher on the same Windows
account), BindForge does **not** need to detect "which storefronts have ED installed" in order
to find the bindings — it only needs to enumerate:

```
%LOCALAPPDATA%\Frontier_Developments\Products\*\ControlSchemes\
```

Storefront detection (Steam registry, Epic manifests) becomes a **secondary, confirmatory**
signal — useful for "is this game actually installed, or is this a leftover folder from an
uninstalled product variant" and for Linux/Proton path construction (see §2/§5) — not the
primary mechanism for locating bindings data.

This finding should be validated against a real multi-storefront test machine before being
treated as fully reliable; see the caveats in each section below.

Sources:
- [Game installation and file locations – Frontier Support](https://customersupport.frontier.co.uk/hc/en-us/articles/4405700513298-Game-installation-and-file-locations-Netlog-AppConfig-Client-Log-Update-Log-Game-Folder)
- [Installing ED in multiple locations — Steam Community](https://steamcommunity.com/app/359320/discussions/0/1699416432417594622/?ctp=2)
- [Epic + Steam — Steam Community](https://steamcommunity.com/app/359320/discussions/0/3109141414229974826/)
- [EDConfig - HerzbubeWiki](https://wiki.herzbube.ch/index.php/EDConfig)

---

## 1. Windows Registry — Steam and Epic

### Steam

Steam's own install location (where `steam.exe` lives, **not** game library locations) is
discoverable via:

```
HKEY_LOCAL_MACHINE\SOFTWARE\Valve\Steam            (32-bit OS)
HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\Valve\Steam (64-bit OS)
```

The `InstallPath` value under that key gives the Steam client's own directory. This is **not**
where games are installed — it only tells you where Steam itself lives, which is needed as a
starting point to find `steamapps/libraryfolders.vdf` (see §2).

There is also a per-user key, `HKEY_CURRENT_USER\Software\Valve\Steam`, which some tools use to
read the currently logged-in Steam user's `SteamPath` — useful as a fallback if the
HKLM key is absent (e.g. non-admin install).

Steam does **not** write a registry key for each individual installed game's path in a
consistently reliable cross-version way; the canonical and currently-supported method for
finding *game* install paths is via `libraryfolders.vdf`, not the registry. (Some legacy
`Uninstall` registry entries exist per-game under
`HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Steam App <appid>`,
but Valve has been inconsistent about populating these, so they're not a dependable primary
source.)

### Epic Games Launcher

Epic does not appear to register individual game install paths in the registry in a way that's
documented as stable for third-party tools to rely on. The reliable mechanism is the manifest
file system under `ProgramData` (§3), not the registry.

Sources:
- [Steam registry keys — Larian Studios forums](https://forums.larian.com/ubbthreads.php?ubb=showflat&Number=489271)
- [Steam Installation Discovery — DeepWiki](https://deepwiki.com/akorb/SteamShutdown/2.2.1-steam-installation-discovery)
- [Use Windows Registry Paths to better determine Steam Library — GitHub issue](https://github.com/JMTK/SunshineGameFinder/issues/19)

**Caveat:** registry-key names and the reliability of `Uninstall` entries can shift between
Steam client versions; treat the registry as a bootstrap for finding `steamapps/`, not as a
source of per-game paths.

---

## 2. Steam Library Config — `libraryfolders.vdf`

This is the practical way to enumerate every Steam library across all drives.

**Default location** (note: Steam keeps two copies and reconciles them at startup):
```
<Steam install dir>\steamapps\libraryfolders.vdf
<Steam install dir>\config\libraryfolders.vdf   (source of truth Steam copies from on launch)
```

**Format** — Valve's KeyValues (VDF) text format, readable without a special library:

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

Each numbered block is a library folder; the `apps` map inside lists installed AppIDs with
their on-disk size. **Elite Dangerous's Steam AppID is `359320`** — checking for that key under
each library's `apps` block confirms an ED install exists in that library, and
`<path>\steamapps\common\Elite Dangerous\` is the install directory.

**Parsing approach:** simple recursive-descent or regex-based VDF parsing is sufficient (the
format has no edge cases like escaping beyond `\\` for backslashes); several small open-source
VDF parsers exist as reference implementations.

**Caveat:** older Steam versions used a flatter, unquoted-path-list format in
`libraryfolders.vdf` (just a numbered list of paths, no `apps` sub-block) — a parser should
tolerate both shapes or just fall back to scanning each library's
`steamapps\common\Elite Dangerous\` directly rather than relying on the `apps` map.

Sources:
- [Where does Steam save the library folders? — Steam Community](https://steamcommunity.com/discussions/forum/1/558751364264531176/)
- [Check installation path based on "libraryfolders.vdf" — Nitrox GitHub issue](https://github.com/SubnauticaNitrox/Nitrox/issues/142)
- [libraryfolders.vdf reset on every steam-start — Steam Discussions](https://steamcommunity.com/discussions/forum/0/3044985412473578414/)
- [fix: Steam update changed path of LibraryFolders.vdf — HXE GitHub issue](https://github.com/HaloSPV3/HXE/issues/218)

---

## 3. Epic Games Manifest Files

Epic Games Launcher maintains one `.item` manifest file per installed game under:

```
C:\ProgramData\Epic\EpicGamesLauncher\Data\Manifests\*.item
```

Each `.item` file is plain JSON. Relevant fields include `InstallLocation`, `DisplayName`,
`AppName`/`CatalogItemId`, and `LaunchExecutable`. A scanner can:

1. Enumerate all `*.item` files in that folder.
2. Parse each as JSON.
3. Match `DisplayName` (or the internal `AppName`) against a known Elite Dangerous identifier
   string to find the right manifest.
4. Read `InstallLocation` for the install path.

This is a stable, documented mechanism (used by several community relinking/migration tools)
and is more reliable than registry scanning for Epic.

**Caveat on shared bindings:** one forum source claimed Epic installs keep a separate copy of
settings/bindings from Steam/Frontier installs on the same machine ("Epic requires a separate
install to other versions"). This wasn't corroborated by an authoritative source and may
reflect either: (a) a historical bug, (b) user error, or (c) a genuine difference in how Epic's
installer lays out the `Products` folder relative to `%LOCALAPPDATA%`. **This should be
verified on a real Epic install before BindForge assumes bindings are always unified** —
worst case, it just means treating Epic as its own discovery path even though Steam/Frontier
can be unified.

Sources:
- [Where are the Epic Games manifest files? — orbispatches.com](https://orbispatches.com/gaming-faq/where-are-the-epic-games-manifest-files)
- [Moving Epic Games Store games without re-installing — codeinsecurity](https://codeinsecurity.wordpress.com/2019/11/22/moving-epic-games-store-games-without-re-installing-them/)
- [Epic Games Library Relinker — GitHub](https://github.com/Supernova1114/Epic-Games-Library-Relinker)
- [Epic + Steam — Steam Community](https://steamcommunity.com/app/359320/discussions/0/3109141414229974826/)

---

## 4. Frontier-Specific Detection

There is no separate Frontier-launcher manifest/registry database analogous to Steam's
`libraryfolders.vdf` or Epic's `.item` manifests that was found in this research. The Frontier
launcher (EDLaunch) installs itself to a user-configurable path (default
`C:\Program Files (x86)\Frontier\EDLaunch`), and game data for Frontier-launched installs goes
to the same `%LOCALAPPDATA%\Frontier_Developments\Products\<product>\` location described in
the "Key finding" section above.

Because that AppData location is **not gated by which storefront launched the game**, Frontier
itself does not need its own discovery mechanism for the purposes of finding `ControlSchemes` —
walking `%LOCALAPPDATA%\Frontier_Developments\Products\*\ControlSchemes\` directly covers
Frontier-launcher installs (and, per the caveat above, likely Steam installs of the same
product) without needing any Frontier-specific registry or manifest lookup at all.

The full, definitive list of `Products\` subfolder names was not independently confirmed in
this research beyond `elite-dangerous-64` and `elite-dangerous-odyssey-64` (and the
historical `elite-dangerous-horizons-64`); BindForge should enumerate whatever subfolders
exist under `Products\` at runtime rather than hardcoding an exhaustive list, since Frontier
has changed product naming across expansions before.

Sources:
- [Game installation and file locations – Frontier Support](https://customersupport.frontier.co.uk/hc/en-us/articles/4405700513298-Game-installation-and-file-locations-Netlog-AppConfig-Client-Log-Update-Log-Game-Folder)
- [How to set the installation folder? — Frontier Forums](https://forums.frontier.co.uk/threads/how-to-set-the-installation-folder.519383/)
- [Location of install of ED:O — Frontier Forums](https://forums.frontier.co.uk/threads/location-of-install-of-ed-o.626009/)

---

## 5. Linux / Proton Path Differences

On Linux, Elite Dangerous typically runs under Steam Proton (it is not natively ported), and
the entire Windows-style AppData tree lives inside a per-app Wine prefix under Steam's
`compatdata` directory:

```
~/.steam/steam/steamapps/compatdata/359320/pfx/drive_c/users/steamuser/AppData/Local/Frontier Developments/Elite Dangerous/Options/Bindings/
```

(`359320` is ED's Steam AppID, matching the `apps` key seen in `libraryfolders.vdf` on
Windows.) The `pfx` directory is the Wine prefix root; everything below
`drive_c/users/steamuser/AppData/Local/...` mirrors the Windows path structure described above.

**This means on Linux, Steam detection is not optional/secondary the way it is on Windows** —
since the bindings live inside a Steam-managed compatdata folder keyed by AppID, BindForge
*must* know the Steam library path (and therefore parse `libraryfolders.vdf`, since
`compatdata` lives under `<library>/steamapps/compatdata/<appid>/`, not always the default
library) to construct the Linux path at all. There is no registry to fall back on.

**Caveats:**
- `compatdata` can live in any Steam library, not just the default one — another reason
  `libraryfolders.vdf` parsing is required rather than assuming a fixed path.
- The Wine prefix is tied to the Linux user who installed/ran the game; multi-user Linux
  machines complicate "find every install" further, though this is an edge case unlikely to
  matter for a single-user desktop companion app.
- Frontier-launcher-direct and Epic installs are not meaningfully supported on Linux outside
  of community Wine setups (per the Frontier Forums Wine thread found in this research); Proton
  is the realistic Linux path.

Sources:
- [Elite Dangerous and GNU/Linux — Steam Community Guide](https://steamcommunity.com/sharedfiles/filedetails/?id=1584261209)
- [How to install ED on Linux using Wine — Frontier Forums](https://forums.frontier.co.uk/threads/how-to-install-ed-on-linux-using-wine-experimental-not-officially-supported.366894/page-46)
- [Always store compatdata in the internal Steam library — steam-for-linux GitHub issue](https://github.com/ValveSoftware/steam-for-linux/issues/12030)
- [ProtonDB — Elite Dangerous](https://www.protondb.com/app/359320)

---

## 6. Practical Fallback (when automatic detection isn't reliable)

Given the caveats above — particularly the unverified Epic bindings-separation claim, VDF
format drift across Steam versions, and the lack of an authoritative Frontier manifest — a
robust BindForge implementation should treat automatic detection as a **convenience, not a
guarantee**, with a manual fallback that is always available:

1. **Primary path:** On startup (or on a user-triggered "detect installs" action), scan
   `%LOCALAPPDATA%\Frontier_Developments\Products\*\ControlSchemes\` directly. This requires
   no storefront-specific code at all and covers the overwhelming majority of Windows users
   regardless of which launcher they use, per the "Key finding" above.

2. **Confirmatory/secondary path:** Optionally cross-check against Steam's
   `libraryfolders.vdf` (does `apps` contain `359320`?) and Epic's manifest folder (does any
   `.item` match Elite Dangerous?) to label *which* storefronts are installed, purely for
   display purposes ("Found: Steam, Frontier Launcher") — not as a requirement for locating
   the bindings folder.

3. **Manual fallback — always present, not just a last resort:** A "Browse…" picker that lets
   the user point directly at a `ControlSchemes` folder (or its parent `Products\<name>\`
   folder) once. The chosen path is then persisted (the existing `player.bindings_dir` DB
   column, per `EXISTING_BINDFORGE_AUDIT.md`, already exists for exactly this purpose) so the
   user is never asked twice for the same install. This is the right default behavior for:
   - Linux/Proton users where automatic compatdata discovery fails (non-default Steam library,
     unusual Wine setup).
   - The unverified Epic-separate-bindings case, if it turns out to be real.
   - Any future Frontier path-naming change that breaks a hardcoded assumption.

4. **Multiple product folders:** If more than one `Products\<name>\ControlSchemes\` folder is
   found (e.g. both `elite-dangerous-64` and `elite-dangerous-odyssey-64` exist from an old
   pre-Odyssey-merge install), surface all of them to the user as named choices rather than
   silently picking one — avoids silently editing the wrong product's bindings.

This mirrors the existing `BindingsApplyService` design philosophy already in the codebase
(per `EXISTING_BINDFORGE_AUDIT.md`): never write blind, always let the user confirm before
the working copy touches the real game file.

---

## Summary of Answers

| Question | Answer |
|---|---|
| 1. Registry — Steam | `HKLM\SOFTWARE\(Wow6432Node\)Valve\Steam\InstallPath` gives Steam's own folder, not game paths. Not a reliable source for per-game install location; use it only to bootstrap finding `steamapps\`. |
| 1. Registry — Epic | No documented reliable per-game registry path. Use the manifest files instead (§3). |
| 2. `libraryfolders.vdf` | Yes — reliable, documented, parseable VDF/KeyValues format at `<Steam>\config\libraryfolders.vdf`. Check each library's `apps` map for AppID `359320`. Tolerate older flat-list format. |
| 3. Epic manifests | Yes — `.item` JSON files under `C:\ProgramData\Epic\EpicGamesLauncher\Data\Manifests\`, each with an `InstallLocation` field. Reliable and documented. |
| 4. Frontier-specific config | None found. Not needed — the AppData `Products\*\ControlSchemes\` path is storefront-agnostic and is itself the answer for Windows. |
| 5. Practical fallback | Always offer a manual "Browse…" picker backed by the existing `player.bindings_dir` DB column, in addition to automatic scanning — treat automation as convenience, not guarantee, especially for Linux/Proton and the unverified Epic case. |

---

## Existing Codebase Findings

**Scope:** Read-only investigation of the actual EliteIntel Java codebase
(`Z:\EliteIntelTesting\EliteIntel`), branch `V1.1-KAN-6-push-to-talk-dawntreader`, performed to
validate/refine the web-research conclusions above against what is actually implemented today.
No source files were modified. All code below is quoted verbatim from the repository.

### 1. BindingsLoader / AppPaths

Two distinct classes exist and they answer two different questions — this is a naming trap
worth flagging up front:

- **`elite.intel.util.AppPaths`** (`app/src/main/java/elite/intel/util/AppPaths.java`) —
  resolves paths for **EliteIntel's own** data (its SQLite DB, its custom-commands JSON, its
  bindings *working-copy/backup* directories, its TTS/native-lib/STT model dirs). It has
  **nothing to do with locating the game's `.binds` files or install location.**
  `getBindingsWorkingDir()` / `getBindingsBackupDir()` (referenced in
  `EXISTING_BINDFORGE_AUDIT.md`) live here and resolve to EliteIntel's *own* AppData folder:

  ```java
  /** Returns the directory for per-preset working copies, creating it if needed. */
  public static Path getBindingsWorkingDir() throws IOException {
      Path dir = getAppDataBase().resolve("elite-intel/bindings");
      Files.createDirectories(dir);
      return dir;
  }

  /** Returns the directory for timestamped game-file backups, creating it if needed. */
  public static Path getBindingsBackupDir() throws IOException {
      Path dir = getAppDataBase().resolve("elite-intel/bindings/backups");
      Files.createDirectories(dir);
      return dir;
  }

  private static Path getAppDataBase() throws IOException {
      if (OsDetector.getOs() == OsDetector.OS.LINUX || OsDetector.getOs() == OsDetector.OS.MAC) {
          String dataHome = System.getenv("XDG_DATA_HOME");
          return dataHome != null && !dataHome.isEmpty()
                  ? Path.of(dataHome)
                  : Path.of(System.getProperty("user.home"), ".local/share");
      } else if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
          String localAppData = System.getenv("LOCALAPPDATA");
          if (localAppData == null || localAppData.isEmpty()) {
              throw new IllegalStateException("LOCALAPPDATA not set");
          }
          return Path.of(localAppData);
      }
      throw new IllegalStateException("Unsupported OS");
  }
  ```

  Note `getAppDataBase()` *does* build `%LOCALAPPDATA%` dynamically via
  `System.getenv("LOCALAPPDATA")` — but only to place EliteIntel's own working-copy/backup
  files, not to find Frontier's bindings folder. This is the only place in the codebase that
  reads `LOCALAPPDATA` dynamically at all.

- **`elite.intel.ai.hands.BindingsLoader`** (`app/src/main/java/elite/intel/ai/hands/BindingsLoader.java`)
  is the class that actually finds the **game's** `.binds` file. It does **not** locate the
  bindings folder itself — it takes the folder as a given:

  ```java
  public File getLatestBindsFile() throws Exception {
      Path bindingsDir = PlayerSession.getInstance().getBindingsDir();

      String presetName = findActivePresetName(bindingsDir);
      if (!presetName.isEmpty()) {
          Optional<Path> matched = Files.list(bindingsDir)
                  .filter(p -> {
                      String name = p.getFileName().toString();
                      return name.startsWith(presetName + ".") && name.endsWith(".binds");
                  })
                  .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
          ...
      }
      // Fallback: most recently modified .binds file
      ...
  }
  ```

  Within the folder, it determines which preset is *active* by reading
  `StartPreset.*.start` (e.g. `StartPreset.4.start` — a file Frontier writes containing the
  active preset name on repeated lines) and picks the newest `.binds` file whose name starts
  with that preset name; if no `StartPreset` file is found it falls back to the most-recently-
  modified `.binds` file in the directory. This logic is unrelated to install-location
  discovery — it assumes the directory is already correct.

  **The actual "locate the bindings folder" logic lives in `elite.intel.session.PlayerSession`**,
  specifically `getBindingsDir()` (`app/src/main/java/elite/intel/session/PlayerSession.java:679-690`):

  ```java
  public Path getBindingsDir() {
      return Database.withDao(PlayerDao.class, dao -> {
          String directory = trimToNull(dao.get().getBindingsDirectory());
          if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
              return directory == null ? Paths.get(System.getProperty("user.home"), "AppData", "Local", "Frontier Developments", "Elite Dangerous", "Options", "Bindings") : Paths.get(directory);
          } else if (OsDetector.getOs() == OsDetector.OS.LINUX) {
              return directory == null ? Paths.get(System.getProperty("user.home"), ".var", "app", "elite.intel.app", "ed-bindings") : Paths.get(directory);
          } else {
              return directory == null ? Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Frontier Developments", "Elite Dangerous", "Options", "Bindings") : Paths.get(directory);
          }
      });
  }
  ```

  **Key findings:**

  - **The Windows default path is hardcoded as a literal string**, built from
    `System.getProperty("user.home")` + literal segments `"AppData", "Local", "Frontier
    Developments", "Elite Dangerous", "Options", "Bindings"` — i.e.
    `%USERPROFILE%\AppData\Local\Frontier Developments\Elite Dangerous\Options\Bindings`. It is
    **not** built from `System.getenv("LOCALAPPDATA")` (unlike `AppPaths.getAppDataBase()`,
    which does use the env var, but for an unrelated purpose — see above). It also does not
    point at the `Products\<name>\ControlSchemes\` structure described in the web research at
    all — it points at a single flat `...\Elite Dangerous\Options\Bindings` directory.
  - **It does not handle multiple Products at all.** There is no `elite-dangerous-64` /
    `elite-dangerous-odyssey-64` branching, no enumeration of a `Products\` parent folder, and
    no mechanism to pick between them. The path is a single fixed string per OS.
  - **It is not auto-detected — the default is a guess, not a discovery mechanism.** If
    `player.bindings_dir` (the DB column) is unset, the method returns the hardcoded guess
    above unconditionally. There is no filesystem probing, no registry lookup, no VDF/manifest
    parsing anywhere in this method or anywhere upstream of it.
  - **Non-Windows handling exists but is unconventional.** There is a `LINUX` branch and a
    `MAC` branch with their own hardcoded defaults — but the Linux default
    (`~/.var/app/elite.intel.app/ed-bindings`) is **not** a real Elite Dangerous path of any
    kind (it looks like a Flatpak-sandbox path for EliteIntel itself, not for the game, and
    does not resemble the Proton/`compatdata` path the web research identified as the actual
    Linux location). The `MAC` branch is unreachable dead code: `OsDetector.getOs()`
    (`app/src/main/java/elite/intel/util/OsDetector.java`) only ever returns `WINDOWS` or
    `LINUX`:

    ```java
    public static OS getOs() {
        return OS.LINUX.getOs().equals(os) ? OS.LINUX : OS.WINDOWS;
    }
    ```

    Any non-Linux OS (including real macOS) falls through to `WINDOWS`. So in practice there
    are only two live code paths: the Windows literal-string guess, and a Linux literal-string
    guess that does not match how Elite Dangerous on Linux/Proton actually stores bindings.

### 2. `player.bindings_dir`

- **Schema:** confirmed in `app/src/main/resources/db-migration/00004__schema.sql`:
  `alter table player add bindings_dir text;`
- **DAO:** `elite.intel.db.dao.PlayerDao` (`app/src/main/java/elite/intel/db/dao/PlayerDao.java`).
  The inner `Player` class holds `private String bindingsDirectory;` with
  `getBindingsDirectory()`/`setBindingsDirectory(String)`. It is included in the single
  `INSERT OR REPLACE INTO player (...)` upsert statement (`:bindingsDirectory` placeholder,
  column `bindings_dir`) and read back in `PlayerMapper.map()`:
  `p.setBindingsDirectory(rs.getString("bindings_dir"));`. There is no dedicated
  read-only/write-only query for this single column — it always goes through the full
  `Player` row get/save.
- **Session wrapper:** `elite.intel.session.PlayerSession` exposes
  `setBindingsDir(String path)` (writes via `Database.withDao(PlayerDao.class, ...)`, calling
  `player.setBindingsDirectory(path)` then `dao.save(player)`) and `getBindingsDir()` (reads the
  column, falls back to the hardcoded per-OS default described in §1 above if null/blank —
  uses a `trimToNull` helper so whitespace-only values are also treated as "unset").
- **Every call site found** (whole-repo grep for `getBindingsDir`, `setBindingsDir`,
  `bindings_dir`, `getBindingsDirectory`, `setBindingsDirectory`):
  - `PlayerDao.java` — DAO definition (`Player` inner class, `PlayerMapper`, SQL upsert) as above.
  - `PlayerSession.java` — `getBindingsDir()` / `setBindingsDir()` as above.
  - `BindingsLoader.java` — `getLatestBindsFile()` and `getActivePresetName()` both call
    `PlayerSession.getInstance().getBindingsDir()` to know where to look for `.binds`/
    `StartPreset.*.start` files.
  - `BindingsMonitor.java` — `startMonitoring()`:
    `this.bindingsDir = PlayerSession.getInstance().getBindingsDir();` — the live file-watcher
    reads the same session-resolved path.
  - `BindingsTabPanel.java` — UI consumer/writer, see below.
  - i18n property files (`gui.properties`, `gui_de.properties`, `gui_es.properties`,
    `gui_fr.properties`, `gui_ru.properties`, `gui_uk.properties`) — only contain the
    user-facing label strings (`player.bindingsDirectory.dialog`, etc.), no logic.
- **UI flow exists and is exactly the manual "Browse…" picker the web research recommended as
  a fallback** — except here it is the *only* mechanism, not a fallback to an automatic one.
  In `app/src/main/java/elite/intel/ui/screen/BindingsTabPanel.java`:

  ```java
  private void selectBindingsDirectory() {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setDialogTitle(getText("player.bindingsDirectory.dialog"));
      String current = playerSession.getBindingsDir().toString();
      if (!current.isBlank())
          chooser.setCurrentDirectory(new File(current).getParentFile());
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          String path = chooser.getSelectedFile().getAbsolutePath();
          playerSession.setBindingsDir(path);
          bindingsDirField.setText(path);
          initData();
      }
  }
  ```

  The bindings tab's profile card shows the resolved directory in a read-only text field
  (`bindingsDirField`, populated in `initData()` via
  `bindingsDirField.setText(playerSession.getBindingsDir().toString());`) next to a
  folder-picker button wired to `selectBindingsDirectory()` above.
- **Is the value auto-detected anywhere?** No. Confirmed by exhaustive call-site search: the
  only writer of `player.bindings_dir` in the entire codebase is
  `BindingsTabPanel.selectBindingsDirectory()`, which is a direct result of the user clicking
  the folder-picker button and choosing a directory in a `JFileChooser`. There is no code path
  that probes the filesystem, environment, registry, or any manifest to populate this column
  automatically — it is purely user-entered, with a hardcoded per-OS guess as the fallback
  display value when unset (see §1).
- **Is it used by the bindings pipeline, or dead?** Fully used, not dead. It is the single
  source of truth that both `BindingsLoader` (file selection / active-preset detection) and
  `BindingsMonitor` (live file-watching for the executor + conflict/missing-binding checks)
  depend on via `PlayerSession.getBindingsDir()`. Every read/write/parse operation in the
  existing bindings system (`BindingsLoader`, `BindingsMonitor`, `BindingsTabPanel`,
  `BindingsWorkingCopyRepository`, `BindingsApplyService`) ultimately roots its file lookups in
  this one resolved path.

### 3. ControlSchemes / DeviceMappings.xml / .buttonMap

Searched the **entire repository** (not just bindings-related packages; both `app/` source and
the rest of the repo tree) for each of the following patterns, case-insensitive:
`ControlSchemes`, `DeviceMappings`, `.buttonMap`/`buttonMap`, `DeviceButtonMaps`.

- **Inside `app/` (all actual Java/resource source code): zero matches for all four patterns.**
  Re-confirmed with a scoped grep restricted to `app/` after the whole-repo pass returned only
  documentation hits, specifically to rule out a stale/cached false-positive: no `.java`,
  `.properties`, `.sql`, or other resource file under `app/` references `ControlSchemes`,
  `DeviceMappings`, `buttonMap`, or `DeviceButtonMaps` in any form.
- **Repo-wide, the only matches are documentation/data artifacts, not code:**
  - `dawntreader-docs/Reports/INSTALLATION_DISCOVERY_RESEARCH.md` — this report itself (the
    web-research content above).
  - `dawntreader-docs/Reports/BindForge_Punch_List.md`,
    `dawntreader-docs/Reports/BINDFORGE_SUBGROUPS_AUDIT.md`,
    `dawntreader-docs/BindForge_BindsFileAudit_v2.md`,
    `dawntreader-docs/Specifications/BindForge_Spec.md` — planning/spec documents (not yet
    implemented).
  - `dawntreader-docs/Actual Game Files/DeviceButtonMaps/Readme.txt` — this is a **reference
    copy of real game data** (a sample/specimen `DeviceButtonMaps` folder with its own
    `Readme.txt`, presumably copied from a real Elite Dangerous AppData folder for audit
    purposes), not application code. It confirms the `DeviceButtonMaps` folder is a real
    on-disk artifact worth handling eventually, but there is no parser, model, or loader for it
    anywhere in `app/`.
- **Conclusion: confirmed with high confidence — none of `ControlSchemes`,
  `DeviceMappings.xml`, `.buttonMap`, or `DeviceButtonMaps` are referenced, parsed, or handled
  anywhere in the EliteIntel application code today.** The existing bindings pipeline
  (`KeyBindingsParser`, `BindingsLoader`, `BindingsMonitor`, `BindingsWriter`,
  `BindingsWorkingCopyRepository`, `BindingsApplyService`) operates exclusively on the flat
  `Options\Bindings\*.binds` + `StartPreset.*.start` files and has no awareness that
  `ControlSchemes`/`DeviceMappings.xml`/`DeviceButtonMaps` exist as sibling data under the same
  Frontier Products folder.

### 4. Steam/Epic/registry/VDF detection code

Searched the whole repository for: `Steam`, `steamapps`, `libraryfolders`, `vdf`/`VDF`, `Epic`,
`EpicGames`, `.item` manifest references, Windows registry access patterns (`Advapi32`,
`WinReg`, `Preferences.userRoot`, `reg.exe`/`reg query`, raw `HKEY_` strings), and any
"GameProcess"/"ProcessDetector"/"IsRunning"-style class for detecting whether Elite Dangerous
itself is running.

- **Steam/Epic/VDF: zero matches anywhere in `app/` source.** A repo-wide grep for
  `Steam|steamapps|libraryfolders|EpicGames|Wow6432Node` scoped to `app/` returned no files.
  The only repo-wide hits for "Steam" are in `distribution/installer.sh` (the *EliteIntel
  installer's own* Linux shell script, which has logic to detect whether **Steam itself** is
  installed via SNAP packaging — for placing EliteIntel's own desktop shortcut — completely
  unrelated to detecting an Elite Dangerous game install or its bindings) and in
  `dawntreader-docs/` planning documents. Confirmed via git log
  (`6ce69151 Fixed English typos. Fixed shortcut creation logic Fixed symlink logic Fixed case
  when Steam is installed via SNAP.`) that this Steam reference is about the app installer, not
  game-install discovery.
- **Registry access: no Steam/Epic-related registry code exists.** The only registry-adjacent
  or low-level Windows API code in the codebase is unrelated to install discovery:
  - `elite.intel.util.AppPaths.toNativePath()` uses JNA's `Kernel32.GetShortPathNameW` to
    convert a path to its 8.3 short form (for non-ASCII usernames breaking native STT/TTS
    libraries) — no registry access, no Steam/Epic relevance.
  - `elite.intel.ui.support.GameWindowActivator` uses JNA's `User32` (`EnumWindows`,
    `SetForegroundWindow`, `ShowWindow`, `BringWindowToTop`, `GetWindowText`) to find and
    foreground the Elite Dangerous **window** by matching its title against
    `"elite - dangerous"` / `"elite dangerous"` substrings — this is **window-detection**, not
    process or install-path detection, and reveals nothing about install location. It is the
    closest thing in the codebase to "detect whether Elite Dangerous is running," and it works
    purely by enumerating top-level window titles, not by reading any registry key, process
    list, or file path.
  - `elite.intel.ai.hands.WindowsNativeKeyInput` / `LinuxX11NativeKeyInput` use JNA for
    simulated key input dispatch (`KeyCaptureMapper`), again unrelated to install discovery.
  - No `Advapi32`, `WinReg`, `Preferences.userRoot()`, `reg.exe`/`ProcessBuilder("reg", ...)`,
    or literal `HKEY_` string appears anywhere in `app/` outside of this report and the
    `INSTALLATION_DISCOVERY_RESEARCH.md` document itself.
- **No "is the game process running" detector exists** beyond the window-title check in
  `GameWindowActivator` above. There is no `ProcessHandle`/`tasklist`/WMI-based check for an
  `EliteDangerous64.exe` (or similar) process anywhere in the codebase.
- **Journal file discovery uses the exact same pattern as bindings discovery — confirmed by
  direct comparison.** `PlayerSession.getJournalPath()`
  (`app/src/main/java/elite/intel/session/PlayerSession.java:657-668`), read immediately
  adjacent to `getBindingsDir()` in the same class, is structurally identical: a DB-column
  override (`player.journal_dir`, read via `PlayerDao.getJournalDirectory()`) falling back to
  a **hardcoded per-OS literal path** when unset:

  ```java
  public Path getJournalPath() {
      return Database.withDao(PlayerDao.class, dao -> {
          String directory = trimToNull(dao.get().getJournalDirectory());
          if (OsDetector.getOs() == OsDetector.OS.WINDOWS) {
              return directory == null ? Paths.get(System.getProperty("user.home"), "Saved Games", "Frontier Developments", "Elite Dangerous") : Paths.get(directory);
          } else if (OsDetector.getOs() == OsDetector.OS.LINUX) {
              return directory == null ? Paths.get(System.getProperty("user.home"), ".var", "app", "elite.intel.app", "ed-journal") : Paths.get(directory);
          } else {
              return directory == null ? Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Frontier Developments", "Elite Dangerous") : Paths.get(directory);
          }
      });
  }
  ```

  This is consumed by `HistoricalMissionScanner.java` (`Path journalDir =
  PlayerSession.getInstance().getJournalPath();`), `JournalParser.java` (`journalDir =
  PlayerSession.getInstance().getJournalPath();`), and `AuxiliaryFilesMonitor.java`
  (`this.directory = PlayerSession.getInstance().getJournalPath();`). There is a matching
  manual "Browse…" picker for this one too, in
  `app/src/main/java/elite/intel/ui/screen/settings/CommonSettingsPanel.java` (a *different*
  settings panel than `BindingsTabPanel`, which has its own picker for bindings) — same
  `JFileChooser` + `playerSession.setJournalPath(path)` pattern. **There is no dynamic
  AppData-path-building logic to "discover" here either** — both journal and bindings discovery
  are pure "hardcoded guess + manual override," not "find AppData dynamically via env var and
  confirm it's correct." The one place that *does* read `LOCALAPPDATA` dynamically
  (`AppPaths.getAppDataBase()`, §1) is unrelated — it locates EliteIntel's own data, not the
  game's.

### 5. Synthesis — sufficient, extend, or replace?

**Not sufficient as-is for BindForge's stated needs, and it needs both extension and a
partial fix, not a full replace.** The foundational pieces BindForge will build on
(`KeyBindingsParser`, `BindingsWriter`, `BindingsWorkingCopyRepository`, `BindingsApplyService`,
`BindingsMonitor`) are sound per the existing `EXISTING_BINDFORGE_AUDIT.md` assessment and out
of scope for this section. What *is* in scope here — install/bindings-folder discovery — is
thin and narrower than either the web research or the prior audit's framing assumed:

- **Confirms** the web research's core premise that there is currently **no automatic
  discovery mechanism of any kind** in this codebase — no Steam VDF parsing, no Epic manifest
  parsing, no registry probing, no `Products\*\` enumeration. The "practical fallback" the web
  research proposed (§6 of the original report: manual Browse picker backed by a persisted DB
  column) is not a fallback here — it is **already the entire and only mechanism**, for both
  bindings and journal directories. This validates that recommendation completely; it is already
  implemented and proven in production for the journal-dir case, so extending the *same* pattern
  for bindings is low-risk.
- **Contradicts/corrects** one specific assumption baked into the web research and into
  `EXISTING_BINDFORGE_AUDIT.md`'s framing: the actual hardcoded Windows default in
  `PlayerSession.getBindingsDir()` is
  `%USERPROFILE%\AppData\Local\Frontier Developments\Elite Dangerous\Options\Bindings`
  (note the **space** in `Frontier Developments`, and the flat `Elite Dangerous\Options\Bindings`
  suffix) — **not** the underscored `Frontier_Developments\Products\<product>\ControlSchemes\`
  structure the web research identified as where `ControlSchemes`/`DeviceMappings.xml`/
  `DeviceButtonMaps` actually live. Both paths may exist simultaneously on a real machine (one
  for `.binds`/`StartPreset` files, a sibling `Products\<name>\` tree for `ControlSchemes`) —
  this was not verified against a real installation in this pass, only against the code — but
  the codebase's current hardcoded guess does not point at, and has no knowledge of, the
  `Products\` tree at all. Any BindForge work that needs `ControlSchemes`/`DeviceMappings.xml`
  will need **new** discovery logic; it cannot reuse or extend `getBindingsDir()`'s existing
  default as a starting point without changing what that default points to, since today's
  `.binds` discovery and the `Products\` tree are different folders Frontier may or may not
  place under the same parent.
- **Refines** the web research's "multiple Products" framing: the current code doesn't merely
  fail to pick the right product automatically — it has **zero concept of "Products" or
  multiple installs at all**. It assumes exactly one bindings folder exists, full stop. There
  is no enumeration, no "found 2, ask the user" UI path like the one recommended in §6 point 4
  of the web research. If BindForge needs to support multiple product folders (e.g. legacy
  `elite-dangerous-horizons-64` + current `elite-dangerous-odyssey-64` side by side), that UI
  and the underlying multi-path data model do not exist yet anywhere — not in `PlayerSession`
  (single `bindings_dir` string column, not a list), not in `BindingsTabPanel` (single text
  field + single picker), not in the DB schema (`player.bindings_dir` is one `text` column).
- **Confirms** the web research's Linux/Proton concern, but reveals the current Linux default
  is worse than "untested" — it is **definitely wrong**. `~/.var/app/elite.intel.app/ed-bindings`
  is not a real Elite Dangerous path under any installation method (native, Wine, or Proton
  `compatdata`); it appears to be a placeholder/sandbox-style path for EliteIntel's own
  Flatpak packaging, not a guess at where Frontier puts bindings. Since `OsDetector` cannot
  distinguish macOS from Windows (it has only two live branches, falling through to `WINDOWS`
  for anything that isn't `Linux`), the `MAC` branches in both `getBindingsDir()` and
  `getJournalPath()` are unreachable dead code today — not a current correctness bug (no macOS
  users would hit it, since `getOs()` never returns `MAC`), but worth flagging since a literal
  reading of `PlayerSession` would suggest macOS support exists when it does not, in practice,
  get exercised.
- **Net assessment:** Extend, not replace, but the extension surface for install discovery
  specifically is much larger than "improve `BindingsLoader`" — there is effectively **no
  existing discovery code to extend**, only a manual-entry fallback UI pattern (proven safe and
  already shipping for journal-dir) and a single hardcoded guess used purely as a starting
  display value. A BindForge implementation should: (a) keep the existing manual
  Browse-picker-backed-by-DB-column pattern exactly as-is (do not replace
  `BindingsTabPanel.selectBindingsDirectory()` / `PlayerSession.getBindingsDir()` /
  `player.bindings_dir`'s role as the authoritative override), (b) add genuinely new discovery
  code — Steam `libraryfolders.vdf` parsing, Epic `.item` manifest parsing, and/or
  `%LOCALAPPDATA%\Frontier Developments\Products\*\` enumeration as described in the web
  research sections above — as a net-new "suggest a default" pass that runs before the picker
  is shown (or via a "Detect" button), never bypassing the manual override, and (c) treat
  `ControlSchemes`/`DeviceMappings.xml`/`DeviceButtonMaps` discovery as fully separate new work
  from `.binds`/`StartPreset` discovery, since the current code's only folder concept
  (`Options\Bindings`) does not naturally extend to cover them — a second persisted path (or a
  derivation rule validated against a real install) will be needed if BindForge's scope
  includes those files.

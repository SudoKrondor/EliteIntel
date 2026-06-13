# AppPaths Report

Every filesystem path produced by `elite.intel.util.AppPaths`
(`app/src/main/java/elite/intel/util/AppPaths.java`), with the exact folder names and how
each is constructed on Windows vs. Linux. OS branching is driven by
`elite.intel.util.OsDetector.getOs()` (see caveat at the bottom — `OS.MAC` is effectively
unreachable).

---

## 1. `APP_DIR` — the app's "home" directory (static, computed once at class load)

Set in a `static { ... }` initializer, independent of OS:

1. **Running from a JAR**: take the JAR file's own location
   (`AppPaths.class.getProtectionDomain().getCodeSource().getLocation()`), and if it ends in
   `.jar`, `APP_DIR` = that JAR's **parent directory**.
   - e.g. JAR at `.../distribution/elite_intel.jar` → `APP_DIR` = `.../distribution`
2. **Running from an IDE/exploded classes** (code source does not end in `.jar`): walk up
   from the code source location through parent directories until one is found containing
   `build.gradle`, `build.gradle.kts`, or `settings.gradle`. That directory becomes
   `APP_DIR`.
   - e.g. classes under `.../EliteIntel/app/build/classes/java/main/...` → walks up to
     `.../EliteIntel/app` (contains `app/build.gradle`) → `APP_DIR` = `.../EliteIntel/app`
3. **Final fallback** (any exception above): `APP_DIR` = current working directory
   (`Path.of(".").toAbsolutePath().normalize()`).

`getAppDirectory()` simply returns this cached `APP_DIR`. This value feeds the
distribution/native paths in section 4.

---

## 2. `getAppDataBase()` — OS-specific per-user data root (private helper)

Used by every "user data" path in section 3.

- **Linux** (and `OS.MAC`, see caveat): 
  - If env var `XDG_DATA_HOME` is set and non-empty → `Path.of($XDG_DATA_HOME)`
  - Otherwise → `Path.of(System.getProperty("user.home"), ".local/share")`
    i.e. `~/.local/share`
- **Windows**:
  - `Path.of($LOCALAPPDATA)`
  - Throws `IllegalStateException("LOCALAPPDATA not set")` if the env var is missing/empty.
- Any other OS → throws `IllegalStateException("Unsupported OS")`.

---

## 3. User-data paths (built on `getAppDataBase()`)

All of these create their parent directory tree via `Files.createDirectories(...)` before
returning (except where noted).

### 3.1 `getDatabasePath()`
```
getAppDataBase().resolve("elite-intel/db")   ← directory created
  .resolve("database.db")                    ← file path returned (not created)
```
- **Windows**: `%LOCALAPPDATA%\elite-intel\db\database.db`
- **Linux**: `$XDG_DATA_HOME/elite-intel/db/database.db`, or
  `~/.local/share/elite-intel/db/database.db` if `XDG_DATA_HOME` is unset

### 3.2 `getCustomCommandsFilePath()`
```
getAppDataBase().resolve("elite-intel/custom-commands")   ← directory created
  .resolve(CUSTOM_COMMANDS_FILE_NAME)                       // = "custom_commands.json"
```
- **Windows**: `%LOCALAPPDATA%\elite-intel\custom-commands\custom_commands.json`
- **Linux**: `~/.local/share/elite-intel/custom-commands/custom_commands.json`
  (or under `$XDG_DATA_HOME` if set)

`CUSTOM_COMMANDS_FILE_NAME` is the public constant `"custom_commands.json"`.

### 3.3 `getBindingsWorkingDir()`
```
getAppDataBase().resolve("elite-intel/bindings")   ← directory created and returned
```
- **Windows**: `%LOCALAPPDATA%\elite-intel\bindings`
- **Linux**: `~/.local/share/elite-intel/bindings` (or under `$XDG_DATA_HOME`)

### 3.4 `getBindingsBackupDir()`
```
getAppDataBase().resolve("elite-intel/bindings/backups")   ← directory created and returned
```
- **Windows**: `%LOCALAPPDATA%\elite-intel\bindings\backups`
- **Linux**: `~/.local/share/elite-intel/bindings/backups` (or under `$XDG_DATA_HOME`)

---

## 4. Distribution / native resource paths (built on `APP_DIR`)

These are OS-agnostic in *construction* — they don't branch on `OsDetector` — but their
final value depends on whether the app is running from a JAR or from a dev build (see
section 1), which in turn is where the platform-specific native libraries (e.g.
`distribution/native/sherpa-onnx`) live.

`getDistributionFile(subPath)` (private helper):
- If `isRunningFromJar()` is true → `APP_DIR.resolve(subPath)`
- Else → `APP_DIR.resolve("../distribution/" + subPath).normalize()`

`isRunningFromJar()` re-derives the code-source path and checks whether it ends in `.jar`.

### 4.1 `getTtsModelDir()` → `getDistributionFile("tts")`
- From JAR (`APP_DIR` = jar's parent, e.g. `distribution/`): `distribution/tts`
- From dev build (`APP_DIR` = `<repo>/app`): `<repo>/app/../distribution/tts` normalized →
  `<repo>/distribution/tts`

### 4.2 `getNativeLibDir()` → `getDistributionFile("native")`
- From JAR: `distribution/native`
- From dev build: `<repo>/distribution/native`

### 4.3 `getParakeetModelDir()` → `getDistributionFile("parakeet")`
- From JAR: `distribution/parakeet`
- From dev build: `<repo>/distribution/parakeet`

None of these three call `Files.createDirectories` — they're read-only resource locations
expected to already exist in the distribution layout.

---

## 5. `getSecretKeyFile()` — encryption key file (returns `String`, not `Path`)

This method does **not** use `getAppDataBase()` — it has its own independent OS branch built
with `File.separator`:

- **Linux/Mac** (`OsDetector.getOs() == LINUX` or `MAC`):
  ```
  <user.home>/.local/share/elite-intel/secret.key
  ```
  (`System.getProperty("user.home")` + `/.local/share/elite-intel/secret.key`)
- **Windows** (everything else):
  ```
  <LOCALAPPDATA>/elite-intel/secret.key
  ```
  (`System.getenv("LOCALAPPDATA")` + `/elite-intel/secret.key`)

**Note**: unlike `getAppDataBase()`, this Linux/Mac branch always uses
`~/.local/share/elite-intel/secret.key` and does **not** check `$XDG_DATA_HOME`. On a system
where `XDG_DATA_HOME` is set to something other than `~/.local/share`, the secret key would
live in a different directory tree than the database/bindings/custom-commands files from
section 3.

---

## 6. `toNativePath(Path path)` — path transformation, not a defined location

- **Windows**: converts the absolute path to its 8.3 short-form name via
  `Kernel32.GetShortPathNameW` (JNA), to work around native libraries (e.g. sherpa-onnx) that
  can't handle non-ASCII characters in paths (e.g. a non-Latin Windows username). Falls back
  to the original absolute path string if the native call fails for any reason.
- **Linux/Mac** (`os.name` doesn't contain `"win"`): no-op, returns
  `path.toAbsolutePath().toString()` unchanged.

---

## Summary table

| Method | Windows | Linux (default, no `XDG_DATA_HOME`) |
|---|---|---|
| `getDatabasePath()` | `%LOCALAPPDATA%\elite-intel\db\database.db` | `~/.local/share/elite-intel/db/database.db` |
| `getCustomCommandsFilePath()` | `%LOCALAPPDATA%\elite-intel\custom-commands\custom_commands.json` | `~/.local/share/elite-intel/custom-commands/custom_commands.json` |
| `getBindingsWorkingDir()` | `%LOCALAPPDATA%\elite-intel\bindings` | `~/.local/share/elite-intel/bindings` |
| `getBindingsBackupDir()` | `%LOCALAPPDATA%\elite-intel\bindings\backups` | `~/.local/share/elite-intel/bindings/backups` |
| `getSecretKeyFile()` | `%LOCALAPPDATA%\elite-intel\secret.key` | `~/.local/share/elite-intel/secret.key` (ignores `XDG_DATA_HOME`) |
| `getTtsModelDir()` | `<jar-dir>/tts` or `<repo>/distribution/tts` | same, OS-independent |
| `getNativeLibDir()` | `<jar-dir>/native` or `<repo>/distribution/native` | same, OS-independent |
| `getParakeetModelDir()` | `<jar-dir>/parakeet` or `<repo>/distribution/parakeet` | same, OS-independent |

---

## Caveat: `OS.MAC` is effectively unreachable

`AppPaths.getAppDataBase()` and `getSecretKeyFile()` both have explicit `OS.MAC` branches
(grouped with Linux), but `OsDetector.getOs()` is implemented as:

```java
public static OS getOs() {
    return OS.LINUX.getOs().equals(os) ? OS.LINUX : OS.WINDOWS;
}
```

i.e. it only ever returns `LINUX` (if `os.name` lowercases to exactly `"linux"`) or
`WINDOWS` (for everything else, including macOS's `"mac os x"`). So on an actual Mac,
`getOs()` returns `WINDOWS`, and `getAppDataBase()`/`getSecretKeyFile()` would take the
Windows branch and look for `%LOCALAPPDATA%` — which doesn't exist on macOS. The `OS.MAC`
enum constant and its branches in `AppPaths` are currently dead code given this detector
logic.

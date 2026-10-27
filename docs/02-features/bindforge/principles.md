# BindForge — Principles

**Settled 2026-10-22 (Alan).** These principles come first: they win over every other BindForge document. A rule,
spec section or design that breaks a principle is wrong until Alan changes the principle itself. **Where any two
other documents disagree, the work stops and Alan decides.** How the files are handled is in [storage.md](storage.md).

## Words

- **Game input configuration files** — these, and only these (2026-10-23, Alan):
  - from the bindings folder (on Windows `%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\`,
    on Linux one per installation): **`StartPreset.4.start`**, exactly one, and **every
    `<name>.<major>.<minor>.binds`**, one or more;
  - from each installation's `Products\elite-dangerous-odyssey-64\ControlSchemes\`: **`DeviceMappings.xml`**,
    exactly one per installation, and **every `DeviceButtonMaps\<device name>.buttonMap`**, none or more — the
    user's own, BindForge's generated ones and the two Frontier ships (`VPCPanel`, `VPCThrottle`).

  Not included: the factory binds files in `ControlSchemes`, a leftover `StartPreset.start`, `.binds.backup` copies,
  `BindingLoadingErrors.log`, and anything else in either folder.
- **Working copy** — those files in Elite-Intel's BindForge folder: uncompressed, and the user can open them in the
  file manager. The source of truth once first setup is complete. **Edits stay on screen until APPLY. Only four things write
  the working copy — APPLY, first setup, RESTORE, and the check's reconcile — and each leaves the working copy and
  the live copies matching** (reworded 2026-10-24, Alan). So the working copy is always what the game's files should
  hold.
- **Live copy** — those files where the game reads them: the bindings folder for `.binds` and `StartPreset.N.start`,
  and each installation's `ControlSchemes` folder for `DeviceMappings.xml` and the `.buttonMap`s. Uncompressed, and
  the user can open them.
- **Backup** — a zip of the live copies, named with its date stamp, in Elite-Intel's BindForge backup folder — **or a
  zip of the working copy**, made the same way, before a live copy replaces it (2026-10-23, Alan). Every backup is a
  zip, so there is one format to restore (2026-10-24, Alan). There are four kinds — **daily**, **before changes**,
  **user** and **first setup** — each kept by its own rule ([storage.md](storage.md) rules 11–12).
- **Edits not applied** — changes on the editor screens that APPLY has not written yet. They live only on screen.
- **"Master" and "draft" are retired.** Older documents use them: *draft* means the editor screens' edits not
  applied, and *master* means the working copy.

## Principles

1. **The four file types are one group.** `.binds`, `StartPreset.N.start`, `DeviceMappings.xml` and every `.buttonMap`
   are always treated together in every file operation: backup, check, apply, restore.
2. **Protect the user's configuration at all cost.** If a game update overwrites any of it and the user loses their
   setup, BindForge failed.
3. **Back up before every write.** Nothing reaches the game's files, and nothing touches them at all, until a backup
   of everything exists.
4. **Nothing is written to the game's files unless the user asks.** APPLY and RESTORE are the user asking.
5. **Offered, never forced.** *"We build BindForge to help when asked, not force a situation on the user."* The one
   thing BindForge does unasked is the silent backup when Elite-Intel starts (2026-10-23).
6. **Ask, don't guess.** Where BindForge cannot know the answer — which device is which, which version to keep — it
   shows what it found and the user decides.
7. **One source of truth: the working copy.**
8. **Every installation gets the same device files.**
9. **Never fail silently.** A write that did not land is reported, naming what failed, and the user is told what to
   do.
10. **Simple enough to understand.** If a user cannot follow what BindForge did to their files, it is wrong, however
    clever it is. Krondor: *"In terms of user experience, anything that confusing will create a lot of support
    requests, and mistakes. In terms of internal impl, it should be robust enough to support the feature, and
    bullet-proof enough to not hide bugs. Bugs with binds can lead to a very bad reputation."*
11. **Every new rule is checked against these principles before it goes into a spec.**
12. **Editors care what you're editing; only File Manager cares where the files are** (2026-10-24). Alias Designer,
    Bind Editor and Preset Editor show no installations, folders or per-installation states. Installations live in
    File Manager's Game Install Locations.

# StarVizion Prototype Branch & Main-UI Tab Removal

## Summary

Split off a dedicated branch that preserves the full StarVizion overlay feature (including the
just-completed `elite.intel.devices` refactor), then hid the StarVizion tab from the main app
window on the working branch — without deleting any StarVizion code.

## 1. Checkpoint commit

The working tree on `V1.1-KAN-57-elite-intel-devices-dawntreader` had uncommitted changes from
the prior session's `elite.intel.devices` extraction (new `elite.intel.devices` package,
StarVizion Vizlets/dialogs and `InputSettingsPanel` migrated to it, old `starvizion` `Sv*`/
`SdlInputService` classes removed) plus the `DEVICES_BUILD_REPORT.md` and
`SESSION_ANALYSIS.md` reports. These were committed as a checkpoint first:

- `34672d1a` — `feat(devices): extract elite.intel.devices package for shared SDL3 input`

Two unrelated uncommitted items found in the working tree were handled per your direction:
- `dawntreader-docs/Reports/SYNC_REPORT.md` — a working-tree edit that reverted this file to an
  older version was discarded (`git checkout --`), restoring the current committed content.
- `dawntreader-docs/Reports/BRANCH_SETUP_REPORT.md` (previously untracked) was included in the
  `34672d1a` checkpoint commit.

## 2. New branch: `V1.1-starvizion-prototype-dawntreader`

Created from `V1.1-KAN-57-elite-intel-devices-dawntreader` at commit `34672d1a` (before the tab
was hidden), so it retains StarVizion fully wired into the main UI plus the new
`elite.intel.devices` layer. Pushed to `origin` as a new branch.

## 3. StarVizion tab hidden on `V1.1-KAN-57-elite-intel-devices-dawntreader`

In `app/src/main/java/elite/intel/ui/view/AppView.java`, removed the single line that added the
StarVizion tab to the main `JTabbedPane`:

```java
tabs.addTab(getText("tab.starvizion"), null, starVizionTabPanel);
```

Nothing else changed:
- `starVizionTabPanel` is still constructed in `buildUi()` and still disposed in `rebuildUi()`
  — it's just never added to the visible tab strip, so the StarVizion tab no longer appears in
  the main window.
- No classes, packages, imports, or i18n keys (`tab.starvizion`, `starvizion.*`) were removed.
- The entire `elite.intel.starvizion` package and the new `elite.intel.devices` package are
  untouched and still compile/build as part of the app.

Committed as `35e6d5bb` — `chore(ui): hide StarVizion tab from main window`.

## 4. Build verification

```
./gradlew shadowJar
```

**BUILD SUCCESSFUL in 10s** (7 actionable tasks: 2 executed, 5 up-to-date).

## 5. Branches pushed to origin

```
70e27646..35e6d5bb  V1.1-KAN-57-elite-intel-devices-dawntreader -> V1.1-KAN-57-elite-intel-devices-dawntreader
 * [new branch]      V1.1-starvizion-prototype-dawntreader      -> V1.1-starvizion-prototype-dawntreader
```

- `V1.1-KAN-57-elite-intel-devices-dawntreader` — now at `35e6d5bb`, StarVizion tab hidden from
  the main window, StarVizion/devices code intact in the source tree.
- `V1.1-starvizion-prototype-dawntreader` — new branch at `34672d1a`, StarVizion tab still
  visible and active in the main window, serving as the preserved prototype reference.

The active working branch (`V1.1-KAN-57-elite-intel-devices-dawntreader`) was not switched at
any point.

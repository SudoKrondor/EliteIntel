# BindForge — Build Plan

**BindForge is built one slice per session, from the briefs in this folder.** A session reads a brief instead
of inheriting a long conversation, does one slice, records what the next session needs, and stops.

These are **not design documents.** The design lives in [`docs/02-features/bindforge/`](../02-features/bindforge/overview.md),
and a decision made while building is recorded *there*, in the spec it belongs to. A brief says what is built,
what comes next, where to read, and what to watch out for — it carries status, and status goes stale, which is
why it is kept apart from the specs.

## How a session runs

Start with **`/next-slice <section>`** — for example `/next-slice core`. The command holds the whole routine:

1. **Check the branch is current.** Krondor pushes to it several times a day. If it is behind, Alan pulls first.
2. **Read this file, then the section's brief.** Take the slice marked **next**.
3. **Read the spec sections the slice names, end to end** — the rule in `CLAUDE.md`, and the cheapest
   insurance there is.
4. **Show Alan the sources table and wait.** Every field and behaviour, and where its data comes from.
5. **Build that slice and nothing else.** Anything found out of scope goes in the brief's notes, not the code.
6. **Alan runs the tests** — Gradle does not run from these sessions. `.\gradlew app:test`
7. **When they pass:** tick the slice, add a hand-off note, mark the next slice, run `python scripts/check_docs.py`,
   and give Alan a commit message. **Alan commits** — never commit, push or merge.
8. **Stop.** Even with room to continue. Starting fresh is the point.

**Reviews run in their own fresh session:** `/check-code-integrity`, scoped to the section's own commits, so
Krondor's work is not reviewed as ours.

## Rules every section inherits

- **`CLAUDE.md`**, and [WORKING-RULES.md](../../WORKING-RULES.md) — spec first, sources table, terminology.
- **Shared code changes on `V1.1-Release`, via Krondor, and merges up.** That covers `bindforge.io`,
  `bindforge.rules`, `elite.intel.io` and `AppPaths` — anything both release lines call. A V1.2 feature that
  needs one of them is built on [the foundation branch](#the-foundation-branch--how-a-v12-feature-changes-a-file-v11-ships-krondor-2026-10-12),
  never worked around, and the slice waits until it has merged up. **A new class only V1.2 calls is
  not a change to shared code** and is built on `V1.2-BindForge` (Alan, 2026-10-11 — [the platform map](../01-host-integration/elite-intel-platform-map.md#the-shared-write-path-is-eliteintelio--built-2026-10-03)).
- **`elite.intel.ai.hands` is keystroke execution only.** BindForge code never goes there.
- **Migrations:** the next free number in BindForge's band, `12000–12499`. Never edit an applied one.
-
**Strings:** all seven `gui*.properties` bundles (RU and UK were dropped 2026-10-15). `getText` runs `MessageFormat` only when arguments are passed,
  so a literal apostrophe is doubled in a string that takes arguments and single in one that does not.
- **UI:** [the HUD canon](../02-features/bindforge/ui-component-map.md) — state is text colour, never a pill;
  `HudConfirmDialog`, never `JOptionPane`.
- **Docs:** `python scripts/check_docs.py` must report no errors.

## Sections, in build order

The spec's [phased rollout](../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) puts Alias Designer
and Preset Editor first, File Manager second, the Bind Editor third and the rename last. **Core** is not a tab:
it is the machinery every tab writes through, so it comes first and is built only as far as the next tab needs.

| # | Section | Brief | Spec | State |
|---|---|---|---|---|
| 0 | **Foundation** — the additions to shared V1.1 files that later slices need, on `V1.1-Release-bf-foundation`; built off `V1.1-Release`, merged by Krondor | [foundation.md](foundation.md) | the specs of the slices it unblocks | **done 2026-10-13**, merged by Krondor — F1 (four-line `StartPreset` read), F2 (`.binds` device rewrite), F3b (`applyFrom`), F4 (writer user-edit path); F3 settled, F5 withdrawn. Reopened only when a slice needs another change to a V1.1 file |
| 1 | **Core** — master and draft, Apply, Edit History filling, startup check | [core.md](core.md) | [overview.md](../02-features/bindforge/overview.md) | C1–C3, C5 and C6 done; C4 unblocked 2026-10-13; C7 waits on Preset Editor |
| 2 | **Alias Designer** | [alias-designer.md](alias-designer.md) | [alias-designer.md](../02-features/bindforge/alias-designer.md) | device list, naming rule, startup `.buttonMap`, first setup, the label merge, divergence against the master and its ways out, the Installations markers, the Built-in Devices tab, which axes and hats a controller has, the Device Editor, the actions (CLEAR, RESET LABELS, APPLY, DISCARD), the missing-controller warning, activation (A1b), onboarding (A6b), rename (A11), catch-up (A14), hot-plug (A7), when hardware changes (A12) built; **A15 next**; A16 and A17 ready |
| 3 | **Preset Editor** | *not yet written* | [preset-editor.md](../02-features/bindforge/preset-editor.md) | nothing built; unblocked 2026-10-13 — F1's four-line read merged up |
| 4 | **File Manager** — Edit History browsing, auto-backup and retention, restore scope | *not yet written* | [file-manager.md](../02-features/bindforge/file-manager.md) | Install Locations built; Player Backups manual only |
| 5 | **Bind Editor: editing** — Game Mode, Settings, capture dialog, mouse inputs | [bind-editor-editing.md](bind-editor-editing.md) | [bind-editor.md](../02-features/bindforge/bind-editor.md) | E1 (controller buttons and hats in the capture dialog) built 2026-10-15; **E2 next** |
| 6 | **Bind Editor: Anomalies** and shared conflict detection | *not yet written* | [bind-editor.md](../02-features/bindforge/bind-editor.md#anomalies) | scanner exists; three known defects |
| 7 | **Rename the section to BindForge** | — | [v1.2-scope.md](../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) | three keys across seven bundles, done by whichever session finishes last |

**A brief marked *not yet written* is written by the first session for that section** — it is that session's
first slice. Written the week its section starts, it is accurate; written months ahead, it would not be.

**The Bind Editor is two sections** because its spec is longer than the other three combined.

## The foundation branch — how a V1.2 feature changes a file V1.1 ships (Krondor, 2026-10-12)

**The question was:** Preset Editor's first job is reading all four lines of `StartPreset` —
`BindingsLoader.getActivePresetName()` reads only the first — and the Bind Editor widens `BindingsWriter` and
consolidates the conflict scanner. All of that is `bindforge.io` and `bindforge.rules`, which V1.1 also ships.
These are V1.2 features, not V1.1 fixes, so the shared-code rule did not cover them.

**The answer:** such changes are made on **`V1.1-Release-bf-foundation`**, a branch off `V1.1-Release`. Krondor
checks it and merges it into `V1.1-Release`, and it reaches V1.2 by the same merge-up as his fixes — so nobody
resolves a conflict in his files without knowing what he has in flight.

**What goes on it must be invisible to V1.1 users**, because it ships in his weekly releases (the terms it was
proposed on):

- **Additions only.** Existing methods do exactly what they did; a widened behaviour is a new method beside the
  old one, never a change to it.
- **No migrations, no UI, no strings, no new startup work.**
- **Nothing that depends on V1.2-only code** — `bindforge.devicefiles`, `bindforge.devices` and the rest do not
  exist there.
- **Tested on its own**, and V1.1's own tests still pass.

**What it unblocks**, each once its change has merged up to `V1.2-BindForge`: the four-line `StartPreset` read
(Preset Editor, Core C7); the `.binds` device rewrite (Alias Designer A6b, A11, and CLEAR on a device `.binds`
names); the writer's user-edit path (Bind Editor); the file-level part of the whole-file `.binds` master (Core C4),
scoped first. Its work is planned in its own brief — **section 0 below**.

## Writing a new brief

Same shape as [core.md](core.md): **spec** (what to read), **built** (with file paths), **settled** (with links to
where each ruling is recorded), **known issues**, **boundaries**, **slices** (ordered, each with a status), and
**hand-off notes**, newest first. Keep it under about two hundred lines; it is read at the start of every session.

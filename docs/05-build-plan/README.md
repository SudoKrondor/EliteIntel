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
  `bindforge.rules`, `elite.intel.io` and `AppPaths` — anything both release lines call. A slice that needs one
  of them is **blocked** until it is settled with him, not worked around.
- **`elite.intel.ai.hands` is keystroke execution only.** BindForge code never goes there.
- **Migrations:** the next free number in BindForge's band, `12000–12499`. Never edit an applied one.
- **Strings:** all nine `gui*.properties` bundles. `getText` runs `MessageFormat` only when arguments are passed,
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
| 1 | **Core** — master and draft, Apply, Edit History filling, startup check | [core.md](core.md) | [overview.md](../02-features/bindforge/overview.md) | C1–C3 done; C4, C5 blocked; C6 after Alias Designer A10 |
| 2 | **Alias Designer** | [alias-designer.md](alias-designer.md) | [alias-designer.md](../02-features/bindforge/alias-designer.md) | device list, naming rule, startup `.buttonMap`, first setup, the label merge and divergence against the master built; A3b next |
| 3 | **Preset Editor** | *not yet written* | [preset-editor.md](../02-features/bindforge/preset-editor.md) | nothing built |
| 4 | **File Manager** — Edit History browsing, auto-backup and retention, restore scope | *not yet written* | [file-manager.md](../02-features/bindforge/file-manager.md) | Install Locations built; Player Backups manual only |
| 5 | **Bind Editor: editing** — Game Mode, Settings, capture dialog, mouse inputs | *not yet written* | [bind-editor.md](../02-features/bindforge/bind-editor.md) | the grid exists in `BindingProfilePanel` |
| 6 | **Bind Editor: Anomalies** and shared conflict detection | *not yet written* | [bind-editor.md](../02-features/bindforge/bind-editor.md#anomalies) | scanner exists; three known defects |
| 7 | **Rename the section to BindForge** | — | [v1.2-scope.md](../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) | three keys across nine bundles, done by whichever session finishes last |

**A brief marked *not yet written* is written by the first session for that section** — it is that session's
first slice. Written the week its section starts, it is accurate; written months ahead, it would not be.

**The Bind Editor is two sections** because its spec is longer than the other three combined.

## One question to settle with Krondor before sections 3, 5 and 6

Preset Editor's first job is reading all four lines of `StartPreset` — `BindingsLoader.getActivePresetName()`
reads only the first — and the Bind Editor widens `BindingsWriter` and consolidates the conflict scanner. All of
that is `bindforge.io` and `bindforge.rules`, which V1.1 also ships to. These are V1.2 features, not V1.1 fixes,
so the shared-code rule does not obviously cover them. **How a V1.2 feature changes a file V1.1 also ships**
needs his answer before the first conflict, not after. Core C4 and C5 wait on the same answer.

## Writing a new brief

Same shape as [core.md](core.md): **spec** (what to read), **built** (with file paths), **settled** (with links to
where each ruling is recorded), **known issues**, **boundaries**, **slices** (ordered, each with a status), and
**hand-off notes**, newest first. Keep it under about two hundred lines; it is read at the start of every session.

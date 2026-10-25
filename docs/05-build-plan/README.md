# BindForge — Build Plan

**BindForge is built one slice per session, from the briefs in this folder.** A session reads a brief instead
of inheriting a long conversation, does one slice, records what the next session needs, and stops.

These are **not design documents.** The design lives in [`docs/02-features/bindforge/`](../02-features/bindforge/overview.md),
and a decision made while building is recorded *there*, in the spec it belongs to. A brief says what is built,
what comes next, where to read, and what to watch out for — it carries status, and status goes stale, which is
why it is kept apart from the specs.

## How a session runs

Start with **`/next-slice <section>`** — for example `/next-slice storage`. The command holds the whole routine:

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
- **Shared code is changed on `V1.2-BindForge`** (Krondor, 2026-10-20). `bindforge.io`, `bindforge.rules`,
  `elite.intel.io` and `AppPaths` are edited here like any V1.2 code; the merge-up rule and the foundation branch
  below are repealed ([CLAUDE.md](../../CLAUDE.md#branching-model)).
- **`elite.intel.ai.hands` is keystroke execution only.** BindForge code never goes there.
- **Migrations:** the next free number in BindForge's band, `12000–12499`. Never edit an applied one.
- **Strings:** all seven `gui*.properties` bundles (RU and UK were dropped 2026-10-15). `getText` runs `MessageFormat` only when arguments are passed,
  so a literal apostrophe is doubled in a string that takes arguments and single in one that does not.
- **UI:** [the HUD canon](../02-features/bindforge/ui-component-map.md) — state is text colour, never a pill;
  `HudConfirmDialog`, never `JOptionPane`.
- **Docs:** `python scripts/check_docs.py` must report no errors.

## Sections, in build order

**Since 2026-10-24 the plan is built from [storage.md](../02-features/bindforge/storage.md) and
[the principles](../02-features/bindforge/principles.md): back up, edit, apply.** One working copy is the source of
truth, edits stay on screen until one APPLY above BindForge's tabs, and one backup system protects everything.
**Storage** is not a tab: it is the machinery every tab writes through, and it comes first — each tab's remaining
work waits on the Storage slice that moves that tab onto the working copy.

| # | Section | Brief | Spec | State |
|---|---|---|---|---|
| 0 | **Foundation** — V1.2's additions to shared V1.1 files | [foundation.md](foundation.md) | the specs of the slices it unblocked | **closed 2026-10-20** — the foundation branch is repealed; F1–F4 merged. A record only |
| 1 | **Core** — master and draft, Apply, Edit History, startup check | [core.md](core.md) | — | **closed 2026-10-24** — replaced by Storage; its master, draft and Edit History are gone from the specs. A record only |
| 2 | **Storage** — backups, the working copy and first-time startup, RESTORE, the APPLY / DISCARD bar, the leave prompt, the check, CREATE, binds-file rename | [storage.md](storage.md) | [storage.md](../02-features/bindforge/storage.md), [principles.md](../02-features/bindforge/principles.md) | S1 (one backup system) done 2026-10-24; S1b (four kinds of backup, every one a zip) done 2026-10-25; **S2 (the working copy, and first-time startup) next**; S3–S12 pending |
| 3 | **Alias Designer** | [alias-designer.md](alias-designer.md) | [alias-designer.md](../02-features/bindforge/alias-designer.md) | built against the old model: device list, naming rule, first setup and the label merge, the Built-in Devices tab, which axes and hats a controller has, the Device Editor, CLEAR and RESET LABELS, onboarding, rename, shadowing a built-in, hot-plug, when hardware changes, the missing-controller warning. **Its mechanics move to the working copy in Storage S4**, which also removes the divergence list, the installation markers and catch-up. Then A19 (CLEAR moves bindings to a hardware ID); **A18 (gamepads) blocked** on a tester's `.binds` |
| 4 | **Preset Editor** | [preset-editor.md](preset-editor.md) | [preset-editor.md](../02-features/bindforge/preset-editor.md) | P1 (the BindName list) done; P2–P3's master, draft and merge replaced by Storage S6; **P4 (the screen) after S6**; P5 and P6 dropped — the check (S8) and the leave prompt (S7) replace them |
| 5 | **File Manager** | — its work is in [storage.md](storage.md) | [file-manager.md](../02-features/bindforge/file-manager.md) | backups S1 (done), Game Install Locations S2 and S9, RESTORE S3. No brief of its own |
| 6 | **Bind Editor: editing** — Game Mode, Settings, capture dialog, mouse inputs | [bind-editor-editing.md](bind-editor-editing.md) | [bind-editor.md](../02-features/bindforge/bind-editor.md) | E1 built 2026-10-15; **E2 after Storage S5**; E7 (gamepad capture) blocked on A18 |
| 7 | **Bind Editor: Anomalies** and shared conflict detection | *not yet written* | [bind-editor.md](../02-features/bindforge/bind-editor.md#anomalies) | scanner exists; three known defects |
| 8 | **Rename the section to BindForge** | — | [v1.2-scope.md](../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) | three keys across seven bundles, done by whichever session finishes last |

**Before V1.2 ships:** the reminders are in [storage.md — Before V1.2 ships](storage.md#before-v12-ships) — among
them, hide or remove the Run first-time startup again button.

**A brief marked *not yet written* is written by the first session for that section** — it is that session's
first slice. Written the week its section starts, it is accurate; written months ahead, it would not be.

**The Bind Editor is two sections** because its spec is longer than the other three combined.

## The foundation branch — how a V1.2 feature changes a file V1.1 ships (Krondor, 2026-10-12)

> **Repealed 2026-10-20 (Krondor: *"we are now solid on 1.2"*).** Shared files are changed on `V1.2-BindForge`.
> What follows is the history of F1-F4, kept because the specs cite it.

**The question was:** Preset Editor's first job is reading all four lines of `StartPreset.4.start` —
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

**What it unblocks**, each once its change has merged up to `V1.2-BindForge`: the four-line `StartPreset.4.start` read
(Preset Editor, Core C7); the `.binds` device rewrite (Alias Designer A6b, A11, and CLEAR on a device `.binds`
names); the writer's user-edit path (Bind Editor); the file-level part of the whole-file `.binds` master (Core C4),
scoped first. Its work is planned in its own brief — **section 0 below**.

## Writing a new brief

Same shape as [storage.md](storage.md): **spec** (what to read), **built** (with file paths), **settled** (with links to
where each ruling is recorded), **known issues**, **boundaries**, **slices** (ordered, each with a status), and
**hand-off notes**, newest first. Keep it under about two hundred lines; it is read at the start of every session.

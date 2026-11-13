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
- **Migrations:** the next free number on `V1.2` at merge time - `V1.2` uses the 12000 range too, so an unshipped
  BindForge migration that clashes is renumbered when merging (Alan, 2026-10-31). Never edit an applied one.
- **Strings:** all nine `gui*.properties` bundles (RU and UK, dropped 2026-10-15, restored on `V1.2` and merged 2026-10-31). `getText` runs `MessageFormat` only when arguments are passed,
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
| 2 | **Storage** — backups, the working copy and first-time startup, RESTORE, the APPLY / DISCARD bar, the leave prompt, the check, CREATE, binds-file rename | [storage.md](storage.md) | [storage.md](../02-features/bindforge/storage.md), [principles.md](../02-features/bindforge/principles.md) | S1 (one backup system) done 2026-10-24; S1b (four kinds of backup, every one a zip) done 2026-10-24; S2a (the working copy store and the write path) done 2026-10-24; S2b (first-time startup) done 2026-10-25; S3 (RESTORE) done 2026-10-25; S4a (the bar, Device Editor, onboarding) done 2026-10-26; S4R (Krondor's storage review) done 2026-10-27; S4b (identity, the old tables) done 2026-10-28; S5 (Bind Editor on the bar) done 2026-10-28; S6 (the preset file on the bar) done 2026-10-28; S6R (the S5/S6 review follow-up) done 2026-10-29; S6R2 (the S4R-S6R review follow-up) done 2026-10-30; S7 (leaving with edits not applied) done 2026-10-30; S8a (the check at the first click) done 2026-10-30; S8b (the check at APPLY) done 2026-10-31; S8R (the S7-S8b review follow-up) done 2026-10-31; S8R2 (Krondor's branch findings) done 2026-10-31; S8R3 (the S8R-S8R2 review follow-up) done 2026-10-31; S8R4 (the working copy's folder renamed) done 2026-10-31; S9a (the check: device files, at the first click) done 2026-10-31; S9R (the S8R4/S9a review follow-up) done 2026-10-31; S9R2 (the S9R review follow-up) done 2026-10-31; S9b (the check: device files, at APPLY) done 2026-10-31; S10 (a newer game version) done 2026-10-31; S11 (CREATE) done 2026-11-02; S12 (binds-file rename) done 2026-11-03; S15 (the bindings folder and the binds file in File Manager) done 2026-11-04; S16 (renaming a binds file, in File Manager) done 2026-11-04; S14a (read only: the state, the bar, the refusal) done 2026-11-05; S14b (read only: the editor screens) done 2026-11-06; S14c (the startup notice) done 2026-11-06; S13a (file names matched ignoring case) done 2026-11-07; S13aR (the S14a-S13a review follow-up) done 2026-11-07; S13b (plain `.binds`) done 2026-11-07; the S13aR-S13b review settled 2026-11-08, planning S13c (VEGA's loader), S13d (Leftover User Files) and S13e (a plain file judged against its twin); S13c (VEGA's loader) done 2026-11-08, adding S13f (VEGA asks which binds file) to plan; S13d (Leftover User Files) done 2026-11-08; S13e (a plain file judged against its twin) done 2026-11-08; the S13c-S13e review settled 2026-11-08, planning S13eR; S13eR (the review's follow-up) done 2026-11-09; the S13eR review settled 2026-11-09, planning S13eR2. S13eR2 (that review's follow-up) done 2026-11-09. S13f (which binds file VEGA reads, asked in a dialog) done 2026-11-10. S17 (the check again after the bindings folder changes) and S18 (the merge row by row for a file the game saved) planned 2026-11-11; S18 done 2026-11-11, with its review's D1 and D2; S18R (the review's watcher follow-up) planned; S18R done 2026-11-11; S17 (the check again after the bindings folder changes) done 2026-11-11. the review of S13f, S18R and S17 settled 2026-11-11, planning S13fR (its follow-up); S13fR done 2026-11-11, D1 and D2 dropped at its sources. RA (remove Run first-time startup again) dropped 2026-11-11 (Alan): the button stays and runs first-time startup at once - S19 done 2026-11-11. S20 (the check's warning grid review follow-up) planned and dropped 2026-11-12 (Alan). **No Storage slice is planned** |
| 3 | **Alias Designer** | [alias-designer.md](alias-designer.md) | [alias-designer.md](../02-features/bindforge/alias-designer.md) | built against the old model: device list, naming rule, first setup and the label merge, the Built-in Devices tab, which axes and hats a controller has, the Device Editor, CLEAR and RESET LABELS, onboarding, rename, shadowing a built-in, hot-plug, when hardware changes, the missing-controller warning. **Its mechanics moved to the working copy in Storage S4a (2026-10-26)**, which also removed the divergence list, the installation markers and catch-up; the identity question moved in S4b (2026-10-28). **A19** (CLEAR moves bindings to a hardware ID) done 2026-11-10; **what is left of A18 (gamepads) blocked** on a tester's `.binds` |
| 4 | **Preset Editor** | [preset-editor.md](preset-editor.md) | [preset-editor.md](../02-features/bindforge/preset-editor.md) | P1 (the BindName list) done; P2–P3's master, draft and merge replaced by Storage S6 (done 2026-10-28); **P4 (the screen) built 2026-11-10**, as a tab of its own between the Bind Editor and Binding Management (Alan, replacing Krondor's same-day ruling of a section inside Binding Management); P5 and P6 dropped — the check (S8) and the leave prompt (S7) replace them |
| 5 | **File Manager** (on screen: Binding Management, which stays its name - Krondor, 2026-11-10) | — its work is in [storage.md](storage.md) | [file-manager.md](../02-features/bindforge/file-manager.md) | backups S1 (done), Game Install Locations S2b (done), S9a (done), S15 and S16 (done 2026-11-04), RESTORE S3 (done). No brief of its own |
| 6 | **Bind Editor: editing** — Game Mode, Settings, capture dialog, mouse inputs | [bind-editor-editing.md](bind-editor-editing.md) | [bind-editor.md](../02-features/bindforge/bind-editor.md) | **The screen is final for V1.2** (Krondor, 2026-11-10): one tab row, E1 and E3 built, E6 and GM1 rejected. **E4** (MOUSE tab) and **E5** (SHIFT, ALT or CTRL on a controller input) built 2026-11-10, not yet run on hardware. **E2** (axis directions) and **E8** (pick on release) done 2026-11-10, checked on hardware. **E10** (the Toggles tab) done 2026-11-11, not yet run against the game. **E10R** (its review's follow-up) done 2026-11-11. E7 (gamepad capture) part built, the rest blocked on A18 |
| 7 | **Bind Editor: Anomalies** and shared conflict detection | — no brief; the 2026-11-03 re-scope replaced it | [bind-editor.md](../02-features/bindforge/bind-editor.md#anomalies) | **Built** as the rows drawn red, no sub-tabs (2026-11-03). **D6** (conflicts between two controller inputs) done 2026-11-10, its status kept in [bind-editor-editing.md](bind-editor-editing.md#slices) |
| 8 | **Rename the section to BindForge** | — | [v1.2-scope.md](../00-overview/v1.2-scope.md#phased-rollout--revised-2026-09-08) | **Done 2026-11-10** (Krondor): the main tab is BindForge and Binding Profile is Bind Editor, in all nine bundles, with the strings that name them; Binding Management keeps its name |

**The work left, how far along each part is and who takes
what:** [v1.2-remaining-work.md](v1.2-remaining-work.md) (rewritten 2026-11-10).

**Before V1.2 ships:** the reminders are in [storage.md — Before V1.2 ships](storage.md#before-v12-ships). *Hiding or
removing the Run first-time startup again button was one until 2026-11-11, when Alan kept it (Storage S19).*

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

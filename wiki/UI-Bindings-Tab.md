# Bindings Tab

<img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **New in V1.1.**
Bindings were a corner of the Actions tab; they are now a tab of their own, with a full editor.

Elite Intel operates your ship by pressing the keys Elite Dangerous is bound to. If a control
has no keyboard binding, Elite Intel cannot use it — this tab is where you find that out and
fix it.

Two sub-tabs: **Binding Profile** and **Binding Management**.

---

## Binding Profile

![Binding profile](images/ui-tab-bindings-profile.png)

### Which file is in use

**Profile** — detected automatically. Elite Intel reads the active `StartPreset` entry, and
falls back to the newest `.binds` file if it has to.

**File** — the `.binds` file currently being used for diagnostics and assignment.

**Bindings directory** — optional. Leave it blank and the standard Elite Dangerous location is
used; set it if your install is somewhere unusual.

Both fields have an ⓘ button explaining exactly how the value was chosen.

### The binding tables

Two tables: **Used bindings** and **Missing bindings**, with a count on each. Bindings are
grouped by category:

Ship / flight · Combat · UI panels · Maps · Exploration · Camera · SRV · On-foot · Miscellaneous

| Column | Meaning |
|--------|---------|
| **Binding** | The control |
| **Primary** / **Secondary** | The two slots Elite Dangerous gives every control |
| **Status** | `Missing` · `No keyboard` (bound, but only to a controller) · `Not defined` |
| **Quick fix** | Assigns a safe free key to this one control |
| **Clear** | Removes the keyboard binding, leaving controller and HOTAS bindings untouched |

> **HOTAS and controllers are shown but not editable.** Elite Intel executes through keyboard
> bindings, so other devices appear for diagnostics only.

**Show conflicts only** filters the tables to the problems.

### Conflicts

Elite Dangerous treats a chord as conflicting only when it is *exactly* the same chord — `G`
and `Shift+G` coexist happily. Elite Intel uses the same rule, so it flags what the game
actually flags.

Conflicting rows are coloured, and hovering one shows **Shares *key* with:** and the list.

You may also see **Ship/SRV twin — many bind it the same as:** — not a conflict, a suggestion.
Some ship and SRV controls are conventionally bound to the same key.

### Editing a binding

Click a slot to open the assignment dialog.

![Assign a key](images/ui-bindings-assign.png)

It shows the selected binding, the slot, and the current value. Then **click the field and
press the keys you want** — modifiers and key together. Esc cancels. Chords with multiple
modifiers are supported.

A live keyboard map shows what is available: **hold Ctrl/Shift/Alt to see the keys free for
that combination — green is free, red is already used.** Keys reserved by the operating system
are marked and cannot be assigned.

**Clear binding** removes the assignment.

### Fix Missing

One button that assigns safe, layout-friendly keyboard keys to **every** control that has no
keyboard binding.

- Existing bindings are never changed.
- No key is ever reused.
- The changes go into your **draft only**.

It reports what it did, and what it skipped and why: both slots already on a controller, no
free safe key left, or no slot that could be edited safely.

### Draft, Apply, Revert

Edits do **not** go straight to Elite Dangerous. They accumulate in a draft, and the status
badge shows **Draft — not applied to game** or **In sync with game**. The same state appears on
the Vega tab's *Keymap* readout.

| Button | What it does |
|--------|--------------|
| **Apply to Game** | Writes the draft to your `.binds` file, taking a backup first |
| **Revert from Game** | Throws the draft away and reloads from the game file |

> **After applying, open and then close the Controls screen in Elite Dangerous.** The game only
> re-reads its bindings when that screen is opened. Elite Intel says this out loud too.

If the game's binding file changed after your draft was created, Apply refuses and asks you to
reload or discard first, rather than silently overwriting someone else's edit.

Close the app with an unapplied draft and you are asked whether to **Apply to Game**, **Keep
Draft**, or **Discard**.

---

## Binding Management

![Binding management](images/ui-tab-bindings-management.png)

Your binding backups, listed by **Created** date and the **Files** each contains. Elite Intel
takes one automatically before every Apply; **Backup Now** takes one on demand.

| Button | What it does |
|--------|--------------|
| **Restore to Editing Slot** | Loads the backup into your draft, so you can review it before it touches the game |
| **Restore to Live** | Loads it and applies it to the game directly. The usual safe-apply checks still run |

Either one replaces unsaved changes in the current draft, and both ask first.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

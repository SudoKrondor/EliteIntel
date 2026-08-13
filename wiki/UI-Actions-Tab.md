# Actions Tab

<img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> Everything Elite
Intel can do, and everything you have taught it to do. Two sub-tabs: **Built-in Commands** and
**Custom Commands**.

---

## Built-in Commands

![Built-in commands](images/ui-tab-actions-builtin.png)

This is the answer to *"what can I say right now?"* — not just *"what does this build know how
to do?"*

### The scope picker

The picker at the top left holds **ALL**, plus every physical situation you can be in: in ship,
in SRV, in fighter, in taxi, on foot; docked, landed, gliding, in supercruise, at a ring, in
orbit, in deep space.

- It **follows the live game** — walk out of your ship and the picker moves to *On foot* by
  itself, and the list below changes with it.
- The moment you pick a scope by hand, it **stops following** and stays where you put it.
- **ALL** lists every action this build has, including ones you cannot use where you are.
  A specific situation lists **only what is usable there**.

Beside it, a read-only **Place** field shows the concrete location the game is reporting —
station, body, or system.

### Search

A plain, literal text filter over the listed actions: their names, their action keys, and the
spoken phrases that trigger them. What you type is what is looked for.

> This is deliberately **not** the companion's routing. Vega's dispatch ranks by *meaning*, so
> typing "find" there would surface commands that share no word with it and give you no way to
> see why. A literal search is the one you want when you are reading a list.

### Available commands and queries

One combined, alphabetically sorted list across three columns, holding built-in actions, your
custom macros, and queries for the chosen scope. It updates live from game events while the
tab is open.

**Double-click any entry** to open its details.

### Command details

| Field | Meaning |
|-------|---------|
| **Command name** | The human-readable name |
| **Action key** | The internal identifier — this is the name the language model sees |
| **Command type** | `Built-in binding` (presses a key) · `Built-in action` (does something in the app) · `Built-in query` (answers a question) · `Custom command` (yours) |
| **Description** | What it does |
| **Training phrases** | The spoken phrases that route to it, in your current language |

Three buttons:

- **Run** — execute it right now from the app, without speaking. If the command takes
  parameters, a small form appears first.
- **Suggest a better translation** — opens a pre-filled GitHub issue with the command id, your
  language, and the current phrases, so you can propose better wording for your locale. This is
  how the non-English phrase sets get better; please use it.
- **Close**

See also: [All Commands & Queries](AllCommands).

---

## Custom Commands

![Custom commands](images/ui-tab-actions-custom.png)

Your own macros — a named sequence of steps, triggered by things you say. Similar in spirit to
VoiceAttack, but matched by meaning rather than by an exact phrase.

The table lists each command's **Name** and its **Training phrases**, with a search box above it.

| Button | What it does |
|--------|--------------|
| **New** | Create a command |
| **Edit** | Edit the selected command |
| **Delete** | Delete the selected command (with confirmation) |
| **Export** | Write selected commands to a file you can share |
| **Import** | Read commands from a file. Your current set is backed up first |
| **Restore from backup** | Bring back the set that was replaced by an import |
| **Open backups folder** | Opens the folder on disk |

> If the custom command file is ever found corrupt on startup, Elite Intel loads from backup
> automatically and tells you it did so.

### The command editor

![Custom command editor](images/ui-custom-command-editor.png)

**Command Identity**

| Field | Notes |
|-------|-------|
| **Name** | What you call it |
| **Description** | What it does |
| **What you'll say** | The phrases you would use to run it — **one per line** |
| **Action key** | The internal identifier. Press **Generate** and the language model writes one for you from your phrases. It must be ASCII snake_case, because it becomes a tool name the model sees — so let the Generate button do it. Add at least one phrase before generating |

**Steps** — the sequence, in order. Add, edit, remove, and move steps up and down.

| Step type | Fields | Use it for |
|-----------|--------|------------|
| **Binding Tap** | Binding | Press a bound control once |
| **Binding Hold** | Binding, Duration ms | Hold a bound control |
| **Delay** | Duration ms | Wait between steps |
| **Speak** | Text | Have Vega say something |
| **Raw Key** | Raw Key, Modifier | Press a key that is not bound to anything in the game |

Prefer **Binding** steps over **Raw Key** where you can — bindings follow whatever keys the
game is actually using, so they survive you re-binding a control.

### Using them

Speak normally. You do not have to reproduce a training phrase word for word — you have to
convey the same meaning. The more distinct your phrases are from other commands, the more
reliably yours will be picked.

Vega tells you at startup how many custom commands loaded, and how many failed validation.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈

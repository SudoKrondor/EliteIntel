# The Input Environment

**What actually exists on a player's computer, and what each thing is called.** This page is definitions only —
the real files, folders and accounts Elite Dangerous keeps. It deliberately contains **no BindForge terms**:
master, draft and mirror live in the [glossary](glossary.md), so this page cannot go stale when our own design
changes.

**Written 2026-09-24, from Alan's definitions.** Read it before writing or reviewing anything that touches
input configuration. Most of the confusion this page exists to kill came from one word — *commander* — being
used to mean *a human being*.

> **Sections 1–6 describe Windows.** The paths and the counting rules are Windows paths and Windows rules.
> **Linux differs in one important way** — the bindings folder stops being shared — and that is
> [section 7](#7-linux-the-bindings-folder-is-not-shared). Everything about people, accounts, commanders and
> controllers holds on both.

---

## 1. The people

**A user, or player, is one human** who plays Elite Dangerous and uses Elite-Intel. The two words mean the same
thing and are interchangeable. **There is always exactly one.** Concretely, it is **the operating-system
account** — `C:\Users\Bob`, `/home/bob`, whatever `whoami` says (Krondor, 2026-09-26). That is what makes the
"exactly one" true: every path in this document hangs off it.

**An account is one Elite Dangerous / Frontier Developments account.** A player can have several.

**A commander is the player's in-game avatar, one per account.** So a player with three accounts has three
commanders. **Elite-Intel reads the current one from the journal, and there is only ever one per session**
(Krondor, 2026-09-26) — never two at once. Closing the game and re-logging from the store page switches the
commander; it does not add a second.

> **Commanders own no input files. Not one.** Every file on this page belongs either to the player or to an
> installation. Commanders appear only in the journals, which are Elite-Intel's concern and not BindForge's.
> If a sentence anywhere describes a `.binds`, `DeviceMappings.xml`, `.buttonMap` or `StartPreset` as belonging
> to a commander, that sentence is wrong.

---

## 2. What the player owns — one set, shared by everything *(Windows)*

**The player has one bindings folder:**

```
%LOCALAPPDATA%\Frontier Developments\Elite Dangerous\Options\Bindings\
```

One folder, whatever the player has installed. Steam, Epic, Frontier and Oculus installs all read the same one.
Every commander reads the same one. Note the space in "Frontier Developments".

**A `.binds` file is one saved set of control assignments.** The bindings folder can hold many of them. There
are two kinds, and the difference matters:

| Kind | Where it lives | Who writes it |
|---|---|---|
| **Custom `.binds`** | the player's bindings folder | the player — by rebinding in-game, or through BindForge. **This is the player's data.** |
| **Shipped `.binds`** | inside **each installation's** `ControlSchemes` folder | Frontier. Read-only templates, never edited in place. BindForge copies *from* them; it does not manage them. |

So "the player has one bindings folder" is about **their own** binds. Frontier's templates sit somewhere else
entirely, one copy per installation.

**A preset name is a `.binds` filename with the version and extension stripped off.** `Custom.4.2.binds` has
the preset name `Custom`. The game's Active Preset file deals in preset names, not filenames.

**The player has one `StartPreset.4.start` file**, in that same bindings folder. It says which `.binds` file the
game loads for each of the four input sections — **General, Ship, SRV, On Foot**, in that order, one per line.
The `4` is a game version number, so the filename changes when Frontier bumps it.

**The player has one journal folder:**

```
%USERPROFILE%\Saved Games\Frontier Developments\Elite Dangerous\
```

It holds the journals of every commander the player has ever played. **This is the only place commanders show
up**, and it is Elite-Intel's side of the work, not BindForge's.

> **"One player" is really "one Windows account."** Every path above starts at `%LOCALAPPDATA%` or
> `%USERPROFILE%`, so it is per Windows login. The same human with two Windows accounts has two complete,
> separate sets of everything on this page. Worth knowing; not a case we design for.

---

## 3. What an installation owns — one set each

**An installation is one full copy of the game's files** on a storage drive. A player can have several: Steam,
Epic, Frontier Direct, Oculus, even a second Frontier install. *This is the concept Krondor had no settled name
for on 2026-09-26 ("GameInstallInstance?"); **installation** is the term this documentation uses.*

**Each installation has its own `DeviceMappings.xml`.** It lists the player's controllers and the name each one
goes by, and **it only affects the installation it sits in**. This is the file that is genuinely duplicated —
confirmed on a machine with Steam and Epic side by side, each holding its own independent copy.

> **Strictly, it is one per *product folder*, not one per installation.** An installation can hold several
> products — `elite-dangerous-64` and `elite-dangerous-odyssey-64` — and each has its own `ControlSchemes`
> folder. Alan's two installations held three `DeviceMappings.xml` files between them. Because only the Live
> game is supported, only the Odyssey product matters — but **anything that scans for these files must pick the
> right product folder, not the first one it finds.**

**An alias is the name one controller goes by**, stored as an entry in `DeviceMappings.xml`. The game matches
that entry to real hardware by **VID and PID** — the USB vendor and product ID numbers the device reports —
**not by the name**. The name is for humans and for filenames; the numbers are what the game uses.

**A `.buttonMap` file names the individual buttons, axes and hats on one controller.** Its **filename is the
controller's alias**, which is how the game knows which controller it describes. They live in that
installation's `DeviceButtonMaps` folder.

There is **up to one `.buttonMap` per controller, and often none.** A missing one is the normal case, not a
fault — Frontier ships only a couple, and Horizons ships none at all.

**Keyboard and mouse are not controllers here.** There is no `Keyboard` or `Mouse` entry in
`DeviceMappings.xml` — a `.binds` file names them directly. They need no alias and no `.buttonMap`, and they
always work. Measured in `Custom.4.2.binds`: 76 assignments to `Keyboard`, 10 to `Mouse`.

---

## 4. Slots, unbound, and missing

**A control has two slots — Primary and Secondary — and each is filled or empty on its own.** This is the
distinction everything else here depends on, so it comes first:

```xml
<LandingGearToggle>
    <Primary Device="SaitekX56Throttle" Key="Joy_9" />
    <Secondary Device="{NoDevice}" Key="" />
</LandingGearToggle>
```

That control is **bound**. It simply has an empty Secondary.

| Term | What it means |
|---|---|
| **Slot** | One of a control's two assignment positions, **Primary** or **Secondary**. |
| **`{NoDevice}`** | What **one empty slot** holds. It is a **per-slot** value, not a per-control one. |
| **An empty slot** | That one slot is `{NoDevice}`. **The control may still be bound** through the other slot. |
| **An unbound control** | **Both** slots are `{NoDevice}`. Nothing on it at all. |
| **Intentionally unbound** | An unbound control that is meant to be that way, and finished — `Pause`, `EjectAllCargo`. Identical in the file; the difference is a note in our database. Never reported as a problem. |
| **Missing** | **Not the same as unbound.** Two shapes, below. |

> **So "unbound" always needs a level.** *Unbound slot* and *unbound control* are different claims, and a
> sentence that does not say which is ambiguous. Confirmed with Alan 2026-09-25, after this page first got it
> wrong by defining `{NoDevice}` at the control level.

**An empty slot is not waste — it is the working space.** A free Secondary is where BindForge adds a keyboard
binding so the assistant can press a control the player flies with a stick. It is also what the FN-1 conflict
bug was about: a chord shared between one control's Primary and another's Secondary is a real clash, and was
invisible until slots were compared as slots.

**Missing's two shapes:**

1. **Nothing bound** — an unbound control, both slots `{NoDevice}`.
2. **Bound perfectly well, but to a stick instead of a keyboard.** The file holds a real assignment, the game
   honours it, the hardware works. It is missing only from Elite-Intel's point of view, because the app can
   simulate a keyboard and nothing else, so it cannot press that control. **The remedy is the free slot.**

Shape two is the dangerous one, because nothing looks wrong.

**Most of a file is empty, and that is normal.** Counted over the controls in `Custom.4.2.binds` that have a
Primary/Secondary pair:

| | Count |
|---|---|
| both slots empty — unbound control | 203 |
| **exactly one slot empty — bound, with a free slot** | **132** |
| both slots filled | 17 |

`DualVirpilDawnTreader.4.1.binds`, the same way: 101 unbound, **163 with one free slot**, 70 fully filled — a
more heavily bound file, and even more of it has a slot free.

*(These count only controls with a Primary/Secondary pair. The wider figures quoted elsewhere — 245 of 422, and
128 of 396 — count every control in the file including axis and settings entries, so the totals differ. Both
are right about their own population.)*

---

## 5. What does not exist

- A `.binds` file that belongs to one installation. *(Frontier's shipped templates do — the player's do not.)*
- A `DeviceMappings.xml` or `.buttonMap` shared between installations.
- **Any input file that belongs to a commander.**
- A `.buttonMap` for a controller with no alias in `DeviceMappings.xml` — its filename would have nothing to be.
- Legacy support. Only the **Live** game is supported, so a folder never holds two versions of one preset.

---

## 6. Counting rules, all in one place *(Windows)*

Three of these change on Linux — see [section 7](#7-linux-the-bindings-folder-is-not-shared).

| This | to this | |
|---|---|---|
| player → bindings folder | 1 → 1 | **Windows.** On Linux, one per installation |
| player → `StartPreset` | 1 → 1 | **Windows.** On Linux, one per installation |
| player → custom `.binds` files | 1 → many | all in the one folder |
| player → journal folder | 1 → 1 | holds every commander. **Windows** — on Linux, one per installation |
| player → accounts | 1 → many | |
| account → commander | 1 → 1 | |
| player → installations | 1 → many | |
| installation → `DeviceMappings.xml` | 1 → 1 | strictly, one per product folder |
| installation → `.buttonMap` files | 1 → many | **up to** one per controller; none is normal |
| installation → accounts | usually 1 → 1 | but one installation **can** be set up to load any number of accounts |
| controller → alias | 1 → 1 | matched by VID/PID, not by name |
| alias → `.buttonMap` filename | 1 → 1 | the filename *is* the alias |
| **commander → any input file** | **1 → 0** | |

---

## 7. Linux: the bindings folder is not shared

**Added 2026-09-26.** Elite Dangerous has no native Linux build; it runs under Steam Proton. That puts the
whole Windows-style `AppData` tree **inside the Wine prefix**:

```
<steam root>/steamapps/compatdata/359320/pfx/drive_c/users/steamuser/AppData/Local/...
```

`compatdata` sits under the **Steam root**, and a Linux machine can have several Steam installations — native,
Flatpak and Snap are independent of each other, with no registry to arbitrate. Krondor, 2026-09-26: *"I had
all three Steams installed... I can have at least 3 Steams and be logged in to all three with different steam
accounts."*

**So each Steam installation carries its own prefix, and therefore its own bindings folder and its own journal
folder.** On Windows a single `%LOCALAPPDATA%` tree is shared by every storefront, which is the whole reason
`.binds` and `StartPreset` are one set per player. Inside a Proton prefix there is no shared `AppData` at all.

### What changes

| | Windows | Linux |
|---|---|---|
| `.binds`, `StartPreset` | **one set per player**, shared | **one set per installation** |
| journal folder | **one per player**, holds every commander | **one per installation** |
| `DeviceMappings.xml`, `.buttonMap` | one set per installation | *unchanged* — one set per installation |

Which makes Linux the simpler case to describe: **everything is installation-scoped.** It does not change the
master — BindForge still keeps one; it simply has more places to push it to.

### What does not change

Sections 1, 3, 4 and 5 hold on both. One user, still the OS account. Commanders still own nothing. A
controller still gets one alias, and a `.buttonMap` is still named after it.

### Where the installations are

Four candidate Steam roots, and two traps — the probe list stops at the first match, and `$HOME/.steam/steam`
is normally a symlink to `$HOME/.local/share/Steam`, so a detector that does not resolve it counts one
installation twice. The exact paths, read out of Elite-Intel's own installer, are in
[§5a–5c of the install-paths reference](../01-host-integration/domain-knowledge/EliteDangerous-InstallPaths.md#5a-linux-has-several-steam-roots-not-one---confirmed-2026-09-26).
On Linux, Elite-Intel reaches the game through two symlinks its launcher builds — `ed-bindings` and
`ed-journal` — rather than a path it computes.

### This arrangement may invert - flagged 2026-09-26

Krondor is considering turning the symlinks around: the real bindings and journal folders would live in the
app's own install directory, and each Steam prefix would link **to** them, so all three Steam installations
read and write one set. *"I have not decided yet. I have to consider impl and side-affects."*

**If that lands, Linux returns to the Windows model** - one shared bindings folder per player, and the table
above collapses back to the counting rules in §6. Nothing in this page's definitions changes either way; only
which row applies.

It is also why the
[`GameInstallationProvider` contract](../01-host-integration/elite-intel-platform-map.md#gameinstallationprovider---krondors-proposal-2026-09-26)
hangs the bindings folder off each **installation** rather than off the provider. Under today's arrangement
each Linux installation reports a different folder; under the inverted one they all report the same folder,
exactly as Windows does now. BindForge needs no change for the flip, because it never asked the question
"is this Windows?" in the first place.

### How sure we are

**The paths are confirmed**, read from `Installer.install4j`, which ships and runs on every Linux start.
**The consequence is reasoned, not observed:** nobody has yet put three Steam installations on one machine and
confirmed three genuinely independent bindings folders. That is
[testing-required item 8](testing-required.md#core-platform), and it is Krondor's platform, not ours.

---

## Where the paths came from

Full detail, including the Linux/Proton layout and the research history:
[EliteDangerous-InstallPaths.md](../01-host-integration/domain-knowledge/EliteDangerous-InstallPaths.md).
Device file format detail:
[EliteDangerous-DeviceMappings-ButtonMap.md](../02-features/bindforge/domain-knowledge/EliteDangerous-DeviceMappings-ButtonMap.md).

# BindForge — Purpose Mode Specification

**Project:** Elite Intel
**Feature:** BindForge — Purpose Mode
**Version:** Draft
**Status:** Deferred — not part of v1. To be implemented after Game Mode is stable.
**Parent document:** `BindForge_Spec.md` Section 6.3

---

## 1. Overview

Purpose Mode is a future viewing mode for the BindForge Bind Editor. It groups bindings by player intent rather than by game category. Where Game Mode asks "what section of the game does this binding belong to?", Purpose Mode asks "what is the player trying to accomplish physically?"

For example "Move Left" is a single purpose that spans Ship Controls, SRV Controls, On Foot Controls, and General Controls simultaneously. In Game Mode these bindings appear in four separate sections. In Purpose Mode they appear together in one group called "Move Left."

---

## 2. Core Concepts

### 2.1 Purpose Groups

A purpose group is a named collection of bindings that share the same physical intent regardless of which game section they belong to. Examples:

- Move Left
- Move Right
- Move Forward
- Move Backward
- Move Up
- Move Down
- Yaw Left
- Yaw Right
- Roll Left
- Roll Right
- Pitch Up
- Pitch Down
- Primary Fire
- Secondary Fire
- Select / Activate
- Thrust Forward
- Thrust Backward
- Deploy / Retract
- Open / Close Panel

Each binding belongs to at most one purpose group. A binding cannot logically serve two different physical intents simultaneously — if it did both groups would require the same key bound, which is a conflict.

### 2.2 Default Groups

A default set of purpose groups is defined by the BindForge team and ships with Elite Intel. Default groups:

- Are read-only — they cannot be renamed, edited, or deleted by the user
- Are shown with a lock icon in the group list
- Cover the most common player intents across all game sections
- Are defined after the Binds File Audit maps all binding actions to their game sections

The exact default group definitions are authored separately after the Binds File Audit is complete. They are not defined in this document.

### 2.3 User Groups

Players can create their own purpose groups in addition to the default set. User groups:

- Are fully editable — name, bindings, and order can all be changed
- Can be deleted
- Are shown without a lock icon in the group list
- Are stored persistently in the Elite Intel database

### 2.4 Ungrouped Bindings

Bindings that do not belong to any purpose group are not shown in Purpose Mode. They remain visible in Game Mode. This is intentional — Purpose Mode is a curated view, not a complete view.

---

## 3. UI — View Groups Tab

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Game Mode      Purpose Mode ◀      Type Mode                           │
├─────────────────────────────────────────────────────────────────────────┤
│  View Groups ◀      Manage Groups                                       │
├──────────────────────┬──────────────────────────────────────────────────┤
│                      │                                                  │
│  PURPOSE GROUPS      │  BINDINGS IN GROUP: Move Left                    │
│                      │                                                  │
│  🔒 Move Left   ◀    │  Search: [                                    ]  │
│  🔒 Move Right       │                                                  │
│  🔒 Yaw Left         │  Binding              Primary    Secondary       │
│  🔒 Yaw Right        │  Ship - Thrust Left   [JOY 1]    —               │
│  🔒 Primary Fire     │  Ship - Move Left     [JOY X-]   —               │
│  🔒 Select           │  SRV - Steer Left     [JOY 1]    —               │
│  🔒 Thrust Forward   │  On Foot - Strafe L   [JOY 1]    —               │
│  ...                 │  General - Move Left  [JOY X-]   —               │
│                      │                                                  │
│  My Groups           │                                                  │
│  My Group 1          │                                                  │
│  My Group 2          │                                                  │
│                      │                                                  │
│  [ + New Group ]     │                                                  │
└──────────────────────┴──────────────────────────────────────────────────┘
```

**Left panel** — all purpose groups, default groups first with lock icons, user groups below. Selecting a group loads its bindings into the right panel.

**Right panel** — binding grid showing only the bindings in the selected group. Same column structure as Game Mode — Binding, Primary, Secondary. Search filters within the selected group in real time.

Clicking a binding row in View Groups opens the same Binding Editor panel as Game Mode — the user can edit the binding directly from Purpose Mode without switching to Game Mode.

---

## 4. UI — Manage Groups Tab

```
┌─────────────────────────────────────────────────────────────────────────┐
│  View Groups      Manage Groups ◀                                       │
├─────────────────┬──────────────────────────┬───────────────────────────┤
│                 │                          │                            │
│  PURPOSE GROUPS │  Group Name:             │  Search: [              ] │
│                 │  [ Move Left          ]  │─────────────────────────  │
│  🔒 Move Left ◀ │                          │ Ship - Thrust Left        │
│  🔒 Move Right  │  BINDINGS IN GROUP       │ Ship - Thrust Right       │
│  🔒 Yaw Left    │  ┌────────────────────┐  │ Ship - Thrust Up          │
│  🔒 Yaw Right   │  │ Ship - Thrust Left │  │ Ship - Thrust Down        │
│  🔒 Primary Fire│  │ Ship - Move Left   │  │ Ship - Move Left          │
│  🔒 Select      │  │ SRV - Steer Left   │  │ Ship - Move Right         │
│  🔒 Thrust Fwd  │  │ OnFoot - Strafe L  │  │ SRV - Steer Left          │
│  ...            │  │ General - Move L   │  │ SRV - Steer Right         │
│                 │  │                    │  │ OnFoot - Strafe Left      │
│  My Groups      │  │                    │  │ OnFoot - Strafe Right     │
│  My Group 1     │  │                    │  │ General - Move Left       │
│  My Group 2     │  └────────────────────┘  │ General - Move Right      │
│                 │                          │ ...                       │
│ [ + New Group ] │  [ << Add ]  [ Remove >> ]                           │
│ [ - Delete ]    │                          │                            │
│                 │  [ Save ]  [ Discard ]   │                            │
└─────────────────┴──────────────────────────┴───────────────────────────┘
```

Three columns flow left to right:

**Left** — purpose group list. Default groups with lock icon at top. User groups below. New Group and Delete buttons at the bottom — Delete only available for user groups.

**Middle** — Group Name field at top, read-only for default groups. Bindings currently in the selected group listed below. `<< Add` moves a selected binding from the right list into the group. `Remove >>` moves a selected binding out of the group back to the right list. Save and Discard at the bottom.

**Right** — full searchable list of all bindings in the current `.binds` file. Search filters in real time.

### 4.1 Conflict Prevention

Each binding can belong to at most one purpose group. When the user attempts to add a binding that already belongs to another group BindForge blocks the action and shows a warning:

*"[Binding name] already belongs to the group [Group name]. Remove it from that group first before adding it here."*

### 4.2 New Group Creation

Clicking New Group creates an empty user group with a placeholder name. The name field becomes active immediately for the user to type. The group is not saved until the user clicks Save.

### 4.3 Persistence

Purpose group definitions — group names and their binding membership — are stored in the Elite Intel database. The exact table schema is determined during implementation. Default group definitions are stored in code, not in the database, so they cannot be accidentally deleted or corrupted by database operations.

---

## 5. Prerequisites

Purpose Mode cannot be fully designed or built until the following are complete:

- **Binds File Audit** — the complete inventory of all binding actions and their game sections is required to define the default purpose groups. See `BindForge_BindsFileAudit.md`.
- **Game Mode stable** — Purpose Mode builds on the same binding data model as Game Mode. Game Mode must be working correctly before Purpose Mode is attempted.
- **Database schema** — a new table or tables in the Elite Intel database are needed for user group persistence.

---

## 6. Known Open Questions

- What happens to a user's custom purpose groups when Frontier adds new binding actions in a game update? The new actions would be ungrouped. BindForge should notify the user that new ungrouped bindings exist.
- Should the default purpose groups be updatable via an Elite Intel update without requiring a full app release? This would allow the default group definitions to evolve as the game adds new content.
- Should bindings that are completely unbound — no Primary or Secondary assignment — appear in Purpose Mode groups or be hidden?

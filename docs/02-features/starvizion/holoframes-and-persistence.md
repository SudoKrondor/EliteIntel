# StarVizion — HoloFrames and Persistence

## HoloFrames

*(Named VizLoadout in earlier design generations — renamed for clarity; same underlying concept.)*

A **HoloFrame** is a named group of Vizlets that can be activated together as a set with a single action — the equivalent of switching a control loadout in Elite Dangerous, but for the player's visual overlay configuration.

**What it contains:** a list of Vizlet IDs. It records which Vizlets belong to the group and does not store those Vizlets' content — each Vizlet owns its own definition. A HoloFrame is a grouping mechanism only, not a container.

**Activating/deactivating:** activating a HoloFrame shows all member Vizlets in their saved screen positions. Deactivating it hides them all. Neither ever closes a member Vizlet's window — see [UI Layout — Hiding Never Closes a Window](ui-layout.md#hiding-never-closes-a-window). Individual Vizlets can be added to or removed from a HoloFrame at any time.

**Multiple HoloFrames:** a user may have several — one for ship flight, one for on-foot, one for SRV. **Automatic HoloFrame switching, based on the same journal-detected game mode used for per-Vizlet Mode-Based Visibility, is a v1 requirement** — earlier drafts of this documentation called this a future direction; it's since been confirmed as needed for v1. Manual activation remains available regardless of whether a HoloFrame has a mode assignment.

### Mode-Based HoloFrame Switching

A HoloFrame can optionally be assigned to one or more game modes, using the same mode list as [per-Vizlet Mode-Based Visibility](ui-layout.md#mode-based-visibility) (Ship — Normal Flight, Ship — FSS Scanner, Ship — Detailed Surface Scanner, SRV, On Foot, Docked / Station, Supercruise). When the detected game mode changes, StarVizion automatically deactivates whichever mode-assigned HoloFrame no longer matches and activates whichever one now does. Motivation: the information relevant on foot is genuinely different from the information relevant in a ship, so switching the whole visible set together is worth doing automatically rather than leaving the player to manage each Vizlet's own visibility individually.

**Precedence over a member Vizlet's own individual mode setting:** while a Vizlet belongs to a mode-assigned HoloFrame, the HoloFrame's own mode assignment governs that Vizlet's visibility — the Vizlet's individual Mode-Based Visibility setting is not consulted while it's a member of one. A Vizlet not currently in any mode-assigned HoloFrame continues to use its own individual setting exactly as before. This keeps the mental model simple: the group's rule wins whenever one applies.

A HoloFrame with no mode assignment behaves exactly as it always has — manual activate/deactivate only, no automatic switching.

### Render Mode Override

A HoloFrame can optionally set a [Render Mode](../../02-features/starvizion/vr-overlay.md#rendering-modes) (Desktop, VR, Desktop + VR, or Desktop+) that applies to every member Vizlet uniformly — the same precedence pattern as Mode-Based HoloFrame Switching above: while a Vizlet belongs to a HoloFrame with a Render Mode override set, that override governs how the Vizlet renders, and its own individually-configured Render Mode is not consulted. A HoloFrame with no Render Mode override leaves each member's own individual setting untouched.

This is deliberately whole-group, not per-member — a HoloFrame is the grouping of Vizlets, so its settings apply to the group as a set. A player who wants some Vizlets in what they think of as one set to render differently than others splits that into two HoloFrames rather than expecting mixed behavior inside one; the same answer already applies to visibility, and there's no reason Render Mode should work differently.

**Deleting a HoloFrame:** removes the grouping only, with confirmation. Since a HoloFrame never owns Vizlet content — it only references Vizlet IDs — deleting one never deletes the member Vizlets themselves; they remain in the Vizlet library, untouched, just no longer grouped under the deleted HoloFrame.

**Storage:** HoloFrames are stored as individual document files in a user-configurable folder managed by Elite-Intel's application path resolver (`AppPaths`), named the same way Vizlets are — see the filename convention below.

## Vizlet Persistence

Every Vizlet is stored as its own document file. There is no unsaved state — edits are written immediately (with debouncing to avoid excessive disk writes). This file is the single source of truth for that Vizlet.

**Conceptual contents of a saved Vizlet:**
- **ID** — a stable identifier, generated once when the Vizlet is created and never changed afterward. This is what HoloFrames actually reference, what import conflict-detection keys off, and what every internal lookup uses — never the Name, and never the filename.
- Descriptive metadata: name, author, creation timestamp, version, tags
- Window properties: size, position, always-on-top, transparency
- A list of NeuroNodes, each with: its primitive type, Z-order, input bindings (empty for a Static-mode node), local expressions, property expressions, and any fixed (non-expression-driven) property values

**Filename convention:** `{Name}-{ID}.vizlet.json` — a sanitized, filesystem-safe version of the Vizlet's current Name, followed by its ID (a GUID). The Name portion exists purely so the file is recognizable to a human browsing the folder directly; nothing in StarVizion depends on it staying fixed. **Renaming a Vizlet silently renames its file to match**, transparent to the user — this is safe precisely because every reference to the Vizlet (HoloFrame membership, import conflict checks) resolves through the ID embedded in the filename and stored inside the file, never through the filename string as a whole. HoloFrames use the same `{Name}-{ID}.holoframe.json` pattern for the same reason.

Malformed node entries are logged and skipped on load; other nodes in the same Vizlet continue to load normally. A fully Static node with no input bindings — for example, a fixed background shape defined entirely by constant expressions — is a completely ordinary Vizlet entry; no separate "static node type" exists (see [Vizlets and the Node System](vizlet-and-node-system.md#node-system)).

**Storage location:** a user-configurable folder, defaulting to a location managed by Elite-Intel's application path resolver (`AppPaths`).

**Corrupt or missing files:** if a Vizlet's file is missing or unreadable when a HoloFrame is activated, that specific Vizlet is skipped and logged; other Vizlets in the HoloFrame load normally, and the editor shows a warning badge on the affected entry.

## Export and Import

### Exporting a Vizlet

Any Vizlet can be exported from the Vizlet library as a standalone file. The export includes all node definitions, input bindings, expressions, static properties, and metadata. It does not include device-specific identifiers that would only make sense on the exporter's machine — bindings reference device type and input identity, which the importer maps to their own hardware.

### Exporting a HoloFrame

A HoloFrame export is a single archive containing a manifest listing the included Vizlets, all referenced Vizlet files, and any custom image assets referenced by Bitmap nodes (embedded). The export archive format is distinct from the plain document format StarVizion uses to store a HoloFrame locally.

### Importing

Import is initiated via a file picker or drag-and-drop onto the Vizlet library.

- The file is validated against the expected structure; invalid files are rejected with a clear error.
- Name conflicts prompt the user: rename, overwrite, or cancel.
- Imported Vizlets are not automatically activated — the user must activate them manually.

**Binding remapping on import:** import always succeeds even if the importing machine does not have the hardware referenced by the Vizlet's input bindings. Nodes with unresolved bindings (device or input not found on this machine) are flagged with a warning indicator. The Vizlet loads and renders — Static-mode nodes work fully; Live-mode nodes with unresolved bindings render in a passive state. The user then opens the affected node in the editor and remaps its bindings to their own hardware. There is no automatic remapping.

### Community Sharing

There is no built-in sharing or marketplace in v1. Export/import serves manual community sharing (forums, Discord, code-hosting sites). A community HoloFrame library is a possible future direction.

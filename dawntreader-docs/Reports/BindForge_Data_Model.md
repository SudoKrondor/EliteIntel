# BindForge — Data Model Specification

**Status:** In progress — being designed section by section.
**Source of truth for:** Java class shapes, field names, types, and rationale for BindForge's in-memory object model.
**Related documents:** `BindForge_Punch_List.md`, `AUDIT_v2_FULL.md`, `BindForge_GameMode_SubGroups.md`

---

## Design Principles

- Three separate in-memory collections (`.binds` elements, `Devices`, `ButtonLabels`) composed inside one loaded `BindingProfile` container. Plain Java object composition — NOT database tables. No binding content is persisted to SQLite.
- Each collection stays close to its own source file's shape for clean round-tripping on save.
- The model exposes cross-referencing lookups (e.g. "resolve the friendly label for this binding's device+key") rather than flattening everything into one merged structure.
- Type safety enforced via inheritance (AXIS/BUTTON/STANDALONE as separate subclasses) rather than a flat class with nullable fields — prevents type-inappropriate properties from being assigned at compile time.
- `<KeyboardLayout>` is a first-class field, not an edge case — must be read, preserved, and written back on every save to support Totem's layout-aware scan code resolution fix.

---

## BindingProfile (top-level container)

Represents one complete loaded state across all four source file types: `StartPreset.4.start`, the active `.binds` file, `DeviceMappings.xml`, and all `.buttonMap` files.

```
BindingProfile
├── generalBindFile: String    // from StartPreset.4.start — the .binds filename assigned to General bind group (e.g. "DualVirpilDawnTreader.4.2")
├── shipBindFile: String       // from StartPreset.4.start — the .binds filename assigned to Ship bind group
├── srvBindFile: String        // from StartPreset.4.start — the .binds filename assigned to SRV bind group
├── onFootBindFile: String     // from StartPreset.4.start — the .binds filename assigned to On Foot bind group
├── keyboardLayout: String     // from <KeyboardLayout> in the .binds file — first-class, must be preserved on save
├── bindings: List<BindingElement>                        // all elements parsed from the .binds file
├── devices: List<DeviceEntry>                            // from DeviceMappings.xml
└── buttonLabels: Map<String, List<ButtonLabel>>          // keyed by device friendly name, from .buttonMap files
```

**Notes:**
- All four `*BindFile` fields are independent — in normal use they all point at the same `.binds` filename, but the split-preset edge case (each group pointing at a different file) is supported without any special logic, since each field is just a string written back to its corresponding line in `StartPreset.4.start`.
- `presetName` is NOT a separate field — the preset name is already implied by the `*BindFile` fields.
- On save: `generalBindFile`/`shipBindFile`/`srvBindFile`/`onFootBindFile` write back to `StartPreset.4.start`; `keyboardLayout` and `bindings` write back to the `.binds` file; `devices` writes back to `DeviceMappings.xml` (fanned out to all discovered installs); `buttonLabels` writes back to the appropriate `.buttonMap` files (fanned out to all discovered installs).

---

*Further class shapes (BindingElement hierarchy, ButtonSlot, AxisSlot, BindingModifier, DeviceEntry, ButtonLabel) to be added as each is designed and agreed.*

# Elite Dangerous — DeviceMappings.xml & .buttonMap Format Reference

**Scope:** The two per-device support files that sit alongside `.binds`: `DeviceMappings.xml` (the device VID/PID registry) and `*.buttonMap` files (human-readable button/axis labels). ~~Neither file affects gameplay — both exist purely to label devices/inputs.~~ *Corrected 2026-09-21:* `.buttonMap` is labels only, but `DeviceMappings.xml` is not — `.binds` names a device by its entry once one exists, and without the file the game loads no bindings at all ([Why `DeviceMappings.xml` is not cosmetic](../overview.md#why-devicemappingsxml-is-not-cosmetic--reclassified-2026-09-08)). BindForge protects both exactly as it protects `.binds` ([Protection is uniform](../overview.md#protection-is-uniform--settled-2026-09-21)). See `EliteDangerous-BindsFileFormat.md` §4 for how the `Device=` attribute values these files support relate back to the `.binds` file itself.

**Confidence:** These two formats are documented more thinly in the source material than `.binds` itself — several details below are inferred from a small number of examples rather than exhaustively verified against the full range of real device configurations. Gaps are called out explicitly in §3 rather than filled in with invented detail.

---

## 1. DeviceMappings.xml

### 1.1 File structure

```xml
<Root>
    <VPCPanel>
        <PID>0259</PID>
        <VID>3344</VID>
    </VPCPanel>
    <T-Rudder>
        <PID>B679</PID>
        <VID>044F</VID>
    </T-Rudder>
</Root>
```

- Root element: `<Root>`. **Corrected 2026-10-01** — this section previously said `<DeviceMappings>`, which
  is the filename rather than the tag. Frontier's own shipped file opens `<Root>`, as does every real
  `DeviceMappings.xml`; see the
  [stock capture](../reference-data/FrontierStock-DeviceMappings.xml). *(The `T-Rudder` PID above was wrong
  with it — Frontier's entry is `B679`, not `0BF3`.)*
- One child element per device, named with the device's logical name. This name is what shows up as a *named* `Device=` value elsewhere in `.binds` (e.g. `RVWAP`, `T-Rudder`) instead of a VID/PID hex string.
- `<VID>` — Vendor ID, 4 hex characters.
- `<PID>` — Product ID, 4 hex characters.

### 1.2 Relationship to `.binds`

The device identifier used in a `.binds` file's `Device=` attribute for a given device is derived by concatenation:

```
BindFileDeviceID = VID + PID   (8 hex characters, concatenated)
```

Example: `VID=3344`, `PID=0259` → `.binds` file `Device="33440259"`.

See `EliteDangerous-BindsFileFormat.md` §4.2 for the full discussion of this derivation, its implications (a device's `.binds` references going stale after a VID/PID change via vendor configuration software), and the caveat that exact formatting details (byte order, hex case, zero-padding) are not independently confirmed beyond this one matching example.

### 1.2b What happens when a `Device=` name has no entry — measured 2026-09-22

**The whole preset is rejected, not just that device's bindings.** Established by direct test on a machine
holding two installs that share one `Options\Bindings` folder, so the only variable was the per-install
`DeviceMappings.xml`.

| | Steam install | Epic install |
|---|---|---|
| Entries for the user's two sticks (`RVWAP`, `LVWAP`) | present | **absent** |
| Shared `StartPreset.4.start` | names the user's active preset | the same file |
| Shared `.binds` (144 bindings naming those sticks) | the same file | the same file |
| Result on the Controls screen | preset loads; all four sections correct | **all four sections fall back to `KEYBOARD & MOUSE`** |

The failing install wrote `BindingLoadingErrors.log` beside the `.binds` files, holding one
`Failed to find GUID for device: RVWAP` line per unresolvable binding — **141 across the user's two
preset files**, roughly one per `Device=` reference, in file order.

**Three facts worth separating:**

1. **The loss is total, not partial.** The keyboard and mouse bindings in that preset — which reference no
   device entry at all — went with it. A user sees a factory preset, not a preset with gaps.
2. **Loading changes nothing on disk.** Both `.binds` files and `StartPreset.#.start` kept their previous
   timestamps through the failing launch, and all 144 `Device="..."` references survived intact. The game
   degrades quietly rather than repairing or rewriting. *(The save path — opening Controls and committing a
   change — was not tested and is a separate question.)*
3. **The game validates every `.binds` in the folder**, not just the active preset: the log carried a block
   for each of the two preset files present.

**Why this matters beyond the device files.** `StartPreset.#.start` and `.binds` are shared by every install
on the machine, and `DeviceMappings.xml` is not. So a file every install reads depends on a file each install
owns privately — which is why a tool cannot treat "which install" as a detail. See
[Protection is uniform](../overview.md#protection-is-uniform--settled-2026-09-21).

### 1.2c The game parses it as XML, and does not reformat it — measured 2026-10-03

**The question:** when BindForge writes this file, must it reproduce Frontier's own layout? Their file is
indented with tabs, crams `<PID>` and `<VID>` onto the opening tag's line, and keeps each `<Alternative>` on
one line. If the game read any of that as structure, a generated file would fail — and
[§1.2b](#12b-what-happens-when-a-device-name-has-no-entry--measured-2026-09-22) shows what failure costs.

**The test.** The developer's Steam install was rewritten with the same content in a different layout, then
played. Nothing else was changed: identical element order, identical VID/PID values *including their original
hex case*, the file's `<!-- XB1 Controller -->` comment left where it was inside `<GamePad>`, all five
`<SupportsIcons>` elements kept, and CRLF line endings preserved so they were not a second variable.

| | Before | After |
|---|---|---|
| Indentation | tabs | four spaces |
| `<PID>`/`<VID>` | on the opening tag's line | one per line |
| `<Alternative>` | one line | four lines |
| Declaration | `<?xml version="1.0" encoding="UTF-8" ?>` | `<?xml version="1.0" encoding="UTF-8"?>` |
| Tags, VID/PID pairs | 840, 138 | **identical** |

**Three results:**

1. **The bindings loaded normally** — no fallback to `KEYBOARD & MOUSE`, and **no `BindingLoadingErrors.log`
   was written**, which §1.2b establishes is what an unresolvable device produces.
2. **The game did not rewrite the file.** It wrote `.binds` and `StartPreset.4.start` during the session, so
   it was reading and saving normally, and left `DeviceMappings.xml` in the new layout untouched.
3. **So the file is parsed as XML, not matched as text.** Whitespace, line breaks and the declaration's
   spacing carry no meaning.

**What this licenses, and what it does not.** BindForge may write clean, readable XML rather than reproducing
Frontier's inconsistent spacing. It may **not** reconstruct the file from the tags it knows about — see
[§1.3](#13-additional-per-device-metadata--confirmed-2026-10-03). *The first attempt at this very test silently
dropped all five `<SupportsIcons>` elements, because it emitted only `PID`, `VID` and `Alternative`. The file
looked correct and the content was gone. Re-indent what is there; never regenerate it.*

*(Still untested: whether the game rewrites the file when a device is added or removed in the Controls screen
while an edited layout is in place. The session above opened the game normally and did not exercise that
path.)*

### 1.3 Additional per-device metadata — confirmed 2026-10-03

The source material noted two possibilities without a confirmed schema. **Both are now confirmed against
Frontier's own shipped file** ([the stock capture](../reference-data/FrontierStock-README.md)) and the
developer's edited Steam copy.

- **`<Alternative>` is real, and it is a wrapper.** It holds its own `<PID>`/`<VID>` pair, as a sibling of the
  element's primary pair. The stock file carries **85** of them across 51 elements — **79 on `<GamePad>`
  alone**, with `<DualShock4>` holding two. *This is why a provenance record is keyed by element name rather
  than by VID/PID: keyed on hardware, `<GamePad>` would become eighty rows describing one entry.*
- **`<SupportsIcons>` is real**, and the name in the source notes was right. Five appear in the developer's
  file. Its value shape has not been examined, and **BindForge has no reason to read it** — it is Frontier's
  metadata about their own UI.

**Practical takeaway, and it is sharper than before.** A parser must not assume a device element has exactly
two children — but the useful rule is the write-side one: **preserve every child element rather than
regenerating the ones you recognise.** Emitting only `PID`, `VID` and `Alternative` drops `<SupportsIcons>`
silently, leaving a file that looks correct and has lost content. That failure was produced once, during
[the formatting test](#12c-the-game-parses-it-as-xml-and-does-not-reformat-it--measured-2026-10-03), by code
written from this section's own warning.

**There may be more tags than these two.** Neither file is a schema, and a device nobody here owns may carry
something else again — which is the whole argument for preserving the unrecognised rather than listing the
recognised.

### 1.4 File location and per-storefront duplication

`DeviceMappings.xml` lives inside the game's own install folder, under a `ControlSchemes` subfolder — **not** in the shared user-config bindings folder used by `.binds`. It only works from that install-folder location; placing a copy in the `.binds` folder does not work.

**This file is genuinely duplicated per storefront install** — confirmed on a real machine with multiple storefronts (Epic + Steam) side by side, each with its own independent copy under its own install tree. This is a different multiplicity rule from `.binds`, which has exactly one shared copy regardless of storefront. See `EliteDangerous-InstallPaths.md` for the full path structure and discovery approach across storefronts and platforms.

---

## 2. `.buttonMap` Files

### 2.1 File structure

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<Root>
    <Joy_1>B1</Joy_1>
    <Joy_2>B2</Joy_2>
    <Joy_13>T1 - Down</Joy_13>
    <Joy_14>T1 - Up</Joy_14>
    <Joy_34>E1 - Counter Clockwise</Joy_34>
    <Joy_35>E1 - Clockwise</Joy_35>
    <Joy_UAxis>A1</Joy_UAxis>
    <Joy_VAxis>A2</Joy_VAxis>
</Root>
```

Each child element's tag is an input token drawn from the same vocabulary as `.binds` `Key=` values (see `EliteDangerous-BindsFileFormat.md` §5); its text content is the human-readable label to display instead of the raw token.

### 2.2 Filename convention

```
<DeviceName>.buttonMap
```

`<DeviceName>` matches the element tag name used for that device in `DeviceMappings.xml`. Examples: `VPCPanel.buttonMap` corresponds to `<VPCPanel>` in `DeviceMappings.xml`; `T-Rudder.buttonMap` corresponds to `<T-Rudder>`.

### 2.3 Input codes supported

A `.buttonMap` file may contain labels for any of:

- Button codes: `Joy_1`, `Joy_2`, ... `Joy_N` (1-based, same convention as `.binds`)
- Full axis codes: `Joy_XAxis`, `Joy_YAxis`, `Joy_ZAxis`, `Joy_RXAxis`, `Joy_RYAxis`, `Joy_RZAxis`, `Joy_UAxis`, `Joy_VAxis`
- Directional pseudo-axes: `Pos_Joy_UAxis`, `Neg_Joy_YAxis`, etc.
- POV/hat codes: `Joy_POV1Up`, `Joy_POV1Down`, etc.

### 2.4 Label values

Observed label text styles:

- Short codes: `B1`, `E1`
- Descriptive names: `T1 - Down`
- Directional indicators: `Clockwise`, `Up`, `Left`
- Axis names: `A1`, `A2`

**Gap — no confirmed icon token syntax.** The source material does not document any structured "icon token" format inside a label value (e.g. a placeholder like `{icon:...}`). Every observed label is plain text. If Elite's own UI renders icons for some devices, the mechanism behind that is not established by the available source material — do not assume a token syntax exists without confirming it against a real `.buttonMap` file taken from a device the in-game UI is known to show icons for.

### 2.5 Fallback behaviour

If a device has no `.buttonMap` file, or a specific input code has no label entry in one that exists, the documented design intent (not confirmed as the game's own internal behaviour, but the reasonable behaviour for a tool consuming these files) is to fall back to a generic label (e.g. "Button 1", "Axis X") and, failing that, to the raw token (e.g. `Joy_4`).

### 2.6 Relationship chain

Conceptually: `DeviceMappings.xml` maps a device's logical name to its VID/PID → that same logical name is also the `.buttonMap` filename stem → the `.buttonMap` file maps raw input tokens (`Joy_N`, axis codes, POV codes) to human labels → those labels are what a UI should display instead of the raw token.

A live-device analogy: a raw SDL-style button index is converted to the game's 1-based `Joy_N` token (see `EliteDangerous-BindsFileFormat.md` §5.3 for the off-by-one conversion), that token is looked up in the appropriate `.buttonMap` file, and the resulting label (e.g. "T1 - Down") is what gets displayed in place of the raw `Joy_13`.

---

## 3. Known Gaps

Carried forward explicitly rather than papered over:

- Exact schema for a device with alternative VID/PID pairs in `DeviceMappings.xml` (§1.3) — real, but shape unconfirmed.
- Exact schema/tag name for any icon-support metadata in `DeviceMappings.xml` (§1.3) — real, but shape unconfirmed.
- Whether `.buttonMap` labels ever encode an icon reference rather than plain text (§2.4) — no evidence found either way in the source material.
- Exact VID/PID → hex-ID formatting details (byte order, case, zero-padding) — see `EliteDangerous-BindsFileFormat.md` §4.2 for the same caveat, since it applies equally to `DeviceMappings.xml` as the origin of those values.

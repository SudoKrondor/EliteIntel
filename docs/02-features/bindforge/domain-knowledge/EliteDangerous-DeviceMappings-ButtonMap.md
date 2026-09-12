# Elite Dangerous — DeviceMappings.xml & .buttonMap Format Reference

**Scope:** The two cosmetic, per-device support files that sit alongside `.binds`: `DeviceMappings.xml` (the device VID/PID registry) and `*.buttonMap` files (human-readable button/axis labels). Neither file affects gameplay — both exist purely to label devices/inputs in the game's own Controls UI (and, by extension, in any external tool that wants to show equivalent labels). See `EliteDangerous-BindsFileFormat.md` §4 for how the `Device=` attribute values these files support relate back to the `.binds` file itself.

**Confidence:** These two formats are documented more thinly in the source material than `.binds` itself — several details below are inferred from a small number of examples rather than exhaustively verified against the full range of real device configurations. Gaps are called out explicitly in §3 rather than filled in with invented detail.

---

## 1. DeviceMappings.xml

### 1.1 File structure

```xml
<DeviceMappings>
    <VPCPanel>
        <PID>0259</PID>
        <VID>3344</VID>
    </VPCPanel>
    <T-Rudder>
        <PID>0BF3</PID>
        <VID>044F</VID>
    </T-Rudder>
</DeviceMappings>
```

- Root element: `<DeviceMappings>`.
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

### 1.3 Additional per-device metadata (partially documented)

The source material notes two further possibilities, without giving a full confirmed schema:

- Some devices may have **alternative VID/PID pairs** — additional child elements beyond the single `<PID>`/`<VID>` pair shown above. Whether this takes the shape of a wrapper element (e.g. something like `<Alternative>`) or a differently-named sibling pair is **not confirmed**.
- Some devices may carry a metadata element related to **icon support** in the game's UI, referred to only generically in the source notes as "`SupportsIcons` or similar." The exact tag name and value shape are **not confirmed**.

**Practical takeaway:** a parser should not assume every device element has exactly two children (`<PID>`, `<VID>`) and nothing else — real devices may carry additional, currently unconfirmed child elements — but no specific additional tag name (`<Alternative>`, `<SupportsIcons>`, or otherwise) should be hardcoded until confirmed against a real multi-VID device's `DeviceMappings.xml`.

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

### 2.5 Fallback behavior

If a device has no `.buttonMap` file, or a specific input code has no label entry in one that exists, the documented design intent (not confirmed as the game's own internal behavior, but the reasonable behavior for a tool consuming these files) is to fall back to a generic label (e.g. "Button 1", "Axis X") and, failing that, to the raw token (e.g. `Joy_4`).

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

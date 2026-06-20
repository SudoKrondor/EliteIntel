# Existing Binding Data Model Audit (Deep Dive)

**Date:** 2026-06-18
**Branch audited:** `V1.1-KAN-6-push-to-talk-dawntreader`
**Scope:** Read-only. No source files were modified during this audit. This is a narrower,
deeper follow-up to `dawntreader-docs/Reports/EXISTING_BINDFORGE_AUDIT.md`, focused purely on
the in-memory Java object model the parser produces (not the DB schema, DAO SQL, or UI), as
prep for designing an extended BindForge data model.

Files read in full for this audit:
- `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java`
- `app/src/main/java/elite/intel/ai/hands/BindingModifier.java`
- `app/src/main/java/elite/intel/ai/hands/BindingSlotType.java` (top-level, separate from the parser's nested enum)
- `app/src/main/java/elite/intel/db/managers/KeyBindingManager.java`
- `app/src/main/java/elite/intel/db/dao/KeyBindingDao.java`

---

## 1. What `KeyBindingsParser` actually returns/populates

`KeyBindingsParser` (`app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java`) is a
singleton with three public parse methods, each building progressively narrower in-memory maps.
None of them write to a database or any other persistent store — all three return plain
in-memory `Map` objects built fresh on every call.

### 1a. `parseReadOnlyBindingSlots` — the "base" parse

```java
// line 156
public Map<String, ReadOnlyBindingSlots> parseReadOnlyBindingSlots(File file) throws Exception {
```

Full signature: `public Map<String, ReadOnlyBindingSlots> parseReadOnlyBindingSlots(File file) throws Exception`.

- Key: `String` — the action's XML tag name (`element.getTagName()`, line 167), used verbatim,
  no transform.
- Value: `ReadOnlyBindingSlots`, a record nested inside `KeyBindingsParser`:

```java
// lines 99-106
/**
 * Read-only Primary/Secondary pair for the Bindings tab.
 * <p>
 * Unlike {@link BindingSlots}, this pair may contain HOTAS, joystick, mouse, or gamepad
 * assignments so the UI can show what exists in the game file without making it executable.
 */
public record ReadOnlyBindingSlots(ReadOnlyBindingSlot primary, ReadOnlyBindingSlot secondary) {
}
```

`ReadOnlyBindingSlots` has exactly two fields, both of type `ReadOnlyBindingSlot` (another
nested record, detailed in section 2). It is purely a Primary/Secondary pair holder — no other
fields.

### 1b. `parseBindingSlots` — filtered to keyboard-usable

```java
// line 135
public Map<String, BindingSlots> parseBindingSlots(File file) throws Exception {
```

Full signature: `public Map<String, BindingSlots> parseBindingSlots(File file) throws Exception`.

- Key: `String` action name (same keys as `parseReadOnlyBindingSlots`, since it iterates that
  map's entries — line 138).
- Value: `BindingSlots`, a record nested inside `KeyBindingsParser`:

```java
// lines 57-61
/**
 * Executable Primary/Secondary slots after non-keyboard assignments have been filtered out.
 */
public record BindingSlots(KeyBinding primary, KeyBinding secondary) {
}
```

`BindingSlots` has exactly two fields, both `KeyBinding` (the parser's own inner class, detailed
in section 2). Internally it is built by calling `toExecutableBinding(ReadOnlyBindingSlot)`
(line 222) on each side of a `ReadOnlyBindingSlots`, which returns `null` if
`!slot.keyboardUsable()` (line 223) — i.e. it is a filter/projection on top of the base parse,
not an independent walk of the XML.

### 1c. `parseBindings` — flattened single-binding-per-action map

```java
// line 114
public Map<String, KeyBinding> parseBindings(File file) throws Exception {
```

Full signature: `public Map<String, KeyBinding> parseBindings(File file) throws Exception`.

- Key: `String` action name.
- Value: `KeyBinding` (the parser's inner class — see section 2). Built by picking
  `entry.getValue().primary() != null ? entry.getValue().primary() : entry.getValue().secondary()`
  (lines 118-120) from the `BindingSlots` map produced by `parseBindingSlots`. This is the map
  actually consulted at command-dispatch time (per the prior audit, held in
  `BindingsMonitor.bindings`).

### Summary of the three return types

| Method | Return type | Value type fields |
|---|---|---|
| `parseReadOnlyBindingSlots` | `Map<String, ReadOnlyBindingSlots>` | `primary: ReadOnlyBindingSlot`, `secondary: ReadOnlyBindingSlot` |
| `parseBindingSlots` | `Map<String, BindingSlots>` | `primary: KeyBinding`, `secondary: KeyBinding` |
| `parseBindings` | `Map<String, KeyBinding>` | (flat) `key: String`, `modifiers: String[]`, `hold: boolean` |

All three are plain `HashMap` instances (`new HashMap<>()` at lines 116, 137, 157) — no custom
Map subclass, no wrapping container, no metadata beyond the map itself.

---

## 2. Existing model/domain classes for parsed binding data (whole-repo search)

A repo-wide search for `KeyBinding`, `BindingSlot`, `BindingEntry`, `BindingElement`, and
related names turned up the following classes that represent parsed `.binds` data (as opposed
to UI/dialog/selection classes, which are listed but not detailed since they wrap rather than
define the model):

### 2a. `KeyBindingsParser.KeyBinding` — non-static inner class

Defined at `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java:45-55`, nested
**inside** the `KeyBindingsParser` singleton class (not `static`, i.e. it is a true inner class
tied to a `KeyBindingsParser` instance, though nothing about its fields actually needs the
enclosing instance):

```java
// lines 39-55
/**
 * Executable binding used by command handling.
 * <p>
 * This model intentionally contains only keyboard bindings that EliteIntel can press.
 * Non-keyboard devices are represented only in the read-only diagnostic models below.
 */
public class KeyBinding {
    public String key;
    public String[] modifiers;
    public boolean hold;

    public KeyBinding(String key, String[] modifiers, boolean hold) {
        this.key = key;
        this.modifiers = modifiers;
        this.hold = hold;
    }
}
```

**Fields (all public, no encapsulation):**
| Field | Type |
|---|---|
| `key` | `String` |
| `modifiers` | `String[]` |
| `hold` | `boolean` |

No `device` field — by design, this class only exists for slots already proven
`Device == "Keyboard"` by the filter in `toExecutableBinding`.

### 2b. `KeyBindingsParser.BindingSlots` — nested record

Defined at `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java:60-61` (shown in
section 1b). Fields: `primary: KeyBinding`, `secondary: KeyBinding`. Nested inside
`KeyBindingsParser`.

### 2c. `KeyBindingsParser.ReadOnlyBindingSlot` — nested record

Defined at `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java:78-97`:

```java
// lines 71-97
/**
 * Read-only representation of one Primary or Secondary slot from a .binds file.
 * <p>
 * It keeps the raw device id and key for UI diagnostics, while {@code keyboardUsable}
 * records whether the slot is eligible for the existing keyboard-only execution path.
 * {@code editable} is narrower: it is true only for slots the basic V1 GUI can safely rewrite.
 */
public record ReadOnlyBindingSlot(
        String device,
        String key,
        String[] modifiers,
        List<BindingModifier> bindingModifiers,
        boolean hold,
        BindingSlotType slotType,
        boolean keyboardUsable,
        boolean editable
) {
    public ReadOnlyBindingSlot {
        modifiers = modifiers == null ? new String[0] : modifiers.clone();
        bindingModifiers = bindingModifiers == null ? List.of() : List.copyOf(bindingModifiers);
    }

    @Override
    public String[] modifiers() {
        return modifiers.clone();
    }
}
```

**Fields:**
| Field | Type |
|---|---|
| `device` | `String` |
| `key` | `String` |
| `modifiers` | `String[]` (defensively cloned in/out via canonical constructor + accessor override) |
| `bindingModifiers` | `List<BindingModifier>` (defensively copied via `List.copyOf` in canonical constructor) |
| `hold` | `boolean` |
| `slotType` | `BindingSlotType` — **the parser's own nested enum** (see section 2e), not the top-level `elite.intel.ai.hands.BindingSlotType` |
| `keyboardUsable` | `boolean` |
| `editable` | `boolean` |

Defined nested inside `KeyBindingsParser`.

### 2d. `KeyBindingsParser.ReadOnlyBindingSlots` — nested record

Defined at `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java:105-106` (shown in
section 1a). Fields: `primary: ReadOnlyBindingSlot`, `secondary: ReadOnlyBindingSlot`. Nested
inside `KeyBindingsParser`.

### 2e. Two distinct `BindingSlotType` types (notable duplication)

There are **two separate, unrelated `BindingSlotType` types** in the codebase:

1. **Nested enum inside the parser** — `app/src/main/java/elite/intel/ai/hands/KeyBindingsParser.java:66-69`:
   ```java
   public enum BindingSlotType {
       PRIMARY,
       SECONDARY
   }
   ```
   This is the type actually referenced by `ReadOnlyBindingSlot.slotType()` and used internally
   within `KeyBindingsParser` (e.g. `readOnlySlot(primary, BindingSlotType.PRIMARY)` at line 177).
   Bare enum, no XML name mapping.

2. **Top-level file** — `app/src/main/java/elite/intel/ai/hands/BindingSlotType.java` (entire
   file):
   ```java
   package elite.intel.ai.hands;

   /**
    * Editable Elite Dangerous binding slots supported by the first keyboard-editing MVP.
    */
   public enum BindingSlotType {
       PRIMARY("Primary"),
       SECONDARY("Secondary");

       private final String xmlElementName;

       BindingSlotType(String xmlElementName) {
           this.xmlElementName = xmlElementName;
       }

       public String xmlElementName() {
           return xmlElementName;
       }
   }
   ```
   This is the type used everywhere **outside** the parser internals: `BindingsWriter.java:210,325`,
   `KeyboardKeyAvailabilityService.java` (6 usages), `KeyboardBindingEdit.java:18`,
   `BindingsGroupTableFactory.java`, `AssignKeyboardBindingSelection.java:11`,
   `BindForgeTabPanel.java`, `AssignKeyboardBindingDialog.java`. It carries an XML-element-name
   string per constant; the parser's nested enum does not.

   Because both are named identically and live in the same package (`elite.intel.ai.hands`),
   any file that wants to use one must fully-qualify or import the right one —
   `ReadOnlyBindingSlot.slotType()`'s field type resolves to `KeyBindingsParser.BindingSlotType`
   (the nested one) purely because it's declared inside `KeyBindingsParser` without import; files
   outside the parser use the top-level one via `import elite.intel.ai.hands.BindingSlotType;`.
   No conversion method exists between the two.

### 2f. `BindingModifier` — top-level record

Defined in its own file, `app/src/main/java/elite/intel/ai/hands/BindingModifier.java:10`:

```java
public record BindingModifier(String device, String key) {
```

**Fields:** `device: String`, `key: String`. That's the entire data shape — two fields. The
rest of the file (lines 11-41) is static allow-list data/helper methods
(`SUPPORTED_KEYBOARD_MODIFIERS`, `SUPPORTED_KEYBOARD_MODIFIER_KEYS`,
`isSupportedKeyboardModifier()`, `supportedKeyboardModifiers()`) — V1 keyboard-editing policy
bolted onto the record, not parsed data.

### 2g. `KeyBindingDao.KeyBinding` — unrelated DB-layer class, same name

Defined as a nested class inside the DAO interface, `app/src/main/java/elite/intel/db/dao/KeyBindingDao.java:47-70`:

```java
class KeyBinding {
    private Integer id;
    private String keyBinding;
    // getters/setters only
}
```

**Fields:** `id: Integer`, `keyBinding: String`. This is **not** a parsed-binding-data model —
per the prior audit, it represents one row of the `bindings` table, which stores
*humanized names of currently-missing bindings* (diagnostic strings), not parsed device/key/
modifier data. It happens to share the simple name `KeyBinding` with
`KeyBindingsParser.KeyBinding` (section 2a) but the two are otherwise unrelated: different
package, different fields, different purpose, no shared interface or supertype.

### 2h. Non-model classes that reference but don't define binding data

These appeared in the grep but only **consume** the model types above; they don't add new
fields to the model itself:
- `KeyboardBindingEdit.java` — a write-request record (slot/key/modifier the user wants written),
  consumes `BindingSlotType` (top-level) and `BindingModifier`.
- `AssignKeyboardBindingSelection` (`ui/support/`) — record `(BindingSlotType, String key, BindingModifier modifier)`, a dialog-result DTO, not parsed data.
- `KeyboardKeyAvailabilityService.SlotAssignment` — private record `(String bindingId, BindingSlotType slotType, String key, BindingModifier modifier)` used internally for collision detection, built by independently re-walking the DOM (not reusing the parser's output).

---

## 3. Class hierarchy among binding model objects

**None found — the structure is entirely flat.** Confirmed by inspecting every model type's
declaration:

- `KeyBindingsParser.KeyBinding` — `public class KeyBinding {` (line 45) — no `extends`, no
  `implements`.
- `KeyBindingsParser.BindingSlots` — `public record BindingSlots(...)` (line 60) — records
  implicitly extend `java.lang.Record` only; no explicit `implements`.
- `KeyBindingsParser.ReadOnlyBindingSlot` — `public record ReadOnlyBindingSlot(...)` (line 78) —
  same, no `implements`.
- `KeyBindingsParser.ReadOnlyBindingSlots` — `public record ReadOnlyBindingSlots(...)` (line 105) —
  same, no `implements`.
- `BindingModifier` — `public record BindingModifier(String device, String key)` (line 10) — no
  `implements`.
- `BindingSlotType` (both the nested one, lines 66-69, and the top-level one) — plain
  `public enum BindingSlotType` — enums implicitly extend `java.lang.Enum` only; neither
  declares `implements` of any project interface.
- `KeyBindingDao.KeyBinding` — `class KeyBinding {` (line 47) — no `extends`/`implements`.

There is no common interface (e.g. no `BindingSlotModel`, no `Bindable`) shared by `KeyBinding`,
`BindingSlots`, `ReadOnlyBindingSlot`, or `ReadOnlyBindingSlots`. There is no base class for
"a parsed binding element." Composition is the only relationship present: `ReadOnlyBindingSlots`
*contains* two `ReadOnlyBindingSlot`s; `BindingSlots` *contains* two `KeyBinding`s;
`ReadOnlyBindingSlot` *contains* a `List<BindingModifier>`. No type in this model is polymorphic
or substitutable for another — every container field has one concrete, named type, never an
interface or abstract supertype.

---

## 4. How Primary/Secondary slots are represented

Primary and Secondary are represented as **dedicated record objects**, not flattened strings —
but only at the `ReadOnlyBindingSlots`/`BindingSlots` level (the pair container). The pair
container itself has exactly two named fields, one per slot, rather than a `List` or `Map`
keyed by slot type:

```java
// BindingSlots — line 60
public record BindingSlots(KeyBinding primary, KeyBinding secondary) {
}

// ReadOnlyBindingSlots — line 105
public record ReadOnlyBindingSlots(ReadOnlyBindingSlot primary, ReadOnlyBindingSlot secondary) {
}
```

Each individual slot is its own object with explicit fields for Device, Key, and (for the
read-only variant) Modifiers:

- `KeyBinding` (executable slot) has `key: String`, `modifiers: String[]`, `hold: boolean` —
  **no `device` field** (device is implicitly always `"Keyboard"` by construction/filtering, so
  it's dropped rather than stored).
- `ReadOnlyBindingSlot` (diagnostic slot) has `device: String`, `key: String`,
  `modifiers: String[]`, `bindingModifiers: List<BindingModifier>`, `hold: boolean`,
  `slotType: BindingSlotType`, `keyboardUsable: boolean`, `editable: boolean` — this one keeps
  `device` explicitly (lines 79-80) since it must represent non-keyboard devices too.

Note `ReadOnlyBindingSlot` itself also records which slot position it is
(`slotType: BindingSlotType`, line 84) — i.e. the slot knows its own Primary/Secondary identity
independently of which field of `ReadOnlyBindingSlots` it's stored in. This is redundant
information (the pair container also encodes position positionally) but it is present.

---

## 5. How Modifiers are represented

Modifiers are represented in **two parallel forms simultaneously** on `ReadOnlyBindingSlot`, and
in a **single flattened-string-array form only** on the executable `KeyBinding`:

**On `ReadOnlyBindingSlot` (lines 78-87):**
```java
String[] modifiers,
List<BindingModifier> bindingModifiers,
```
- `bindingModifiers: List<BindingModifier>` is the authoritative structured form — each element
  is a `BindingModifier(String device, String key)` record (section 2f), built directly from XML
  `<Modifier Device="..." Key="...">` children by `getBindingModifiers(Element binding)`:
  ```java
  // lines 245-255
  private List<BindingModifier> getBindingModifiers(Element binding) {
      NodeList children = binding.getChildNodes();
      List<BindingModifier> modifiers = new ArrayList<>();
      for (int i = 0; i < children.getLength(); i++) {
          Node child = children.item(i);
          if (child instanceof Element modifier && "Modifier".equals(modifier.getTagName())) {
              modifiers.add(new BindingModifier(modifier.getAttribute("Device"), modifier.getAttribute("Key")));
          }
      }
      return modifiers;
  }
  ```
- `modifiers: String[]` is a **derived, lossy** projection of the same data — just the `key`
  strings, device info dropped:
  ```java
  // lines 257-261
  private String[] modifierKeys(List<BindingModifier> modifiers) {
      return modifiers.stream()
              .map(BindingModifier::key)
              .toArray(String[]::new);
  }
  ```
  Both fields are populated from the same `getBindingModifiers()` call inside `readOnlySlot()`
  (lines 209, 213) — i.e. the record carries redundant data, one structured+complete, one
  flat+lossy, for the same underlying XML content.

**On `KeyBinding` (executable, line 47):**
```java
public String[] modifiers;
```
Only the flat `String[]` form survives into the executable model — `toExecutableBinding()`
(lines 222-225) calls `slot.modifiers()` (the lossy array accessor) when building a `KeyBinding`,
so `BindingModifier`/device-per-modifier information is discarded entirely once a slot is
deemed keyboard-usable. There is no `List<BindingModifier>` field anywhere on `KeyBinding` or
`BindingSlots`.

Nowhere is a modifier represented as a single concatenated string (e.g. `"Ctrl+Shift"`) in the
parsed model itself — that kind of concatenation only happens in display-formatting code
(`BindingSlotDisplayFormatter`, per the prior audit), not in the model.

---

## 6. `ToggleOn`, `Hold`, `Inverted`, `Deadzone` representation

| XML element/attribute | Represented in model? | Evidence |
|---|---|---|
| `Hold` (attribute on `<Primary>`/`<Secondary>`) | **YES** | Read at `KeyBindingsParser.java:208`: `boolean hold = "1".equals(slot.getAttribute("Hold"));`. Stored as `hold: boolean` on both `ReadOnlyBindingSlot` (line 83) and `KeyBinding` (line 48). Passed through `toExecutableBinding()` (line 224: `new KeyBinding(slot.key(), slot.modifiers(), slot.hold())`). |
| `ToggleOn` (separate XML element, sibling of Primary/Secondary under the action) | **NOT represented anywhere.** | No occurrence of the string `"ToggleOn"` anywhere in `KeyBindingsParser.java` (confirmed by reading the full file — only `Device`, `Key`, `Hold`, and `Modifier` attributes/tags are read). No field named `toggleOn`/`toggle` exists on `KeyBinding`, `BindingSlots`, `ReadOnlyBindingSlot`, or `ReadOnlyBindingSlots`. This matches the prior audit's finding (`EXISTING_BINDFORGE_AUDIT.md` line 287, 325, 379) and is independently confirmed here at the field level: there is no slot in any record/class signature where a `ToggleOn` value could even be assigned. |
| `Inverted` (AXIS-only element, e.g. `<Inverted Value="1"/>`) | **NOT represented anywhere.** | `KeyBindingsParser` only ever calls `element.getElementsByTagName("Primary")` / `"Secondary"` (lines 169-170); it never looks for `<Binding>`, `<Inverted>`, or any AXIS-specific tag. No field named `inverted` exists on any model class/record in the parser, `BindingModifier`, or `BindingSlotType`. |
| `Deadzone` (AXIS-only element, e.g. `<Deadzone Value="..."/>`) | **NOT represented anywhere.** | Same as `Inverted` — no AXIS parsing path exists at all in `KeyBindingsParser`, so there is no opportunity for a `deadzone` field to exist. Confirmed no field of that name in any of the model types read. |

Searched locations for all four: the full text of `KeyBindingsParser.java` (parser logic and
all nested model types), `KeyBindingDao.java` (DAO model — only has `id`/`keyBinding`, unrelated
to slot content), `KeyBindingManager.java` (manager — no model fields of its own),
`BindingModifier.java`, and the top-level `BindingSlotType.java`. None of these five files
contain the strings `ToggleOn`, `Inverted`, or `Deadzone` outside of this report. `Hold` is the
only one of the four actually wired into the model, and only as a slot-level boolean attribute,
never as an AXIS concept.

---

## 7. What `KeyBindingManager` does with parsed model data

**`KeyBindingManager` never touches `KeyBindingsParser`'s output at all.** It is a completely
separate code path operating on a different model. Full file content
(`app/src/main/java/elite/intel/db/managers/KeyBindingManager.java`):

```java
package elite.intel.db.managers;

import elite.intel.db.dao.KeyBindingDao;
import elite.intel.db.dao.KeyBindingDao.KeyBinding;
import elite.intel.db.util.Database;

import java.util.List;

public class KeyBindingManager {
    private static volatile KeyBindingManager instance;

    public static KeyBindingManager getInstance() {
        if (instance == null) {
            instance = new KeyBindingManager();
        }
        return instance;
    }

    // Add a binding
    public void addBinding(String binding) {
        Database.withDao(KeyBindingDao.class, dao -> {
            dao.save(binding);
            return Void.class;
        });
    }

    // remove a binding
    public void removeBinding(String binding) {
        Database.withDao(KeyBindingDao.class, dao -> {
            dao.removeBinding(binding);
            return Void.class;
        });
    }

    // clear table data
    public void clear() {
        Database.withDao(KeyBindingDao.class, dao -> {
            dao.clear();
            return Void.class;
        });
    }

    // Get all bindings
    public List<KeyBinding> getMissingBindings() {
        return Database.withDao(KeyBindingDao.class, KeyBindingDao::listAll);
    }
}
```

Notes on what this actually shows:

- The import on line 4, `import elite.intel.db.dao.KeyBindingDao.KeyBinding;`, is the **DAO's**
  `KeyBinding` (section 2g — `id`/`keyBinding` string pair), **not**
  `KeyBindingsParser.KeyBinding` (section 2a — `key`/`modifiers`/`hold`). `KeyBindingsParser` is
  not imported or referenced anywhere in this file.
- `addBinding(String binding)` and `removeBinding(String binding)` take a plain `String` —
  per the prior audit, this is the *humanized* missing-binding name, not a parsed binding object.
  The manager does no parsing, formatting, or validation of that string; it is passed straight
  to `dao.save(binding)` / `dao.removeBinding(binding)`.
- `getMissingBindings()` returns `Database.withDao(KeyBindingDao.class, KeyBindingDao::listAll)`
  — a direct method-reference pass-through of the DAO call with **zero additional logic**: no
  mapping, no filtering, no wrapping, no caching. The return type `List<KeyBinding>` (DAO's
  inner class) is returned completely unchanged from what `KeyBindingDao.listAll()` produces.
- `clear()` likewise passes straight through to `dao.clear()`.
- There is no method on `KeyBindingManager` that accepts or returns any of
  `KeyBindingsParser.KeyBinding`, `BindingSlots`, `ReadOnlyBindingSlot`, `ReadOnlyBindingSlots`,
  or `BindingModifier`. The manager class adds no structure, indexing, or metadata on top of
  parser output for the simple reason that **it never receives parser output in the first
  place** — its sole job is CRUD over the `bindings` (missing-bindings) table via the DAO, which
  is conceptually downstream of the parser/monitor pipeline (per the prior audit,
  `BindingsMonitor.checkForMissingBindingsAndPersist()` is what bridges parsed data to this
  manager, by computing a list of missing humanized names and presumably calling
  `addBinding`/`removeBinding` — that bridging logic lives in `BindingsMonitor`, not in
  `KeyBindingsParser` or `KeyBindingManager` itself, and was out of scope for this file-level
  read).

---

## Summary for BindForge model design

Factual characterization of gaps in the current in-memory model relative to the full `.binds`
schema (AXIS / BUTTON / STANDALONE SETTING, Primary+Secondary+Modifiers, ToggleOn, Inverted,
Deadzone), based on the class-level evidence above:

- **The entire model is BUTTON-only and flat.** Every model type (`KeyBinding`, `BindingSlots`,
  `ReadOnlyBindingSlot`, `ReadOnlyBindingSlots`, `BindingModifier`) is a plain class/record with
  no inheritance, no shared interface, and no representation of AXIS (`<Binding>`, `<Inverted>`,
  `<Deadzone>`) or STANDALONE SETTING (`Value=`) elements — those element types have zero
  corresponding fields anywhere in the model.
- **`ToggleOn` is entirely absent** from every model class — no field, no parsing call, no
  partial support; same for `Inverted` and `Deadzone`. Only `Hold` is wired through, and only as
  a plain `boolean` on the slot.
- **Modifiers are represented redundantly and lossily across two model layers**:
  `ReadOnlyBindingSlot` keeps both a structured `List<BindingModifier>` (device+key per modifier)
  and a derived flat `String[]` (key only); the executable `KeyBinding` keeps only the flat,
  device-less `String[]` form, discarding per-modifier device information once a slot is
  classified as keyboard-usable.
- **Primary/Secondary are dedicated per-slot objects, but only paired via two named fields**
  (`primary`/`secondary`) on a wrapper record, not via a `List`/`Map` keyed by slot type — adding
  a third slot type (or per-slot extensibility) would require widening the wrapper record's
  shape, not just adding list entries.
- **Two independently-defined, identically-named `BindingSlotType` enums coexist** in the same
  package (`elite.intel.ai.hands`) with different members and no conversion between them — one
  nested inside `KeyBindingsParser` (used only by `ReadOnlyBindingSlot.slotType()`), one top-level
  (used by the writer, dialogs, and UI). Any BindForge model unification needs to resolve or
  consciously preserve this split.
- **The DB-facing `KeyBindingManager`/`KeyBindingDao` model is entirely disconnected from the
  parser's model** — it stores only humanized missing-binding name strings
  (`KeyBindingDao.KeyBinding{id, keyBinding}`), never device/key/modifier/axis data, and
  `KeyBindingManager`'s methods are pure passthroughs to the DAO with no added structure. There
  is no persisted, queryable representation of a full parsed binding (across any element type)
  anywhere in the codebase — every parse is transient and in-memory only.

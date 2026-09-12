# StarVizion — StarCalc Expression Engine

StarCalc is StarVizion's built-in expression evaluation system. It lets users write simple formulas that combine input values and constants to produce derived outputs — average two axes into a composite thrust level, compute a ratio, apply conditional logic, normalize or scale an axis range.

## Expression Syntax

StarCalc expressions are evaluated per frame. Supported constructs:

- **Arithmetic:** `+`, `-`, `*`, `/`, `%`, `^` (power)
- **Comparison:** `==`, `!=`, `<`, `<=`, `>`, `>=`
- **Logic:** `&&`, `||`, `!`
- **Ternary:** `condition ? valueIfTrue : valueIfFalse`
- **Built-in functions:** `sin()`, `cos()`, `tan()`, `abs()`, `min()`, `max()`, `clamp()`, `round()`, `floor()`, `ceiling()`, `sqrt()`, `ln()`, `log10()`, `atan2()`
- **Input references:** user-named variables bound to device inputs (axes as floats −1.0 to 1.0, buttons as booleans)

## Local (Intermediate) Expressions

A NeuroNode can define **local expressions** — named computed values evaluated before the node's property expressions, and available within them exactly like variables. Locals let a complex calculation be written once and referenced multiple times across different properties, instead of repeating it in every property expression that needs it.

**Evaluation order within a frame:**
1. Input variables are read from the device
2. Window and time variables are updated
3. Locals are evaluated top to bottom — each local can reference locals defined above it
4. Property expressions are evaluated using all variables and locals

**Scope:** locals are per-NeuroNode. A local defined in one node is not visible to other nodes in the same Vizlet. Vizlet-wide shared locals are a planned future direction — see [Roadmap](roadmap.md#vizlet-wide-shared-locals).

**In the editor:** locals appear as an editable list above the property expressions in the property panel. Each row is a name and an expression field; rows can be added, removed, and reordered.

## Parametric Evaluation

For Parametric primitives, StarCalc is evaluated in a loop — the sweep parameter is injected at each step alongside all standard variables, while all input, window, and time variables remain constant across the steps within a single frame. This means a Parametric node's shape can be both mathematically defined and reactive to input simultaneously.

## Error Handling

An invalid expression is flagged in the editor with a visual indicator on the node. At runtime, a failed expression displays a fallback placeholder value and logs the error. A failed expression on one node does not affect other nodes.

## Telemetry Expressions (Future)

When game telemetry is integrated (see [Telemetry](../../03-data-models/telemetry.md)), StarCalc expressions will be able to reference journal-derived fields — ship status, cargo load, current game mode. In v1, StarCalc operates on controller input only.

---

## Variable Contract

Every StarCalc expression evaluates inside a context — a set of named variables and functions available at evaluation time. The editor exposes all of these to the user; expressions can reference any variable or function listed here.

### Input Variables

Input variables are per-NeuroNode and user-named — the user defines the name and maps it to a physical device input in the node's input-bindings panel. The expression can then reference that chosen name; the naming is entirely up to the user, nothing is fixed.

| Input Kind | Value Type | Range |
|---|---|---|
| Axis | float | −1.0 to 1.0 |
| Button | boolean | true / false |
| Hat switch | boolean | true / false — each hat direction is a separate button |

Hat switches are treated as individual buttons regardless of how the hardware reports them — a 4-way hat exposes four boolean bindings, an 8-way hat exposes eight. StarVizion does not treat hat switches as axes or POV values.

**"Regardless of how the hardware reports them" is doing real work here.** Confirmed 2026-09-06: the hardware
reports a hat as a single 4-bit value (`0–7`, plus null for centred), never as booleans. The expansion into
per-direction booleans is StarVizion's translation to perform.

**StarVizion and BindForge deliberately use different hat models, and that is not a defect to reconcile.**
StarVizion shows what the hardware does, so an 8-way hat gets eight booleans and a diagonal is its own
state. BindForge edits what Elite binds, and `.binds` names only four POV tokens per hat, so a diagonal is
two tokens held together. Same hardware, two correct answers, because the questions differ.

If an expression references a variable that has not been bound, it fails validation. An unbound variable is a hard error, not a silent default.

### Window Variables

Always available to every NeuroNode with no setup required — they reflect the Vizlet's current window geometry: width, height, center X (width/2), center Y (height/2), and aspect ratio (width/height). These let an expression like "axis value times half the window width, offset from center" work correctly regardless of the Vizlet's actual size.

### Time Variables

Time variables enable animation — behavior that changes over time with no input required: total elapsed seconds since the Vizlet started, and elapsed seconds since the previous frame. Both are read-only and monotonic. Example: a dot that pulses in size using a sine wave of elapsed time.

### Helper Functions

In addition to the standard math functions above, StarCalc provides:

| Function | Description |
|---|---|
| `clamp(x, min, max)` | Restrict a value to a range |
| `lerp(a, b, factor)` | Linear interpolation from a to b by the given factor |
| `map(v, inMin, inMax, outMin, outMax)` | Remap a value from one range to another — for example, translating a raw axis range into a pixel range in one call |
| `smooth(value, speed)` | Exponential smoothing that damps sudden changes over time; `speed` is a unitless strength value from 0.0 (no smoothing) to 10.0 (heavy, slow, floaty smoothing) |

### Reserved Names

`width`, `height`, `centerX`, `centerY`, `aspectRatio`, `time`, `deltaTime`, `t`, `pi`, `e`, `true`, `false` cannot be used as input-binding variable names. `t` is reserved specifically because it is injected automatically as the sweep parameter during Parametric primitive evaluation.

### Built-in Constants

`pi` and `e` are available in all expressions with no setup required.

### Color Values in Expressions

Color properties accept hex strings, and expressions can produce a color string conditionally (for example, a color that switches based on a button state). Supported formats: `#RRGGBB` and `#RRGGBBAA`.

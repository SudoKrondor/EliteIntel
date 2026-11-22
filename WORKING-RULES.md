# Working Rules

Three checks Alan runs while Claude is building. Keep this on screen - pinned tab, scratch
file, or a sticky note - and ask them out loud rather than trusting that the work looks right.

They exist because work built from the wrong premise looks exactly like work built from the
right one. See **Before Building a Feature** in [CLAUDE.md](CLAUDE.md) for Claude's half of
the same rule, and for the incident that produced both.

---

## 1 - Which doc, which section?

Ask **before** Claude builds anything.

If the answer is not a doc path and a heading, Claude is off the map. Stop there - do not
let it start and check later.

## 2 - Show me the sources table.

Before any code: every field, column and control the screen shows, and where its data comes
from. One row each.

You are approving the **sources**, not the code. If it takes longer than 60 seconds to scan,
it is not a plan yet - send it back.

## 3 - Are you still on spec?

Ask at the **start** of each slice, not the end.

Drift accumulates one plausible step at a time, and no single step looks wrong. The spec doc
is the only instrument that measures the total.

---

## Sticky-note version

Plain text, no list syntax - sticky-note apps auto-bullet and will double up the numbering.
Lines are kept under 35 characters so nothing wraps in a narrow column.

```
WORKING RULES

1 - WHICH DOC, WHICH SECTION?
Ask before Claude builds.
No heading = off the map. Stop.

2 - SHOW ME THE SOURCES TABLE.
Every field on screen, and where
its data comes from. One row each.
Can't scan in 60s = not a plan.

3 - ARE YOU STILL ON SPEC?
Ask at the START of each slice.
Drift adds up one plausible
step at a time.
```

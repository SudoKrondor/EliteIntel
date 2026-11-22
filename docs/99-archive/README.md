# Archive — historical record, not live documentation

**Everything in this folder is closed. Do not edit it to match current design, and do not cite it as current
design.**

These files record *why* decisions were reached, including decisions that have since been reversed. Their value
is that they were not rewritten. An entry that contradicts a live document is not a bug in this folder — it is
an old answer, correctly preserved. The live answer lives in the live document.

| Read this as | Not as |
|---|---|
| "this is what we thought on that date, and why" | "this is how BindForge works" |
| evidence for a decision's history | a specification |

**If something here contradicts a live doc, the live doc wins** — and the fix belongs in the live doc, which
should say what changed and why, and may link back here for the superseded reasoning. Nothing here gets
corrected, struck through or brought up to date.

## What is in here

| File | What it is | Closed |
|---|---|---|
| [conflicts-and-open-questions.md](conflicts-and-open-questions.md) | The numbered list of design conflicts carried over from the EDO StellarCore port, each with how it was settled. Its own header lists the standing corrections the port made. Entries about Elite Dangerous itself — file formats, conflict resolution, device identity, install paths — are still factually accurate; entries about hosts, plugins and the working-copy model are superseded. | 2026-09-17 |

## Where live answers live instead

- Current terminology — [glossary.md](../00-overview/glossary.md)
- What is in and out of V1.2 — [v1.2-scope.md](../00-overview/v1.2-scope.md)
- Open questions the port created — [PORTING-NOTES.md](../PORTING-NOTES.md)
- Ideas considered and parked — [04-other-ideas/](../04-other-ideas/)
- Things that need hands-on testing — [testing-required.md](../00-overview/testing-required.md)

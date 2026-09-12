# BindForge — Roadmap and Open Items

## Post-V1.2 / Back-Burner Ideas

**Controller Mode** — a visual, picture-based view of the physical controller with bindings overlaid on the corresponding physical buttons/axes. No implementation scope defined yet.

**Control Types** — see [Bind Editor — Control Types](bind-editor.md#control-types). Confirmed out of v1 scope (see [v1.2-scope.md](../../00-overview/v1.2-scope.md)).

## Formerly "Needed Before V1 Can Be Built" — Now Resolved

**Input Mode design** — done. See [Bind Editor — Input Mode](bind-editor.md#input-mode).

**A single, consolidated bind data table** — done. [`BindForge_ConsolidatedActionTable.xlsx`](reference-data/BindForge_ConsolidatedActionTable.xlsx) merges all three prior sources into one authoritative 482-action table:
- [`EliteDangerous-ActionCatalog.md`](domain-knowledge/EliteDangerous-ActionCatalog.md) (domain knowledge) — In-Game Name, XML Element, Bindable. Screenshot-and-`.binds`-cross-checked, includes known correction history.
- [`ActionCatalog.json`](reference-data/ActionCatalog.json) (reference data) — name, XML element, occasional source-provenance tag. Independently built from a third-party binds-export tool plus screenshots; category naming doesn't fully match the domain-knowledge list (this was [conflict 3.12](../../00-overview/conflicts-and-open-questions.md#312-actionsubgroup-taxonomy--resolved-reconciliation-complete)).
- [`BindForge_Binding_Zone_Map.xlsx`](reference-data/BindForge_Binding_Zone_Map.xlsx) (reference data) — the richest of the three: Group, Subgroup, In-Game Name, XML Element, Bindable, Notes, and a **Mode Type** classification (Always Active / Conditional / Exclusive Overlay, with an explanatory note and a confidence rating per row) — this is the raw source data behind [Bind Editor's context-aware conflict findings](bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing).

All three source files are preserved untouched, as-is, for provenance. The consolidated table is built on the xlsx's structure (the only source with Mode Type, and one of the two covering all 482 actions, bindable and settings-only alike). **No data was discarded where sources disagreed on an action's name** — the xlsx and domain-knowledge markdown agreed with each other on every one of the 62 naming conflicts found; `ActionCatalog.json` was the outlier in each case (usually a wording/punctuation difference, occasionally a genuinely different label). Rather than picking a winner, each conflict got a second row inserted directly beneath the primary one, carrying the alternate name and which source used it — both rows share the same XML Element, Bindable, and Mode Type data, since that part was never in dispute. The domain-knowledge markdown's own Needs-Review annotations (the `UI_Select` and `ExplorationFSSEnter` corrections, the two intentionally-Yes settings-only mouse rows) and the JSON's provenance tags (inferred/screenshot sourcing, corrected CSV typos) were carried forward as row notes rather than lost in the merge. See the table's own README sheet for the full methodology.

None of the three source files includes a structural axis/button/toggle classification per action — that's intentional, not a gap, since [Binding Schema](../../03-data-models/binding-schema.md) already establishes that a binding element's kind is meant to be determined by parsing a real `.binds` file's structure at runtime, never from a name-matched lookup table.

## Open Items Carried From BindForge's Punchlists

- **Control Types' foundational questions** — see [Bind Editor — Control Types](bind-editor.md#control-types). Tagged blocking in BindForge's current punchlist: whether it's a standalone mode or a filter, and whether it has real value beyond axis review. Moot for v1 now that Control Types is confirmed out of scope, but still worth resolving before deciding whether to build it post-v1.
- **Cross-section conflict matrix** — whether top-level sections (General/Ship/SRV/On Foot) are truly isolated from each other for conflict purposes, or whether a cross-section matrix is needed in addition to the four per-section matrices. See [Bind Editor — Shared Conflict Detection](bind-editor.md#how-the-game-actually-resolves-conflicts-confirmed-by-direct-in-game-testing).
- **UI-action-vs-ship-action behavioral safety** — confirmed assignable with no in-game warning, but whether it's actually safe when both are live simultaneously needs a second, behavior-focused round of testing. See the same section above.
- **On Foot conflict coverage** — only three On Foot subgroups have been identified/tested so far; real on-foot play likely has more untested contexts (ship-interior actions while on foot, SRV boarding, taxi/Apex travel).

See [conflicts-and-open-questions.md](../../00-overview/conflicts-and-open-questions.md) for every place different design generations disagree on scope, naming, or mechanism rather than simply being at different stages of the same idea.

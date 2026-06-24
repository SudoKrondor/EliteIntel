# CODING_STANDARD.md

Coding standard for EliteIntel (Java 21, Gradle multi-module, event-driven Swing desktop app). Hard architectural constraints (EventBus decoupling, journal-only game data, no API keys in logs)
live in `CLAUDE.md` and take precedence where they overlap.

## Design Principles

### Design Priorities

- Clarity > Cleverness
- Simplicity > Flexibility
- Current needs > Future possibilities
- Explicit > Implicit
- Low cognitive load > Clever or messy solutions

### Design Rules

- Single responsibility per unit (method, class, subscriber, manager).
- Keep concerns separated; do not mix unrelated responsibilities in one class.
- Favour extension without modification (Open/Closed) where it does not add speculative abstraction.
- Depend on interfaces at module seams (this project already does: `EarsInterface`, `MouthInterface`,
  `AiCommandInterface`, etc.).
- Subtypes must be substitutable for their base types (Liskov).
- Prefer specific interfaces over broad ones (Interface Segregation).
- Weigh long-term maintainability in design decisions.

### Design Anti-Patterns

DO NOT:

- Mix multiple responsibilities in one unit.
- Create tight coupling between modules. Cross-module communication goes through the EventBus, not direct references.
- Introduce hidden dependencies, implicit behaviour, or implicit ordering between components.
- Leak implementation details of a library or layer through an interface. Callers must not need to know vendor names, config keys, or transport details.
- Handle the same concept (value type, missing-value representation, naming) differently across code paths. New code matches how the rest of the system already handles it.

## Robustness

This project favours **graceful degradation for intentional, documented cases** and **fail-fast for genuinely unexpected
states**. The two are not in tension when applied deliberately.

- **Fail fast on the unexpected.
  ** When an invariant is violated or input is impossible per the design, throw an exception with a clear, actionable message rather than limping along with a guessed default. Do not silently swallow it.
- **Graceful degradation is allowed only as a deliberate product decision, and must be documented with a `// WHY:`
  comment.** Examples already in the codebase:
  `EventsTextProvider` falls back to English for a missing localization key; the app waits for a journal file instead of crashing when started before the game; optional STT/TTS/LLM backends degrade rather than abort. These are features, not masked bugs.
- **Never use a fallback to hide a real failure.** A
  `catch` that substitutes a default to make a symptom disappear is a bug, not degradation.
- Make illegal states unrepresentable: prefer enums, typed DTOs/records,
  `final` fields, and constructors that reject invalid input over flag soup and post-hoc validation.
- Prefer constructing typed objects (DTOs/records) over passing raw `Map`/
  `JsonObject` around. Direct construction surfaces typos and type errors at compile time.
- Removing a validation guard requires replacing it with an equivalent (a constructor check, a type constraint, a Bean Validation annotation).

## Simplicity

### Quality Standards

- KISS, YAGNI, DRY.

### Simplicity Rules

- Less code is better than more code.
- Reach for the simplest solution that fully solves the current problem.
- Every class, field, and parameter must earn its place.

### Simplicity Anti-Patterns

DO NOT:

- Add "just in case" features or speculative configuration (YAGNI).
- Implement future requirements that were not asked for (YAGNI).
- Create abstractions with a single caller and no second use in sight (YAGNI).
- Add helper methods or parameters that were not requested or needed.
- Introduce code, libraries, or approaches that were not asked for.
- Change existing design unless that is the task.
- Leave obsolete code behind. Remove it.

## Code Structure

### Structure Rules

- Use guard clauses to reduce nesting: handle invalid cases early and return/continue, keep the happy path unindented.
- **Aim for methods under ~40 lines.
  ** This is a guideline, not a hard gate: a single cohesive operation (e.g. a tight I/O loop) may run longer when splitting it would obscure the flow, but it must carry a
  `// WHY:` explaining the cohesion. Default to smaller.
- Initialize variables with sensible defaults up front rather than across conditional branches.
- Keep cyclomatic complexity low; prefer several small methods over one branching giant.
- Names of variables, classes, methods must be self-explanatory without reading the surrounding code.
- Domain-layer names must not reference a specific adapter or external API (keep `gameapi`/
  `session` names game- and vendor-neutral where practical; see the plugin-direction note in project memory).
- Code is read far more than written. Optimize for the reader.
- Place new files in the package that matches their responsibility; tests mirror the source package (see Testing).

Guard-clause pattern (Java):

```java
// BAD - nested
public void handle(Item item) {
    if (!isIgnored(item)) {
        if (!existsInDb(item)) {
            process(item);
            save(item);
        }
    }
}

// GOOD - guard clauses, happy path unindented
public void handle(Item item) {
    if (isIgnored(item)) return;
    if (existsInDb(item)) return;
    process(item);
    save(item);
}
```

### Top-Down Organization

- The entry point / highest-level method appears first in the class.
- Callers are ordered before callees so the file reads top to bottom as a flow.
- Orchestration before implementation detail.

### Comments

- Most code should be self-explanatory through good names. Refactor for clarity before reaching for a comment.
- Only two comment kinds are encouraged:
    - Javadoc (`/** ... */`) - explain purpose when it is not obvious from the signature.
    - `// WHY: <reason>` - explain the rationale when it is not obvious from the code.
- Comments that restate WHAT the code does are discouraged.
- Preserve existing `// WHY:` comments unless they become irrelevant after a change.
- When you change behaviour, update or delete any Javadoc/comments describing the old behaviour.

## Error Handling

- Never silently skip or ignore errors, duplicates, or constraint violations.
- Handle errors explicitly and log them with enough context (log4j2 is already used project-wide).
- Catch specific exceptions, not `Exception`/`Throwable`, except at a deliberate top-level boundary (thread
  `run()`, EventBus handler edge) where you log and keep the app alive. Comment why the boundary is broad.
- Never log or transmit API keys or secrets.

## Testing

The project uses JUnit 5. Tests live under `app/src/test`, mirroring the source package. The default
`test` task runs against an in-memory SQLite DB (`elite.intel.db.url` system property); live-LLM checks are tagged
`local-integration` and excluded from the default run.

- Add tests for new logic.
- Tests mirror the source package structure.
- Tests follow the same standards as production code.
- Avoid test duplication: if a case is a strict subset of a broader test, drop the redundant one.
- Design for testability (constructor injection, pure helpers, seams like the DB system property).

### Testing Anti-Patterns

DO NOT:

- Remove or disable still-relevant tests instead of fixing them. When a responsibility moves, move its test to follow it (do not just delete the assertion).
- Write tests for states the type system makes impossible.
- Place test files outside the package that mirrors the source.

## Dependencies & Libraries

- Keep dependencies minimal.
- Investigate a library's intended syntax/patterns before abandoning it over an initial snag.
- When adding a dependency, state it explicitly (module `build.gradle`) and call it out for review.

## API & Compatibility

- Do not introduce breaking changes to event shapes (`BaseEvent` subclasses /
  `EventRegistry`), public handler contracts, or DB migration history without explicit approval.
- DB schema changes go through a new versioned migration in
  `app/src/main/resources/db-migration/`; never edit an applied migration.

## Security

- Consider security implications of changes.
- API keys are encrypted at rest via `Cypher` and accessed through `ConfigManager`; never log, print, or transmit them.
- Honour the hard constraint: game data comes only from the journal/EDSM/Spansh. No memory reading, injection, or overlays.

## Performance

- Do not optimize prematurely.
- Be mindful of the event-driven hot paths (journal parsing, STT/TTS threads); avoid blocking the EDT and avoid unnecessary work per journal line.

## AI Assistant Behavior

- You may suggest new design or libraries, but get confirmation before applying them.

### Starting Tasks

- Seek clarity on requirements before coding; ask rather than assume.
- Understand the current architecture and identify the files to change.
- Break large or vague tasks into smaller steps, or ask for clarification.
- Read entire files before editing to understand context and avoid duplication.

### Working with Libraries

- Look up current syntax/usage when unsure (web search or official docs).
- You may suggest a better library, but if the user keeps theirs, use it. Do not swap a library without approval.

### Handling Violations

1. Name the specific principle breached.
2. Explain the violation in plain English.
3. Offer the simplest correction.

### Continuous Focus

- Watch for and correct scope creep and unnecessary complexity.

### Quality Assurance

- Implement real solutions, not stubs.
- Do not perform large refactors unless explicitly instructed.
- Find the root cause of repeated issues instead of trying random fixes.
- Run the full relevant test suite after touching shared code, not just the tests you added.

### Linting Anti-Patterns

DO NOT:

- Suppress warnings with `@SuppressWarnings` or by loosening project settings instead of fixing the cause.
- Formally bypass a problem rather than fixing it (e.g. wrapping a real failure in a swallowing try/catch, or shortening a method by extracting code without regard to responsibility).

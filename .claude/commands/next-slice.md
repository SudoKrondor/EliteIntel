---
description: Start the next BindForge build slice from its brief in docs/build/
argument-hint: "<section>  — core, alias-designer, preset-editor, file-manager, ..."
---

Section: $ARGUMENTS

You are building one slice of BindForge. The plan and the routine are in `docs/build/`, and they replace the
conversation history a long session would have carried — so follow them rather than improvising.

# 1. Before anything else

- Run `git fetch` and `git status -sb`. If the branch is **behind** its remote, stop and tell Alan to pull
  (`git pull --ff-only`). Krondor pushes several times a day; building on a stale branch means a rebase later.
- If the working tree has uncommitted changes, say what they are and ask before going on.

# 2. Read

1. `docs/build/README.md` — the routine and the rules every section inherits.
2. `docs/build/<section>.md` for the section named above. If none was given, list the sections from the README
   and ask which.
   - **If the brief does not exist yet**, writing it is this session's slice. Follow "Writing a new brief" in
     the README, read the section's spec end to end to do it, and stop when the brief is written.
3. Take the slice marked **next**. If it is **blocked**, say why and which slice could be done instead, and ask.
4. **Read the spec sections that slice depends on, end to end** — not a search result, not the line you think
   you need. The rule is in `CLAUDE.md`, and it exists because skipping it built the wrong screen once.

# 3. Plan, then wait

Tell Alan in a few lines which slice this is and what it will produce. Then show the **sources table**: every
field, behaviour or file the slice touches, and where each comes from — an existing class (with its path), a
table, a service, or "to build". Flag anything the docs disagree about, and anything the slice would need from
shared code (`bindforge.io`, `bindforge.rules`, `elite.intel.io`, `AppPaths`).

**Wait for Alan's approval.** He approves the sources, not the code.

# 4. Build

- That slice and nothing else. Something out of scope goes into the brief's hand-off notes, not the code.
- A decision made while building is recorded in the **spec** it belongs to, with a date — not only in the brief.
- Gradle cannot run from this session. When the code and its tests are written, ask Alan to run
  `.\gradlew app:test` and report back.

# 5. When Alan says the tests pass

1. Update `docs/build/<section>.md`: mark the slice **done** with the date, add a hand-off note at the top of
   that list — what was decided and where it is recorded, what was found, what the next slice needs — and
   mark the following slice **next**. Fix the brief's "Built" table if it changed.
2. Run `python scripts/check_docs.py` and fix anything it reports.
3. Give Alan a commit message: a one-line summary, then short bullet points, lines not hard-wrapped, ending
   with the attribution line your instructions specify.
4. **Never commit, push or merge.** Alan does all of that.

# 6. Stop

Say the slice is finished and name the next one. **Do not start it in this session**, even with room to spare —
the next session starts fresh, and that is the point of working this way.

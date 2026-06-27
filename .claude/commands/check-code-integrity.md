---
description: Review a change set (working tree, branch, or GitHub PR) against CODING_STANDARD.md
argument-hint: "[PR number/URL | branch/ref | nothing for local changes] [--inline]"
---

Change set to review: $ARGUMENTS

Review the code for integrity and compliance with `@CODING_STANDARD.md`. The hard architectural constraints in
`@CLAUDE.md` take precedence where they overlap.

# 1. Determine the change set and scope

**Base branch (read this first).** This project does NOT use `master` as trunk.
`master` is the current production release (e.g. V1.0); the **integration branch** (see "Branching model" in
`CLAUDE.md`, currently
`V1.1`) is what feature branches merge into. Diff feature work against the integration branch, not
`master`. Diffing against
`master` would surface the entire unreleased delta rather than the change under review. The only time the base is
`master` is a release-promotion review (integration branch -> master).

Pick the mode from the argument:

- **No argument** (default): review local work.
    - If on a feature branch: the change set is the uncommitted working tree plus
      `git diff <integration-branch>...HEAD`.
    - If on the integration branch itself: review the uncommitted working tree (and any local commits not yet pushed). Do not diff the whole branch against
      `master` here, that is a release review.
    - Use `git status` and `git diff` for the working tree.
- **A branch or ref**: the change set is `git diff <base>...<ref>`, where
  `<base>` is the integration branch for feature work, or `master` if the ref is the integration branch being promoted.
- **A PR number or URL**: review the GitHub PR.
    - Confirm the PR reference resolves. Read its base branch with
      `gh pr view <pr> --json baseRefName` and use THAT as the base (do not assume `master`).
    - Check CI status with `gh pr checks`.
    - Check whether the base branch is merged into the PR branch; if not, update it via
      `gh api repos/{owner}/{repo}/pulls/{pull_number}/update-branch -X PUT` (this preserves approvals, unlike a CLI merge). If the update fails (conflicts, permissions), STOP and report.
    - **If CI is failing, the PR does not resolve, or the branch cannot be updated: STOP, report the issue, and do not
      proceed.**
    - Use `gh pr diff` for the diff. Check out the PR branch so reported line numbers match the files.

Scope to the project modules (`app/`,
`updater/`). If the change set includes spec/docs files, review them for internal contradiction, ambiguity, and divergence from the actual implementation. Verify new files sit in the correct package and that tests mirror the source package.

# 2. Overview

- If reviewing commits/PR, list the authors of the changes in the set.
- Give a brief big-picture summary of what changed and the common theme(s).

# 3. Analysis criteria

Enforce every applicable rule in `CODING_STANDARD.md`. Before judging, sample neighbouring code
(same package, sibling packages) and infer the de facto conventions of this codebase (how events are defined and registered, how subscribers persist via managers/DAOs, how the EventBus is used, how config/secrets are accessed, how logging is done, how tests are structured). Flag any change that contradicts these patterns, and say explicitly when the rule is implicit, citing 1-2 example files where the existing pattern is visible.

Additionally review for:

- Logic, coherence, and flow.
- Style, clarity, conciseness, readability.
- Duplication and long-term maintainability; obsolete/dead code left behind.
- Names that are unclear or ambiguous without surrounding context.
- Security (secret handling, the journal-only data constraint).
- Testing: missing tests for new logic; disabled/removed still-relevant tests; an assertion deleted rather than moved when a responsibility moved; tests for impossible states; test structure not mirroring source.
- Event/DB/API integrity: breaking changes to `BaseEvent` shapes or
  `EventRegistry`, edited applied migrations, broken handler contracts.
- Performance (blocking the EDT, per-journal-line work, premature optimization).
- Error handling: catch-all blocks, swallowed failures, missing logs, fallbacks that hide real bugs vs intentional documented graceful degradation.
- Abstraction quality: interfaces/signatures/domain models hiding implementation detail, no unused params/fields/state.
- Consistency: the same concept handled identically across all code paths that touch it.
- Robustness: fail-fast on unexpected states; only intentional, `// WHY:`-documented degradation.
- New libraries: state any added dependency.
- Linting: list any `@SuppressWarnings` or settings changes used to bypass rather than fix.

# 4. Reporting

Use a warm, constructive tone that fosters trust and psychological safety, focused on shared goals
(reliability, maintainability) rather than personal preference. Tone must not reduce thoroughness; findings must be precise and complete.

- Do not use em dashes ("-" or rephrasing instead).
- Number findings starting from 1. For each: a brief plain-English explanation of the problem, the implication, and the simplest fix, prefixed with
  `file:line`.
- Tag each finding with one or more labels: `logic`, `design`, `clarity`, `readability`,
  `duplication`, `convention` (implicit/undocumented), `robustness`, `error-handling`, `testing`,
  `security`, `performance`, `api`. Extend as needed.
- Attribution: report issues introduced by this change set first; list pre-existing related issues separately afterward.

Output: by default, write the full report to
`code-integrity-review.md` in the repo root and print a short summary inline. If
`--inline` is in the arguments, print the full findings inline and do NOT write a file (use this for quick iterations on small local change sets).

---
description: Draft a human-readable commit message describing what changed, written to commit-message.txt
argument-hint: "[ticket key e.g. KAN-XYZ] [--staged]"
---

Optional input: $ARGUMENTS

Generate a commit-message draft for the current uncommitted work. Do NOT stage or commit anything; just write the draft to a file and print it. The user commits manually.

# 1. Gather the change set

The change set is what differs from the last commit on the current branch:

- Default: all uncommitted changes vs `HEAD` - staged and unstaged tracked changes plus new
  (untracked) files. Use `git status --short`, `git diff HEAD`, and read untracked files' contents.
- If `--staged` is in the arguments: scope to staged changes only (`git diff --cached`).

Read the actual diffs and new-file contents so you describe behaviour, not filenames. If the diff is large, read enough of each hunk to understand the intent; do not guess.

# 2. Understand what changed (functionally)

Work out, from the diffs, what actually changed in terms of:

- Functionality / behaviour (new features, changed behaviour, bug fixes, removed behaviour).
- Notable implementation decisions (new abstractions, data flow, concurrency, error handling).
- Tests added or changed.
- Anything user-visible (UI, voice/announcements, config).

Group related changes into themes. A theme spans whatever files implement it; do not organise by file.

# 3. Write the message

This message is a record of work already done, not a plan. Every bullet describes something that now exists in the diff, never something that should happen next. If a bullet could be mistaken for a to-do item or a code review comment, rewrite it.

Format:

- **Summary line**: past tense, concise (aim <= 72 chars). If a ticket key was passed in the arguments, prefix it:
  `KAN-61 <summary>`.
- Blank line.
- **Body**: bullet points grouped by theme, past tense throughout (e.g. "Added", "Fixed",
  "Reworked", not "Add", "Fix", "Rework"). Each bullet states WHAT changed and, where it aids understanding, WHY or the key implementation choice. Describe functionality and implementation, not a list of files. It is fine to name a component/area (e.g. "credit tracking", "journal parser") when it helps the reader, but do not enumerate changed files.
- If tests were added or updated, include a short `Tests:` line or bullet, past tense.
- Use plain hyphens, never em dashes ("-").
- Do not hard-wrap lines with \n
- Keep it factual and reviewable; no filler.

Example shape (illustrative, not a template to copy verbatim):


# 4. Output

Write the message to `commit-message.txt` in the repo root and print it inline. Do not run `git add` or
`git commit`. Mention that the draft is ready for the user to review and commit.

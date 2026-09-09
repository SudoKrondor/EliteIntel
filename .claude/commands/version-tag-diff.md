---
description: Draft a human-readable commit message describing what changed, written to version-diff.txt This file is for me to work with. I will copy and paste the parts where they need to go. It will be used to let users know what changes where introduced.

---

Compare current version to previous tag.

Do NOT stage or commit anything; just write the draft to a file and print it. The user commits manually.

# 1. Gather the change set

Version is in app/src/main/resources/version.txt The tag is same format as version. For example is version.txt reads v-1.1.0015 compare that tag to the current branch.

Run the diff, provide information in plain English targeted at non-technical users describing the changes in the new release.

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

- **Summary line**: past tense, concise (aim <= 72 chars).
-
    - Blank line.
- **Body**: bullet points grouped by theme, past tense throughout (e.g. "Added", "Fixed",
  "Reworked", not "Add", "Fix", "Rework"). Each bullet states WHAT changed and, where it aids understanding, WHY or the key implementation choice. Describe functionality and implementation, not a list of files. It is fine to name a component/area (e.g. "credit tracking", "journal parser") when it helps the reader, but do not enumerate changed files.
- Use plain hyphens, never em dashes ("-").
- Do not hard-wrap lines with \n
- Keep it factual and end user friendly.

Example shape (illustrative, not a template to copy verbatim):

```
---

- Item 
- Item
...
- Item

---
```

# 5. Output

Write the message to `version-diff.txt` in the repo root and print it inline. Do not run `git add` or `git commit`. Mention that the draft is ready for the user to review and commit. Always override `version-diff.txt`

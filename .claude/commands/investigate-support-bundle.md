---
description: Run Support Bundle Analysis


argument-hint: "/home/alex/Downloads/*.zip"
---

# 1. Gather the change set

Look for a provided .zip file in /home/alex/Downloads/ directory It contains Support Bundle.

Analyze this bundle against the current code line (the branch we are on right now)

# 2. Understand what went wrong

It could be a bug in our code, it could be bad binding configuration, it could be audio problems (mic).

# 3. Write the message

The message contains three parts.

- What Went Wrong
- What User Can Do
- What We Need to Fix

Format:

- **Summary line**: past tense, concise (aim <= 72 chars).


- Blank line.

-
**Body**: bullet points grouped by theme, past tense throughout. Describe what went wrong, either this is a bug in our code, a configuration problem on the user side, bad audio input - wrong STT or any combination of above.


- Use plain hyphens, never em dashes ("-").
- Do not hard-wrap lines with \n
- Keep it factual
- Use a non-technical user-friendly format for end-users in the "What User Can Do" section

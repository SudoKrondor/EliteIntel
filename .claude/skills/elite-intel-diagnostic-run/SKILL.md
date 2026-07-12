---
name: elite-intel-diagnostic-run
description: >-
  Drive the running EliteIntel app through its file-based diagnostics harness to verify
  command/query routing for one language. For each command/query it composes FRESH, natural
  utterances in the target language — using authentic in-game Elite Dangerous terminology from
  EXTERNAL references (official/wiki), never the app's own localization files (those are what is
  under test) — feeds them at conversation speed, reads the diagnostics log, scores routed vs.
  expected, and recommends concrete fixes (llmDescription / ai_action_aliases) for each miss. So
  the run measures generalization, not memorization. Use when asked to run the routing diagnostic,
  e.g. "/elite-intel-diagnostic-run EN" or "прогони диагностику RU". Requires the app built from
  this repo (diagnostics mode is a dev harness). Runs the REAL companion LLM (billable, live
  network); game commands and queries are recorded, never executed (no keystrokes, no EDSM/Spansh
  calls).
---

# elite-intel-diagnostic-run

Automated tester for companion routing. You play the role a human tester would: speak phrases to
the app (by appending them to a file), wait like a person would for the assistant to answer, and
check the app understood each phrase — all read back from a log file, because an LLM-driven app
can't be unit-tested deterministically.

Crucially, you do **not** replay the test's phrases verbatim. The reference test's aliases are only
a *reference for the intent* of each command; you compose your **own** natural utterances a real
commander might say for that same intent, so the run tests whether routing generalizes beyond the
exact training/alias phrases. **Ground truth is the intent→action mapping** taken from the test
group; the words are yours.

## Arguments and preconditions

- **Language (required, no default)** — one of `EN`, `RU`, `UK`, `DE`, `ES`, `FR`, `IT`, `PT`. It
  selects the reference test `app/src/test/java/elite/intel/junit/prompt/NaturalSpeechIntegrationTest<LANG>.java`
  and the cached glossary `glossary/<LANG>.md`.
- **Count (optional, default `3`)** — generated utterances per command, e.g.
  `/elite-intel-diagnostic-run RU 4`.
- **`--refresh-glossary` (optional)** — rebuild the cached terminology from the web.
- **`--only <id1,id2,...>` (optional)** — restrict the run to those expected action ids (targeted
  mode); everything else is skipped. Turns drop from ~270 to `len(ids) × count`.
- **`--replay-fails [path]` (optional)** — regression mode: instead of generating fresh utterances,
  re-feed the utterances that FAILED in a prior run's saved plan (`path` defaults to the most recent
  `runs/<LANG>-*.json`). No generation, no web. This is the one case where verbatim re-feeding is
  correct — see *Run modes*.

### Run modes

- **Full generalization (default).** All ~90 groups × `count` with FRESH utterances. This is the
  point of the skill: it measures whether routing generalizes, not memorization. Use it for a real
  audit of a language.
- **Targeted (`--only`).** Same fresh-generation logic, restricted to specific actions — for
  iterating on a few intents you just changed without paying for the whole run.
- **Regression (`--replay-fails`).** Re-feed the *exact* utterances a previous run flagged, to
  confirm a fix landed. Verbatim replay is deliberate here because the job is *verifying a specific
  regression*, not exploring new phrasings — so it does not contradict the "fresh, not memorized"
  rule that governs the other two modes. After a green replay, run a fresh targeted pass on the same
  ids to confirm the fix generalizes, not just that it satisfies those specific strings.

**Validate the language first.** If it is missing or not one of the eight codes, do nothing else —
no files, no app launch, no web — print an error and stop, e.g.
`elite-intel-diagnostic-run: language argument is required — one of EN, RU, UK, DE, ES, FR, IT, PT (e.g. /elite-intel-diagnostic-run EN)`.

**Run exactly ONE language per invocation.** Turns ≈ (command groups, ~90) × count; at the default
that is ~270 live companion LLM turns — the whole cost of the run. Never loop over several
languages; if the user wants more, run them as separate, explicitly requested invocations. Raising
`count` scales turns linearly — keep it modest.

**Run without pre-flight questions.** Given a valid language, do the whole procedure — build the
glossary if missing, launch the app, feed, report. The provider/model is the user's in-app choice
and irrelevant here (the skill only writes phrases and reads the log): do not detect it, factor it
in, or ask.

Environment: Windows PowerShell 5.1, Windows paths with drive letters, no bash-isms, no `&&`/`||`
chaining.

## How the harness works

The app runs in diagnostics mode when the input file `input.txt` exists at startup — its presence
is the gate. The app only ever READS it and never creates it, so you own its lifecycle: create it
before launch, delete it after the run, so the mode does not stick to later launches.

Files (all under `%LOCALAPPDATA%\elite-intel\diagnostics\`):

- `input.txt` — the gate; its existence enables the mode, and you append phrases to it during the
  run.
- `language.txt` — a language code read at startup for the boot language (a data file, NOT the
  gate).
- `session.log` — the app mirrors everything here.

The app truncates the log at its own startup, but that happens a while *after* you launch it, so
you must ALSO clear the log yourself right before launching (step 5) — otherwise you read the
previous run's log and its stale `DIAG ready`.

In diagnostics mode STT is off (input comes from the file, so the mic model isn't loaded — faster
startup), but TTS is REAL: the companion voice stays audible. App turns are fast (~1 s each: the
LLM answers, then a short spoken reply). If a run feels slow it is almost never the app — it is
utterances generated lazily between feeds (step 3: generate everything UP FRONT).

### Input line kinds

- A plain-text line = a spoken phrase → routed like microphone input.
- `@visible <actionId>` = **the preferred way to set "where we are".** Puts the game in the first
  context in which that action is visible to the router — using the app's real `isVisibleForLLM`,
  exactly as the routing test's `applyStateFor` does. Emit it before each utterance with that
  utterance's expected id; you never guess a context. Applies immediately, no turn. (Also tries a
  deployed fighter for fighter orders.)
- `@status <context>` = manual context override (`main_ship`, `supercruise`, `docked`, `landed`,
  `srv`, `on_foot`) — only for special cases; prefer `@visible`. Applies immediately, no turn.
- `@fighter on` / `@fighter off` = toggle the fighter-deployed gate manually (usually unnecessary —
  `@visible` covers it).
- `@lang <CODE>` = switch the app's command language at runtime. **Do not use this to set the run
  language** — it is too late for the semantic reducer, which freezes its language at construction.
  Set the run language via `language.txt` (step 5) instead. (Kept only for ad-hoc TTS-language
  tweaks.)
- A JSON line with an `"event"` field = inject a journal game event (advanced; rarely needed for
  routing).
- A line starting with `#` = comment, ignored.

### Log markers you parse

Each line is `<UTC-timestamp> <marker>`:

- `DIAG ready` — **all services up and the LLM endpoint confirmed reachable**; the ONLY readiness
  signal — do not feed before it. (`DIAG companion-started` appears earlier, when the companion is
  merely wired — NOT readiness; `DIAG llm-not-connected` means the LLM probe failed and the app is
  retrying.)
- `DIAG log opened` — a fresh app instance re-opened the (cleared) log; expect it before this run's
  `DIAG ready`.
- `DIAG input="<phrase>"` — opens a turn for that phrase.
- `DIAG dispatch tool=<actionId>` — an action the companion dispatched this turn (the routing
  result you score).
- `DIAG speaking=true|false` — TTS boundaries.
- `DIAG boot-language=<CODE>` — the app set the command language from `language.txt` at startup
  (before the companion/reducer were built). This is how the run language is applied. A
  `DIAG boot-language error` line means the code didn't parse (check for a stray BOM/newline).
- `DIAG lang=<CODE>` — a runtime `@lang` switch was applied (does not re-language the reducer).
- `DIAG visible=<actionId> state=<ctx>` — `@visible` set the context so `<actionId>` is visible.
  `state=unknown-action` = the id did not resolve (fix it); `main_ship(fallback)` = it was never
  visible in any probe state.
- `DIAG status=<ctx>` / `DIAG fighter=<bool>` — a manual directive was applied.
- `LOG` / `DBG` / `AI` / `USER` — mirrored SYSTEM LOG, useful for diagnosing a miss.

### Pass criterion

A phrase **passes** if its expected action id appears in a `DIAG dispatch tool=` line in that
phrase's turn (the lines between its `DIAG input=` and the next `DIAG input=` / `DIAG status=` /
`DIAG event=`).

## Procedure

**You are the driver — do NOT build one.** Do not write a PowerShell/Python script, harness, or any
standalone program to orchestrate the run (a script cannot write natural-language utterances). You
generate each utterance, then feed and observe it with plain one-off tool calls — `Add-Content` to
append a line, `Get-Content -Tail` to read new log lines, decide PASS/FAIL, repeat. The only
process you ever launch is the app itself (`.\gradlew :app:run`). If you catch yourself authoring a
loop-runner, stop and do the next step by hand.

0. **Validate the language argument.** Per *Arguments and preconditions* — on a bad/missing code,
   print the error and stop before touching any files, the app, or the web.

1. **Resolve the expected action ids.** The test references actions by Java constant (e.g.
   `DeployLandingGearCommand.ID`), but the log reports the runtime id string. Build the map once:
   grep `public static final String ID =` under `app/src/main/java/elite/intel/ai/brain/actions/`
   to get `ClassName → "id_string"`.

2. **Parse the reference test** `NaturalSpeechIntegrationTest<LANG>.java`. For each
   `@ParameterizedTest` method, read its `assertRouted(input, <Class>.ID)` for the expected class
   and its companion `static Stream<String> <method>()` for the reference aliases. Produce an
   ordered list of groups: `{ expectedId (from step 1), intentName, referenceAliases[] }`. Skip
   `startListening`/`ignoreMe` unless asked — they are attention control, not routing.
   - **`--only <ids>`:** keep only groups whose `expectedId` is in the list; if an id doesn't match
     any group, name it and stop (likely a typo).
   - **`--replay-fails`:** skip generation entirely. Load the saved plan (`runs/<LANG>-*.json`, most
     recent unless a path was given), take the records with `pass=false`, and use those exact
     utterances as the plan — each already carries its `expectedId`. Jump to step 4.

3. **Generate ALL utterances UP FRONT — before feeding anything.** This is the point of the skill
   (see *Terminology sourcing* and *Phrase generation*), and one-pass generation is what keeps the
   run fast. For every group, infer the intent and required parameters from the reference aliases
   and the in-game vocabulary from the external references (NOT the app's own files), then write
   `count` fresh, natural utterances in the target language. Produce the WHOLE plan (all groups ×
   `count`) at once and hold it in memory, THEN start feeding. **Do not generate lazily between
   turns** — pausing 30–120 s to think up the next line is the single biggest reason a run drags
   (app turns are ~1 s). These generated utterances — not the aliases — are what you feed; each
   utterance's expected action is its group's `expectedId`. (In `--replay-fails` mode there is no
   generation — the plan came from the saved file in step 2; skip straight to step 4.)

4. **Set context with `@visible`, not by guessing.** Before each group's utterances, emit
   `@visible <expectedId>`; the app puts the game in the first context where that action is visible
   (via its real `isVisibleForLLM`, exactly as the routing test does), so you never guess which
   `@status` a command needs. Confirm with `DIAG visible=<id> state=<ctx>`. `state=unknown-action`
   means the id is wrong (fix it); only fall back to a manual `@status`/`@fighter` if you
   deliberately want a non-default context.

5. **Arm diagnostics mode and start the app — create the gate FIRST, then launch.** Do this without
   asking.
   - `New-Item -ItemType Directory -Force "$env:LOCALAPPDATA\elite-intel\diagnostics"`.
   - **Write files as UTF-8 WITHOUT a BOM.** `Set-Content -Encoding utf8` in PowerShell 5.1
     prepends a BOM, which corrupts the first line (a leading `@directive` or the language code).
     Use `[System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))`
     for the initial writes. (The app also strips a leading BOM defensively, but write clean.)
   - **Create `input.txt` empty** (WriteAllText with empty string, no-BOM) — this is the gate; its
     presence enables diagnostics mode. Do this BEFORE launching.
   - **Write the boot language into `language.txt`** (the code only, e.g. `RU`, no-BOM) — a
     separate data file, NOT `@lang`. The app reads it at startup and sets the command language
     before the companion is built. Required, because the semantic reducer **freezes its language
     when constructed**, so a later `@lang` never reaches it and it would miss obvious commands.
     Confirm with `DIAG boot-language=<LANG>`.
   - **Clear `session.log`** so you never mistake a previous run for this one:
     `Set-Content -Path "$env:LOCALAPPDATA\elite-intel\diagnostics\session.log" -Value $null`.
   - **Launch the app in the background:** `.\gradlew :app:run`. The window can take ~1 min to
     appear; services autostart.
   - Readiness is decided **only** by a FRESH `DIAG ready` from THIS run. Because you cleared the
     log, it starts empty: wait for the new instance to re-open it (`DIAG log opened`), then a
     `DIAG ready` after it. Never treat a pre-existing `DIAG ready` as readiness, and do not narrate
     "still starting" from anything but the log. While waiting, do NOT pre-stage phrases — just
     wait. If `DIAG ready` never appears, report that the app didn't reach a ready state (check API
     credentials / that it's really this build).

6. **Feed ONE line at a time — never in bulk.** The language is already set (boot file, step 5), so
   no `@lang` is needed. Hold the whole plan in memory and emit it line by line — never pour it in
   with one write.
   - Per group, first append `@visible <expectedId>` (wait for `DIAG visible=...` — near-instant),
     then its utterances one per turn (step 7). Re-emit `@visible <expectedId>` for each group even
     if the previous differed — it is cheap and keeps context correct.
   - Write each line with `Add-Content -Encoding utf8` so non-ASCII is preserved. The bookkeeping
     comment `# intent -> id (ref: ...)` is for your own tracking/report — you need not write it to
     the file.

7. **Per-utterance turn loop — feed one, wait for the whole turn, then the next.** For each
   utterance:
   1. Append exactly that one line to `input.txt`.
   2. Read `session.log` **INCREMENTALLY** (track a byte/line offset; never re-slurp the whole file
      — that is the main way this skill wastes *your* tokens) until this turn is done: its
      `DIAG input="<utterance>"` appears, then its `DIAG dispatch tool=<id>` line(s); and if
      `DIAG speaking=true` appeared, wait for the matching `DIAG speaking=false`. Bound with a
      per-turn timeout (~90 s).
   3. Record PASS/FAIL (PASS iff `expectedId` is among that turn's `DIAG dispatch tool=` lines),
      then append the next line **promptly** — add no extra delay; the app already inserts a short
      conversational gap, so padding it makes the run drag.

   This mirrors real speech: one command, wait for the assistant to answer and stop talking, then
   the next. The app paces internally as a backstop, but you must still drive strictly
   one-at-a-time. Context is already correct via `@visible`, so a zero-dispatch miss is a real
   recall/visibility finding — do NOT cycle `@status` to "rescue" it. If `DIAG visible=` showed
   `main_ship(fallback)` or `unknown-action`, note that in the report; otherwise treat the miss as a
   genuine result for step 9.
   - **Stream progress.** A full run is 10–20 min of silence otherwise. After each group (or every
     ~10 turns), emit one terse line — `группа 34/90 · пройдено 78 · fail 6` — so the user sees it
     is alive and where the failures are clustering. Keep it to a single line per update, not a
     running transcript.

8. **Report** (in Russian, per project convention) a compact table:
   utterance | expected id | dispatched | PASS/FAIL, grouped by intent, plus totals and the failing
   utterances with what they routed to instead. Because the utterances are yours, before recording
   any FAIL sanity-check it against the *Phrase generation* rules: if it is genuinely ambiguous or
   drifted off-intent, that is a skill error — fix the utterance and re-run it, don't report it as
   an app failure. For a real FAIL where the reducer never offered the target, note it (the mirrored
   `DBG`/`LOG` lines around that turn show the reducer selection) so the user can tell a
   reducer/alias problem from an LLM-choice problem.
   - **Classify each intent, not just each utterance (LLM routing is nondeterministic).** With
     `count ≥ 2`, bucket the intent by how many of its utterances passed: **PASS** (all), **FLAKY**
     (some but not all), **FAIL** (none). A FLAKY intent is instability, not a clean bug — a single
     0/1 or 2/3 result would over- or under-state it. Surface the three buckets with their ratios
     (e.g. `deploy_landing_gear 2/3 FLAKY`) so the signal is honest.
   - **Persist the run** to `runs/<LANG>-<UTC-timestamp>.json` next to this SKILL.md (same writable
     location as `glossary/`): `{ language, count, mode, timestamp, records: [{ intent, expectedId,
     utterance, dispatched, pass }] }`. This is what `--replay-fails` reads, and it lets you diff two
     runs to see whether a fix actually moved the needle.

9. **Improvement recommendations.** Base concrete recommendations on **systematic FAIL** intents
   (0 passes) — those are real, actionable routing defects. List **FLAKY** intents separately as
   "нестабильно, не чистый баг": don't prescribe an `llmDescription`/alias edit off a single flaky
   turn (you'd be tuning to noise); if a FLAKY intent matters, suggest re-running it with a higher
   `count` via `--only <id>` to see whether it's really borderline. For each systematic FAIL, target
   the two owners of routing:
   - **Function description** — the action's `llmDescription()` (an intent directive, e.g.
     `DeployLandingGearCommand.llmDescription()`), which is what the companion LLM sees.
   - **Training phrases** — the action's aliases in `ai_action_aliases_<lang>.properties` (bundle
     `i18n.ai_action_aliases`, keyed by the same action id), which feed the reducer.

   Pick the recommendation from the failure mode (evidence: the reducer-selection line in the log):
   - **Recall miss** — the reducer never offered `expectedId`: the alias set under-covers this
     phrasing. Recommend concrete alias additions to the `<expectedId>` key in
     `ai_action_aliases_<lang>.properties` — full utterances carrying every required parameter, in
     real ED terminology, not duplicating an existing alias (respect the project's alias/probe
     hygiene rule).
   - **Selection miss** — the reducer offered `expectedId` but the LLM dispatched a different id
     `X`: the two descriptions don't separate cleanly. Recommend a concrete edit to
     `<expectedId>.llmDescription()` (and/or `X`'s) that disambiguates the confusable pair — name
     the pair and the distinguishing cue, keep it a terse intent directive (no prose bloat).
   - **No dispatch even after context retries** — likely a visibility gate (`isVisibleForLLM` /
     wrong `@status`) or a deep recall gap; say which, and what to check.

   Group recommendations by action, dedupe, and ground every one in the logged evidence — never
   guess. Do not recommend editing the tests or the glossary; the fix belongs in the app's
   description or alias owner.

10. **Tear down — delete the gate.** When the run is done (or if you abort), always delete
    `input.txt`: `Remove-Item -Force "$env:LOCALAPPDATA\elite-intel\diagnostics\input.txt"`. Its
    existence is the mode gate and the app never removes it, so leaving it would boot the next
    normal launch into diagnostics mode. Do this even on failure. (Do NOT delete it mid-run — its
    absence is only checked at startup, but keep it for the session.)

## Terminology sourcing (authority is EXTERNAL, not the app)

Utterances must use the actual in-game Elite Dangerous terminology for the target language (e.g. RU
«суперкруиз», «шасси», «дипольные отражатели», «авианосец»; the localized commodity/ship/module/
system nouns), not a literal translation.

**Do not source terminology from the app's own files.** `ai_action_aliases_<lang>.properties`,
`ed_events`, the normalizer rules and the test phrases are exactly what is under test — drawing
vocabulary from them makes the run circular and hides the bug we are hunting: the app understanding
only its own idiosyncratic wording and not the terms a real player uses. The terminology ground
truth must be independent of the app.

0. **Reuse the cached glossary, or build it once.** Look for `glossary/<LANG>.md` next to this
   SKILL.md.
   - If it exists (and no rebuild was requested), load it and **do not touch the internet** —
     generate from it.
   - If it is missing (first time for this language) or a rebuild was requested (`--refresh-glossary`,
     or the user asks), build it from the sources in step 1, **write it to `glossary/<LANG>.md`**,
     then generate from it. The web cost is paid once per language and reused for every later run.
     Building it is part of the skill's job — do not ask permission.

   The glossary is a **vocabulary reference, not a canned phrase list** — never replay its example
   lines verbatim (that reintroduces memorization); vary as always. See `glossary/README.md` for
   the format.

1. **Authoritative sources (used only when building the glossary):** official / community Elite
   Dangerous references in the target language — the game's own localized UI terms, the Elite
   Dangerous wiki (Fandom and its localized editions), Frontier materials, INARA. From these get the
   real localized terms for ship systems (landing gear, hardpoints, heat sink, chaff, FSD /
   supercruise), modules, commodities (e.g. alexandrite/bromellite), star classes, station services,
   and fleet-carrier / SRV / fighter concepts. Verify each term (cross-check two sources when you
   can); **never invent** one, and record the source URLs in the glossary. Mark anything you can't
   confirm as uncertain.

2. **The app's local files are used ONLY for intent, not vocabulary.** Read
   `app/src/main/resources/i18n/ai_action_aliases_<lang>.properties` (keyed by the same action id
   the log prints — e.g. `enter_super_cruise`) and the reference test purely to learn *which* action
   a group targets and its **required parameters** (placeholders like `{lat:X, lon:Y}`, `{key:X}`).
   Do not lift the phrasing. If your externally-sourced term differs from the app's alias term, that
   divergence is *interesting* — a real-world phrasing the app may fail to route — not something to
   "correct" toward the app's wording.

## Phrase generation (hard rules)

You author the utterances; a badly written one causes a false failure, so treat these as strict:

- **Keep the intent.** The utterance must mean the same command as the reference aliases — not a
  neighbouring one. Don't drift "deploy landing gear" into "prepare to land" if that's a different
  action.
- **Keep the polarity.** Never flip on/off, deploy/retract, increase/decrease, start/cancel.
  "Retract gear" is a different intent from "deploy gear".
- **Keep every required parameter, with a concrete value.** If the intent carries a number, target
  or unit, your utterance must too: "increase speed **by 10**", "set fuel reserve **to 5000**",
  "find **alexandrite** within **300 ly**". An utterance that drops the parameter is invalid — the
  reducer treats it as a different, under-specified request.
- **One complete utterance per line** — a full spoken command as a commander would say it, not a
  keyword and not two commands joined. No trailing-punctuation tricks.
- **Use authentic external ED terminology** drawn from *Terminology sourcing*, never lifted from the
  app's alias/localization files (reproducing the app's own wording tests memorization and hides
  real-world misroutes).
- **Natural and idiomatic — this is critical.** Each utterance must sound like a native speaker
  casually talking to their ship's AI, not a translated manual or a keyword string. Use natural word
  order, everyday imperative phrasing, and contractions the way a real commander speaks aloud. Weave
  ED terminology in naturally — don't stuff terms in or calque English structure. Read each line back
  in your head; if it sounds stiff, textbook, or translated, rewrite it. Vary verb, word order and
  register across the `count` utterances; never just reorder the alias words. (RU specifically:
  prefer how a Russian-speaking pilot would actually say it, not a wooden literal rendering.)
- **Unambiguous.** If a phrasing could reasonably be two different commands, pick a clearer one.
  Ambiguity you introduced is not an app bug.
- **Do not paraphrase attention-control** (`startListening`/`ignoreMe`) unless asked; those are
  matched narrowly by design.

## Cost discipline

- **One language per run** (see *Arguments and preconditions*): ~90 groups × `count` (default 3) ≈
  ~270 live LLM turns is the budget. Don't chain languages.
- **Iterate with the cheap modes.** A full run is for auditing a language. When fixing a handful of
  intents, use `--only` (a few × `count` turns) and `--replay-fails` (just the prior failures) — the
  full ~270-turn run is not the tool for a tight edit→verify loop.
- **Incremental log reads only.** Track an offset and read the appended tail; never re-slurp the
  full `session.log` in a poll loop.
- **Web only to build a missing glossary.** If `glossary/<LANG>.md` exists, generate from it and
  stay offline; build from the web at most once per language, then reuse.

# EliteIntel Localization Guide

This guide is written for people who speak the target language fluently but are not programmers. You will be editing text files - no programming knowledge is needed, but you do need to follow the formatting rules exactly.

---

## What you are localizing

EliteIntel is a voice assistant for the space game *Elite
Dangerous*. The player speaks a command out loud (e.g. "jump to hyperspace"), the app recognizes the speech, and sends the correct action to the game.

Your job is to make this work in your language. You need to teach the app:

- **What phrases** a player in your language might say
- **How to understand** those phrases correctly
- **How to ask the AI** to interpret them

---

## Files you will edit - one per language

Each language has its own set of files.  
They all follow the same naming pattern (using `de` as an example for German):

| File | What it is |
|---|---|
| `i18n/gui_de.properties` | Labels visible on screen (buttons, checkboxes, etc.) |
| `i18n/llm_de.properties` | Phrases the AI assistant speaks back to the player |
| `i18n/ed_events_de.properties` | Short descriptions of in-game events |
| `i18n/ai_action_aliases_de.properties` | **Special file** - voice trigger phrases (see below) |
| `…/de/GermanPromptRules.java` | Hints for the AI about how your language works |
| `…/de/GermanInputNormalizerRules.java` | Synonym and paraphrase normalization |
| `…/de/GermanAiActionAliases.java` | Wake-up word and "listen" prefix in your language |

Language codes: `de` German · `fr` French · `es` Spanish · `it` Italian · `pt` Portuguese · `ru` Russian ·
`uk` Ukrainian

---

## 1 - The three standard properties files

**Files:** `gui_XX.properties`, `llm_XX.properties`, `ed_events_XX.properties`

These are plain text files where each line looks like:

```
key=The text that is shown or spoken
```

**Rules:**

- Translate only the text **after** the `=` sign. Never change the part before it.
- Keep any `{placeholder}` exactly as-is - these are filled in with real values at runtime.
- Lines starting with `#` are comments. You can read them for context, but do not translate them.
- If a line does not exist in the language file, the English version is used automatically.

**Example (German):**

```
# English base
fuel.status=Fuel status: {level}%

# German translation
fuel.status=Treibstoffstand: {level}%
```

---

## 2 - The action aliases file (special)

**File:** `ai_action_aliases_XX.properties`

This file is different from the other three. It teaches the AI **which phrases should trigger which game action**.

Each line looks like this:

```
action_key=phrase one, phrase two, phrase three
```

- The **left side** (before `=`) is the internal action name used by the game. **Never change this.**
- The **right side** (after `=`) is a comma-separated list of phrases the player might say to trigger that action.

Your job is to write **natural phrases in your language** that a player would actually say. Think about:

- Colloquial expressions, not just literal translations
- Multiple ways of saying the same thing
- Short commands as well as longer phrases

**Example (German, jump to hyperspace):**

```
jump_to_hyperspace=sprung in den hyperraum, spring, hypersprung, los gehts, nächster wegpunkt
```

**Important:**

- Do not worry about covering every possible phrasing - the AI will still understand close variations.
- Separate phrases with commas and a space.
- Phrases containing `{key:X}`, `{lat:X, lon:Y}` etc. are parametric templates - keep the
  `{...}` part exactly as shown and translate only the words around it.
- If you are not sure whether a phrase belongs here or in the normalizer rules (see section 4), put it here - the AI handles the variation.

---

## 3 - PromptRules (Java file)

**File:** `…/XX/GermanPromptRules.java` (or French, Spanish, etc.)

This file gives the AI four short hints about how your language works. You only need to edit the text inside the quotation marks on each of these four lines.

### `languageName()`

The full English name of the language.  
Example: `"German"`, `"French"`, `"Russian"`  
Do not translate this - it stays in English.

### `queryStarterExamples()`

A comma-separated list of words that typically start a **question** in your language.  
These are words a player uses when asking for information (not giving a command).

English example: `"what, where, how, which, why, is, are, does, tell me, how much, how many"`  
German example: `"was, wo, wie, welche, warum, ist, sind, sag mir, wie viel, wie viele"`

### `commandVerbExamples()`

A slash-separated list of **action verbs** - words that start a command.  
These are words a player uses when telling the app to *do* something.

English example: `"show / open / find / navigate / deploy / retract / enable / disable"`  
German example: `"zeig / öffne / finde / navigiere / entsende / einfahren / aktiviere / deaktiviere"`

### `queryPhraseExamples()`

A slash-separated list of short **query starters** - phrases the player uses to ask a question.

English example: `"where / tell me / how much / how many / any / what is / what are"`  
German example: `"wo / sag mir / wie viel / wie viele / irgendwelche / was ist / was sind"`

### `disambiguationHints()` (optional)

This section is for edge cases where the AI repeatedly misclassifies a particular phrase. Only add entries here if you find real problems during testing.

Each hint should follow this pattern (one per line):

```
- "phrase in your language" → action_name
```

If you have nothing to add, you can leave the section returning `null` or remove it entirely.

---

## 4 - InputNormalizerRules (Java file)

**File:** `…/XX/GermanInputNormalizerRules.java`

This file is a list of **synonym substitutions
** - it maps alternative phrases to a single canonical form before the AI ever sees the input.

Think of it as a find-and-replace list that runs on every utterance.

Each entry looks like this:

```
m.put("what the player says", "canonical form");
```

Left side = what the player might actually say  
Right side = the standardized phrase it becomes

**When to add something here instead of the aliases file:**

- When two phrases mean *exactly* the same thing and you want to collapse them into one canonical form
- When a phrase is a well-known colloquial synonym that the AI might not handle reliably on its own

**Critical ordering rule:**  
Longer phrases must come *before* shorter ones.  
If you put `"jump"` before
`"how many jumps"`, the word "jump" inside "how many jumps" will be replaced first, corrupting the longer phrase.  
Always add more specific (longer) entries above more general (shorter) ones.

**For morphologically rich languages (Russian, Ukrainian, German):**  
Simple left-to-right replacement does not know about word endings or inflections.  
Only add standalone phrases you are certain cannot appear as part of another word.  
When in doubt, add the synonym to the aliases file instead.

---

## 5 - AiActionAliases (Java file)

**File:** `…/XX/GermanAiActionAliases.java`

This small file defines two things:

### Wake-up phrases (`wakeBypassPhrases`)

The exact words a player says to wake the assistant from sleep mode.  
These must be an exact, complete match - nothing added before or after.

Example (Russian): `"проснись"`, `"слушай"`, `"слушай меня"`, `"активируйся"`

Keep the list short - 2–4 phrases is plenty.

### Listen-prefix phrases (`listenBypassPrefixes`)

These are words the player can put at the start of a command to signal that what follows is a live instruction - not small talk.

Example (English): `"listen up"`, `"listen"`  
Example (Russian): `"слушай меня"`, `"слушай"`

If the player says `"listen, open the galaxy map"`, the word `"listen"` is stripped and
`"open the galaxy map"` is sent to the AI.

---

## 6 - Integration test

**File:** `app/src/test/java/elite/intel/junit/prompt/NaturalSpeechIntegrationTestXX.java`
(where `XX` is your language code - e.g. `FR`, `DE`, `RU`)

This test is the final quality check. It sends each voice phrase to the AI and verifies that it triggers the correct game action.
**If a test group passes, that group of phrases is production-ready.**

The English test (
`NaturalSpeechIntegrationTestEN.java`) is the reference. Language tests mirror its structure exactly - same section headings, same order numbers - but with your language's phrases instead of English ones.

### What the test file looks like

Each test group has two parts. Here is a simplified example from English:

```
// The test - do not change this part
void jumpToHyperspace(String input) throws InterruptedException {
    assertRouted(input, JumpToHyperspaceCommand.ID);   ← do not touch
}

// The phrases - this is what YOU fill in with your language
static Stream<String> jumpToHyperspace() {
    return Stream.of("jump to hyperspace", "jump", "lets go");
}
```

Your job: replace the English phrases inside `Stream.of(...)` with equivalent natural phrases in your language.

**Leave everything else exactly as-is:**

- The method names (`jumpToHyperspace`, `deployHardpoints`, etc.)
- The action constants (`JumpToHyperspaceCommand.ID`, `DeployHardpointsCommand.ID`, etc.)
- All the surrounding code structure

### Requirements before running

1. The app has been started at least once with the game running (so session data exists)
2. A local LLM is installed and running
3. The language is set to your language in the app settings

### How to run

The test class is tagged
`local-integration`. Run it through your IDE by right-clicking the class and choosing "Run", or via the command line with:

```
./gradlew app:test --tests "elite.intel.junit.prompt.NaturalSpeechIntegrationTestFR" -Plocal
```

The full suite takes roughly 2-3 minutes depending on LLM speed.

### If the LLM is slow

At the top of the test file there is a line:

```
private static final int LLM_WAIT_MS = 8000;
```

This is how long the test waits for a response before declaring failure. If your LLM takes longer, increase this number (it is in milliseconds - 8000 = 8 seconds).

### Interpreting results

- **Green (pass):** The phrase correctly triggered the expected action. Done.
- **Red (fail):** The phrase routed to the wrong action, or no action was returned in time.

When a test fails, the error message shows what action was expected and what was actually returned. Use this to decide where to fix things:

| What happened | Where to fix it |
|---|---|
| The AI returned a wrong but close action | Add a disambiguation hint in `PromptRules.disambiguationHints()` |
| The phrase contains a colloquial shorthand the AI misses | Add it to the aliases file (`ai_action_aliases_XX.properties`) |
| The phrase is a predictable variation of a canonical form | Add it to `InputNormalizerRules` as a synonym substitution |
| The LLM timed out | Increase `LLM_WAIT_MS` or check that the LLM process is running |

### Tip

Start with the groups that cover the most important commands for your language (navigation, combat, speed). Do not aim for 100% before submitting - a partial localization with tested, passing phrases is more valuable than an untested one that covers everything.

---

## What you must never change

- The part before `=` in any `.properties` file (these are internal keys)
- Action names inside `{...}` templates
- The `package` line at the top of any `.java` file
- The method names (`languageName`, `queryStarterExamples`, etc.)
- The surrounding Java syntax (`return`, `Set.of(`, `m.put(`, `;`, etc.) - only edit the quoted text inside

---

## Quick checklist before submitting

- [ ] Run Unit Test class in app/src/test/java/elite/intel/junit/prompt/
- [ ] All phrases in the aliases file are natural to a native speaker
- [ ] No key (left side) has been changed in any properties file
- [ ] Longer phrases come before shorter ones in the InputNormalizerRules file
- [ ] Wake-up phrases match what a player would actually say in your language
- [ ] `languageName()` is still in English
- [ ] No `{placeholder}` has been removed or altered

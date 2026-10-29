# Training-phrase quality — knowledge base for fixing commands

The tool and the rules for judging the quality of the companion semantic reducer's **training phrases**
(aliases), and for fixing commands that are recognized badly. Everything is deterministic and **needs no LM
Studio** — only the embedding model (`distribution/embed`).

Example phrases below are quoted verbatim in the language they belong to, because they are the data under test; an English gloss follows where the argument depends on the meaning.

---

## 1. What this measures, and why

On every turn the reducer picks a short list out of ~188 commands (the top 8 game commands plus the system functions) and that list goes to the model.
**The reducer is a candidate generator, not a classifier:** its job is to get the right command **onto the short
list**, and the LLM makes the final choice.

Hence the headline KPI is
**`offered@K`** (the command is on the list), not `hit@1` (the command is first). For antonym pairs (`увеличь`/`уменьши`, "increase"/"decrease") `hit@1` always floats around 50/50, which is normal:
both land in offered, and the LLM tells them apart.

The point of the test is to find commands whose real player phrasings **do not
lead** to the right command (do not land in offered), and to suggest what to add to the aliases.

---

## 2. Files in this package

| File | Purpose |
|---|---|
| `TrainingPhraseQualityProbe.java` | Scoring: runs the probes through the embedder, writes `build/training-phrase-quality-<lang>.csv` |
| `SemanticReducerProbe.java` | Diagnostics: `dumpsCatalog` (the catalog), `dumpsParams` (the parameter schema), `dumpsAntonymCosines` (pair cosines) |
| `../../../resources/trainingphrases/probe-phrases-ru.json` | Probes: `{ "id": [10 phrases], ... }` — test phrasings for each command. For EN, while there is no `probe-phrases-en.json`, the test uses a seed of the English alias phrases. |
| `../../../../scripts/build_training_xlsx.py` | Builds the Excel file from the CSV (sheets `data` / `Legend` / `Method`) |

The production code the test rests on (in the `companion.prompt` package): `SemanticActionReducer`,
`AliasEmbeddingText`, `GameToolCandidates`, `SemanticPhraseMatcher`.

---

## 3. How to run it

```bash
# 1) dump the parameter schema of every command (name:type:req/opt) - for auditing the probes
./gradlew :app:embeddingTest --tests "elite.intel.vega.trainingphrases.SemanticReducerProbe.dumpsParams"

# 2) run the scoring -> build/training-phrase-quality-ru.csv and build/training-phrase-quality-en.csv
./gradlew :app:embeddingTest --tests "elite.intel.vega.trainingphrases.TrainingPhraseQualityProbe"

# 3) build the Excel file (into Downloads)
python scripts/build_training_xlsx.py "C:\Users\Alex\Downloads\training-phrase-quality-ru.xlsx"
python scripts/build_training_xlsx.py en "C:\Users\Alex\Downloads\training-phrase-quality-en.xlsx"
```

The catalog (id/aliases/purpose) for authoring probes: `SemanticReducerProbe.dumpsCatalog` -> `build/catalog-ru.txt`.

---

## 4. How to read the result (the Excel columns)

- **rank** — the command's place among all ~188 on one probe. `1` = recognized; `>1` = other commands are ahead.
- **score** — `hit@1 X/10` (how many probes put it first) plus `offered Y/10` (how many landed on the short list).
- **verdict** — **OK** = offered 10/10; **WATCH** = 9/10;
  **WEAK** = 8/10 or less (on 2+ probes the command is hidden from the LLM).
- **conflict_group** — the command that most often beats this one (its semantic conflict partner).
- **own_match** — the command's own best training phrase for the probe, and its similarity.
- **competitor** — the foreign command and phrase that beat it (the gap is competitor − own_match).
- **suggested_additions** — the probes where the command lost (candidates for new aliases, after review).

---

## 5. THE MAIN RULE (alias/probe hygiene)

> **A training phrase, and a test probe, is a complete utterance: it has an action verb (not a bare entity
> fragment) AND it has a value for every required parameter.**

Bad: `двигатели` ("engines"), `к точке пиратской миссии` ("to the pirate mission point"), `торговый бюджет`
("trade budget"), `таймер напоминания` ("reminder timer"), `быстрее` ("faster"). Good: `наведись на двигатели` ("target the engines"), `проложи маршрут к точке пиратской миссии` ("plot a route to the pirate mission point"), `поставь торговый бюджет десять миллионов` ("set the trade budget to ten million"), `напомни через пять минут проверить топливо` ("remind me in five minutes to check the fuel"),
`прибавь скорость на двадцать` ("increase speed by twenty").

**Why:** a bare fragment becomes a "semantic hub" and catches other commands' short phrases; and a parameterless phrase for a command with a required parameter can route to a command that
**cannot run** without a value.

### A value per parameter type (see `dumpsParams`)

| parameter type (required) | what the alias/probe must contain |
|---|---|
| `number` | a number in the phrase: `сбавь ход на 20` ("slow down by 20"), `бюджет десять миллионов` ("budget ten million") |
| `string` / enum | a concrete entity: `наведись на двигатели` ("target the engines"), `добавь платину в добычу` ("add platinum to mining") |
| `boolean` (state) | an explicit on/off: `включи объявления добычи` ("turn on mining announcements"), `разреши стронгхолды` ("allow strongholds"), `выключи радио` ("turn off the radio") |

To check the probes for missing values: dump `dumpsParams`, then go through `probe-phrases-ru.json`
(number → a digit or numeral in every probe; boolean → an on/off/allow/forbid word; string → an entity).

---

## 6. The annotation convention and the matching surface

In the bundle, aliases carry `{name:hint}` placeholders (`{key:X}`, `{minutes:X}`, `{state:true/false}`).
**Every** phrase of a parameterized command must carry the placeholder; the convention and parameter extraction both require it.

But it is **not** the raw alias that goes to the embedder: `AliasEmbeddingText` builds a matching surface —

-
**number** → substitutes an example number (`{key:X}` → `20`); measured to match a real "на двадцать" ("by twenty") better than either the annotation or a bare strip;
- **everything else** (string/enum/boolean) → **stripped** (the phrase's own noun or verb already carries the value).

The bundle and the annotations are left alone; the derived form exists only for embedding.

---

## 7. The limits of embeddings (what aliases will NOT fix)

`multilingual-e5` captures **topic and structure**, but blurs **polarity and content meaning**:

- **Polarity:** `уменьши скорость` vs `увеличь скорость` ("decrease"/"increase speed") ≈ 0.95; `открой люк` vs
  `закрой люк` ("open"/"close the hatch") ≈ 0.96, which is higher than an unrelated pair (~0.84). Antonyms are nearly tied →
  **both** land in offered → the LLM tells them apart. This is normal.
- **A free-text
  parameter** (`set_reminder`): the content ("тритий"/"tritium", "стыковка"/"docking") dominates the "remind me" intent → it catches content commands. Partly cured by strengthening the intent frame; the rest is a hard limit.
- **Bare numbers** (`navigate_to_coordinates`): coordinates `двадцать пять десять` ("twenty five ten") match
  `set_speed_25`.

For these cases the target is **offered@K** (on the list), not hit@1. Measure with `dumpsAntonymCosines`.

---

## 8. Playbook: how to fix a WEAK/WATCH command

1. Open the command's row in Excel: look at `conflict_group`, `competitor`, `suggested_additions`.
2. **Diagnose by cause:**
    - *no examples in the target language* (the alias is the English id) → add localized aliases to
      `ai_action_aliases_ru.properties` (and the missing ones to the base `ai_action_aliases.properties` as the EN fallback).
    - *a bare fragment on another command is
      winning* (for example `двигатели`, "engines", on a query command) → remove or qualify it (`двигатели` → `состояние двигателей`, "engine status").
    - *a collision with a neighbour* → add the **distinguishing** aliases the command lacks (verb plus entity).
    - *a parameterless probe/alias for a required-parameter command* → add the value (see §5), not the empty form.
3. **Do not copy probes verbatim into
   aliases** (overfitting) — write canonical forms; the probes stay an independent check.
4. Run §3 again and check the verdict. Beware
   **whack-a-mole**: strengthening one command pulls it towards its untouched twins, so check those too and go round again if needed.
5. The residue from §7 (polarity, free text, numbers) at offered ≥ 9 is left in place deliberately.

---

## 9. Worked cases (reference examples)

- **`target_subsystem`** (was rank 5 on `наведись на двигатели`, "target the engines"): `query_ship_loadout`
  carried a bare `двигатели` ("engines") alias. Removing it and adding `наведись на двигатели` gave rank 1 at beta=0. The fix was alias hygiene, not scoring.
-
**`decrease_speed`** (WEAK): the probes `быстрее`/`медленнее`/`притормози немного` ("faster"/"slower"/"slow down a bit") carried no number for the required `number` parameter. `притормози` ("slow down") is in any case close to "pull up to a place" (navigation). The fix was probes with a number (`сбавь скорость на десять`, "reduce speed by ten").
- **`set_reminder`** (WEAK→OK): free text; the alias `напомни {key:X}` after stripping is a bare `напомни`
  ("remind"). Replaced by the intent frame `напомни мне / поставь напоминание / не забудь / сделай напоминание`
  ("remind me / set a reminder / don't forget / make a reminder") → offered 10/10.
-
**`navigate_to_pirate_mission_target`**: the bare fragment `к точке пиратской миссии` ("to the pirate mission point") caught other commands' short phrases. Replaced by `проложи маршрут к точке пиратской миссии` ("plot a route to the pirate mission point").
- **`increase_speed` / `decrease_speed` (EN, 2026-07-20)**: the English aliases had
  **one** form (`increase speed by {key:X}`), where ru/de/fr had several. On "set speed plus five" the command scored 0.862 against a cutoff of 0.869 (the top was `set_optimal_speed` at 0.909), so it
  **did not land in
  offered**, and the model honestly answered that it could only do 100% or optimal. The fix was 5 canonical forms per command covering verb × noun (`set speed plus`, `speed plus`, `speed up by`, `throttle up by`); the embedder generalizes on its own to `increase throttle by`, `raise speed by`, `bump speed up by` and so on, so there is no need to enumerate every permutation.
- **`increase_speed` / `decrease_speed` (DE,
  2026-07-20)**: the same class of defect, found by running the probe across languages. In German the aliases named only `Geschwindigkeit`, while
  **`Schub`** belonged to the fixed thrust commands (`voller schub`, `viertel schub`, `schub auf null`). Any thrust-based phrasing (`erhöhe den schub um zehn`, `schub plus fünf`, `schubregler um zehn hoch`) lost to `set_speed_100`: 6 of 16 probes missed offered (ranks 3/24/14/9). The fix was to add the Schub forms to both commands → 0/16 misses, with the fixed-thrust commands still first.
  **Lesson:** check whether a neighbour owns the language's key noun; RU in the same run gave 0/16 immediately (6 aliases per command).
- **`increase_speed` / `decrease_speed` (FR/ES,
  2026-07-20)**: running the remaining languages confirmed the same pattern. FR: aliases only through `vitesse`, while `poussée`/`gaz` belonged to the fixed commands (`pleine poussée`, `quart de poussée`, `plein gaz`) → 2 of 16 probes missed offered (`poussée plus cinq` rank 11, `poussée moins cinq` rank 19). ES:
  **one** alias per command, and `acelerador` belonged to the fixed ones; formally offered 16/16, but at ranks 7-11, behind
  **unrelated** commands (`retract_landing_gear`,
  `target_hostile_highest_threat`, `query_ship_loadout`) — that is, the phrase matched nothing properly and got through only because the leader was weak too. Adding poussée/gaz and acelerador/empuje forms put almost everything first, and the fixed commands did not move.
  **Conclusion:** "offered@K = OK" does not mean healthy: if the leader is an unrelated foreign command, the margin rests on luck, and that is exactly the posture DE and EN fell from.
- **`increase_speed` / `decrease_speed` (IT/UK,
  2026-07-20)**: closing the last two languages that have aliases. UK was an exact copy of the DE/FR defect: aliases only through `швидкість` ("speed"), while `тяга`/`хід`
  ("thrust"/"way") belonged to the fixed commands → 2 of 16 missed (`тяга плюс п'ять` rank 10,
  `плюс десять до тяги` rank 15). Tellingly, **RU in the same run was
  clean**, because there `тягу`/`ходу` were already in the nudge aliases: the same sibling language, a different result, and the only difference was the aliases. IT: every speed command says `velocità`, while `spinta`/`manetta` appeared nowhere →
  `più manetta di dieci` rank 16. The fix was `spinta`/`manetta` and `тяга`/`хід` forms → 0/16 misses in both, with the fixed commands unmoved. The result across all languages:
  **EN, DE, FR, UK, IT had the defect; RU was clean; ES was formally clean but fragile.**
  The regression is locked down in `ThrottleNudgeRoutingTest` (EN/RU/DE/FR/ES/IT/UK, 119 phrasings plus 36 checks that the fixed-speed commands stayed first).

---

## 10. The trap: catalog visibility depends on the LIVE game

`new GameToolCandidates()` filters commands through `Status.getInstance()`, which is the
**live** game status. If the application is running and the commander is docked or on foot, every thrust command disappears from the catalog (146 instead of 164), and the probe or test fails for a reason that has nothing to do with routing.

In tests and probes, pin the situation:
`new GameToolCandidates(Status.detached(PlayerSituation.IN_SHIP_DEEP_SPACE))`. The result is then reproducible and independent of whatever the player is doing.

---

Applies to any language, not only RU. See also the `project_alias_probe_hygiene` memory.

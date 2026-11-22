# Terminology glossaries

Per-language Elite Dangerous terminology caches for the `elite-intel-diagnostic-run` skill. One file per language:
`EN.md`, `RU.md`, `UK.md`, `DE.md`, `ES.md`, `FR.md`, `IT.md`, `PT.md`.

## Why these files exist

`elite-intel-diagnostic-run` generates its own commander utterances using the **real, external** Elite Dangerous
terminology for each language (never the app's own localization, which is what the test verifies). Fetching
that terminology from the web on every run is slow and costly, so the skill builds a glossary once per
language, writes it here, commits it, and reuses it on later runs. Rebuild with the skill's
`--refresh-glossary` argument (or delete the file).

These are a **vocabulary reference, not a phrase bank**: the skill must not replay the example lines verbatim
— it composes fresh, varied utterances each run. The examples only illustrate natural phrasing and confirm
the terms read well in a sentence.

## File format (`<LANG>.md`)

```markdown
# Elite Dangerous terminology — <Language> (<LANG>)

built: <YYYY-MM-DD>   |   game locale: <e.g. ru-RU>
sources:
- <official / wiki URL 1>
- <localized wiki URL 2>

## Terms
| concept (EN)        | localized term(s)                    | notes / source | confidence |
|---------------------|--------------------------------------|----------------|------------|
| landing gear        | шасси                                | wiki           | confirmed  |
| supercruise         | суперкруиз                           | game UI        | confirmed  |
| hardpoints          | орудийные пилоны, жёсткие точки       | wiki           | confirmed  |
| heat sink           | теплоотвод                           | wiki           | uncertain  |
| chaff               | дипольные отражатели                 | wiki           | confirmed  |
| fleet carrier       | флотский авианосец                   | game UI        | confirmed  |
| alexandrite         | александрит                          | wiki (commodities) | confirmed |
| ...                 | ...                                  | ...            | ...        |

## Example phrasings (reference only — do NOT replay verbatim)
- supercruise on: «уходим в суперкруиз», «разгоняй до сверхсвета»
- ...
```

## Rules for whoever builds a glossary

- Cover the concepts the reference test exercises: ship systems, flight/nav, combat/hardpoints, power,
  panels/maps, science/exploration/mining commodities, fleet/squadron carrier, station services, SRV,
  fighter, on-foot.
- Cross-check two sources where possible; mark anything unconfirmed `uncertain` rather than guessing.
- Record the source URLs and the build date so the cache can be audited and refreshed.
- Keep terms independent of this app's `ai_action_aliases_*`/`ed_events_*` files — those are under test.

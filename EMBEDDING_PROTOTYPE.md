# Semantic Embedding Prototype (in-process, CPU)

A working slide-in that turns text into meaning-vectors so inflected Slavic forms and synonyms match without per-language declension/synonym tables. Built to replace the word-overlap
**Reducer** path over time. This branch is a **prototype to develop against**, not a finished feature.

## Why

The reducers (`ai.brain.Reducer`,
`companion.WordOverlapActionReducer`) pre-filter the command catalog to a prompt-sized candidate list by **word overlap
**. That breaks on Russian/Ukrainian: `авианосец`,
`авианосцу`, `авианосцем` are different strings, so a valid command silently drops out before the LLM sees it.
`CompanionWordMatch` (stem/prefix/Levenshtein) and the
`InputNormalizer` synonym tables are hand-built workarounds for this. Embeddings solve it at the root: the model already learned the morphology.

## What's here

| Piece | Path |
|---|---|
| Seam interface | `app/.../ai/embed/TextEmbedder.java` |
| ONNX impl | `app/.../ai/embed/OnnxTextEmbedder.java` |
| Cosine / normalise | `app/.../ai/embed/VectorMath.java` |
| Proof (manual, tagged) | `app/src/test/.../ai/embed/OnnxTextEmbedderManualTest.java` |
| Pure unit test | `app/src/test/.../ai/embed/VectorMathTest.java` |
| Model (bundled) | `distribution/embed/multilingual-e5-small/` |
| Path getter | `AppPaths.getEmbedModelDir()` |
| Deps + test task | `app/build.gradle` |

Model: **multilingual-e5-small**, int8 ONNX (~118 MB) + tokenizer (~17 MB). 384-d vectors. CPU only,
~130 MB system RAM — **does not touch the GPU the game uses**. Packaged in
`distribution/embed/` exactly like Parakeet/Kokoro, so the installer/updater carry it and users never hunt down a model.

## Run the proof

```bash
./gradlew embeddingTest      # loads the real model, prints the cosine matrix, asserts clustering
./gradlew app:test           # default suite — excludes the embedding-manual + local-integration tags
```

Observed (this branch):

```
авианосец  ↔ {авианосца, авианосцу, авианосцем}   ≈ 0.98–0.99   (same word, 4 cases)
авианосец  ↔ "курс на авианосец"                  ≈ 0.93        (synonym phrase)
авианосец  ↔ "выбросить помехи"                   ≈ 0.79        (unrelated command)
```

> e5 cosines sit in a high band (≈0.79 floor); what matters is the **ordering and gap**, so compare scores
> against each other / a threshold, not against an absolute like 0.5.

## How it works

`embed(text)` → prepend e5's
`"query: "` prefix → DJL HuggingFace tokenizer → ONNX Runtime → attention-mask-weighted mean-pool of token vectors → L2-normalise. With unit vectors,
`VectorMath.cosine` is just a dot product.

## Legacy `Reducer` — wired, toggle at runtime (A/B)

The legacy LLM-pipeline
`Reducer` now has both strategies behind one system property. Word-overlap is the default, so nothing changes until you opt in:

```bash
# default — exact word-overlap, no model loaded
java ... -jar distribution/elite_intel.jar
# semantic — embeds input, matches by meaning (loads the model lazily on first utterance)
java ... -Delite.intel.reducer=semantic -jar distribution/elite_intel.jar
```

Run the app once each way and compare routing on Russian/Ukrainian phrases. Tuning knobs (also
`-D`, no recompile), since e5 cosines sit in a high, compressed band:

| Property | Default | Meaning |
|---|---|---|
| `elite.intel.reducer.semantic.floor` | `0.80` | below this best-similarity → treat input as unrelated (fallback) |
| `elite.intel.reducer.semantic.margin` | `0.04` | keep candidates within this of the best score |
| `elite.intel.reducer.semantic.max` | `25` | hard cap on surviving candidates |

Notes: the model loads **lazily and only in semantic mode
**, so word-overlap keeps its exact original cost. Catalog phrases are embedded once and cached (
`SemanticPhraseMatcher`), so only the first semantic utterance pays the catalog cost; after that just the input is embedded per turn. Exact-alias preservation and the conversation/nonsensical fallback are shared by both strategies. This toggle is a deliberately small in-file switch — the legacy pipeline is temporary, and when it goes the durable piece (
`SemanticPhraseMatcher`) stays.

A/B proof: `./gradlew embeddingTest` runs `SemanticReducerAbTest` — word-overlap drops the inflected
`авианосцу`, semantic keeps it as `{авианосец=carrier_status}`.
`ReducerWordOverlapTest` (default suite, no model) locks the unchanged word-overlap behaviour across the refactor.

## The seam — where this plugs into selection (companion path)

`CompanionActionReducer` is already documented as the swap point ("can later be replaced by a smarter
(e.g. semantic) reducer without touching any call site"). Reuse the same `SemanticPhraseMatcher`. Next step:

1. At startup, embed every command's training phrase once; cache the vectors (a plain `float[][]` keyed by command id —
   **no vector DB needed** at a few hundred commands; consider caching to disk to skip the one-time startup cost).
2. Per utterance: `embed(input)`, cosine vs the cached catalog, keep top-N over a threshold → candidate list.
3. Implement `SemanticActionReducer implements CompanionActionReducer`, **A/B against**
   `WordOverlapActionReducer` on the Russian/Ukrainian phrase set, keep the fast-paths (exact alias,
   `ReflexResolver`) in front.
4. Reuse the same embedder for `SessionMemoryGateway` recall (it currently leans on
   `CompanionWordMatch`). One mechanism upgrades command selection *and* memory.

## Offline natives (done) — the bundling example

Both natives this feature needs are **bundled in the fat jar and load with no network**:

- **ONNX Runtime**: native ships inside the `com.microsoft.onnxruntime` jar; nothing to do.
- **Tokenizer (DJL)**: the `tokenizers` jar already bundles CPU natives at
  `native/lib/<os>/cpu/libtokenizers.*` (verified present in
  `distribution/elite_intel.jar` for the supported targets, linux-x86_64 and win-x86_64). The catch: DJL appends a CUDA
  *flavor* (e.g.
  `cu122`) when it sees an NVIDIA GPU — which most Elite players have — and then downloads a CUDA-flavored native that isn't bundled.
  `DjlTokenizerNatives.configure()` pins `RUST_FLAVOR=cpu` (+ `ai.djl.offline=true`
  as a fail-fast guard) **before the first tokenizer load
  **, so DJL uses the bundled CPU native. We run the model on CPU anyway. Operator-set env/
  `-D` values are left untouched, so a power user can override.

  Proof: wipe `~/.djl.ai` and run `./gradlew embeddingTest` — the log shows `Uses override RUST_FLAVOR: cpu`
  and loads from the `...-cpu-...` path with **no "Downloading jni" line**.

  This is the template for bundling any future native offline: find the loader's override knob, pin it to the variant you ship, turn on offline as a guard — mirrors
  `SherpaOnnxNatives`.

## Hardening (done in this branch)

- **Graceful degradation.** If semantic mode is selected but the model is absent/corrupt,
  `Reducer` logs one warning and falls back to word-overlap for the session instead of throwing on every utterance. It matches how the project degrades other optional backends.
- **Selection math under test.**
  `SemanticReducerLogicTest` (default suite, no model) covers the floor / margin / cap behaviour and the null-matcher degradation path via a deterministic stand-in
  `TextEmbedder`.

## Known gaps to close before shipping

- **First-utterance catalog warm (perf).
  ** The first semantic turn embeds the whole catalog on the routing thread (a few seconds; not the EDT). The startup connection check does
  **not
  ** cover this — it short-circuits before the reducer. A background pre-warm when semantic mode is enabled removes the stall.
- **ORT vs sherpa onnxruntime coexistence.
  ** ORT loads its own onnxruntime native alongside sherpa's. Fine on Linux here; verify on Windows (Parakeet already documents the System32 DLL-precedence trick).
- **Thread safety.** One `OnnxTextEmbedder` = one ONNX session, not safe for concurrent
  `embed`. Pool or guard if called from multiple threads.
- **Model choice.** e5-small is the prototype size.
  `bge-m3` is stronger on Ukrainian but ~600 MB int8 — evaluate if quality on real phrases is short.
- **Prefix semantics.** e5 wants `"query: "`/`"passage: "`; we use
  `"query: "` symmetrically, which is correct for short-text matching. Revisit if used for asymmetric retrieval.

```

# Vision & Background

**Status:** Ported from the EDO StellarCore documentation set to Elite-Intel. The goals below are the ones that survived the change of host; the ones that belonged to the shell itself did not.

---

## Executive Summary

**BindForge** and **StarVizion** are two companion tools for Elite Dangerous: Odyssey, designed over roughly two years and specified in detail during 2026. They are now being built as features of [Elite-Intel](https://github.com/SudoKrondor/EliteIntel), SudoKrondor's LLM side-kick and data analyst for Elite Dangerous.

1. **BindForge** — a safe external editor and backup tool for Elite Dangerous control bindings — keyboard, mouse and controllers alike — with live device-input monitoring and device-layout views to support binding assignment. BindForge is not a floating overlay — that is StarVizion's domain.
2. **StarVizion** — a live controller-input and game related data visualization overlay system (desktop and VR).

## The Problem that requires BindForge

Elite Dangerous players have only the in game bind managment system for editing and determining how their controllers will send input to the game. In addition to the .binds file there are 3 other game controller files that each control an aspect of how a players controls look and work in game. These are manually editable, but it becomes hard to know what to do where with users having to determine the XML and conventions that the Frontier Developers designed. BindForge is designed to help the user manage these 4 domains and make it easier to customise the controller inputs and ways that the game displays those controls in game. Elite Dangerous's Control configuration is fragile: the game has, at least once, overwritten a player's `DeviceMappings.xml` back to factory defaults during an update. A complex binding set — HOTAS, HOSAS, Game Controllers, keyboard, mouse, or the usual mixture of all — represents hours of work
and is stored in files the game will happily clobber. Protecting that configuration from loss is BindForge's first priority, ahead of editing it.

## The Problem that requires StarVizion

StarVizion's problem is narrower: players flying with springless HOTAS or HOSAS setups have no physical center or stop feedback, and inside a VR headset they cannot see their own hands. They need visual confirmation of control state that the game does not provide. Additionally, Elite-Intel's current overlay system provides players information about missions and other in game activities progress. StarVizion seeks to add capabilty for users to design their own overlays that contain information that they would like to show in game.

## History

This work has gone through several attempts under different names — an early host-shell design, a separate prototype of the StarVizion concept, an implementation of a bindings-diagnostic feature inside a voice-companion tool, and a full Windows-only rebuild. Each produced real design work and, sometimes, real working software. None shipped a usable v1.

The most recent of those attempts was **EDO StellarCore**: a cross-platform host application with a plugin framework, hosting BindForge, StarVizion, and a third plugin that is not being built — see [Other Ideas](../04-other-ideas/FlightDeck.md). Most of the documentation in this set was written for that design.

**Why the host changed.** Building a plugin host is a large project that produces nothing a player can use until the plugins arrive. Elite-Intel is an existing, shipping application with an active community, and it already supplies — as working code — most of what the StellarCore shell was going to have to build: a device input service, journal parsing, path handling, settings persistence, a native overlay with an OpenVR backend, and a first-generation bind editor. Building BindForge and StarVizion into Elite-Intel means they reach players without a shell being finished first. See the [Elite-Intel Platform Map](../01-host-integration/elite-intel-platform-map.md) for exactly what the host provides and where the gaps are.

**What was given up.** The plugin ecosystem goal — a documented contract letting the community extend a shared platform — does not carry over. Elite-Intel accepts contributions to the application itself, not plugins to a framework. That was a real goal of the StellarCore design and it is genuinely abandoned here, not deferred.

**What was kept.** Every piece of Elite Dangerous domain knowledge — file formats, install paths, the action catalog, conflict rules — survived every prior attempt unchanged and survives this one too. It is preserved alongside the feature specs in each `domain-knowledge/` folder, and it is the most durable asset in this documentation set.

## Goals

- Protect Elite Dangerous control configuration from loss, and make it viewable and editable outside the game.
- Give VR and desktop players live, accurate visual feedback of their control inputs.
- Reach players inside an application they already run, rather than as another separate tool to install.
- Build on Elite-Intel's existing infrastructure rather than duplicating it — particularly `elite.intel.devices`, which is the input layer both features need.
- Leave Elite-Intel better for having accepted these features: shared infrastructure improvements should be usable by the rest of the application, not private to BindForge or StarVizion.

## Non-Goals

- Replacing in-game UI or automating gameplay.
- Interacting with the running game process, or reading or modifying game memory.
- Building a plugin framework inside Elite-Intel.
- Supporting every possible controller/device on day one without a compatibility strategy.
- Any feature requiring the player to install external dependencies — explicitly forbidden by Elite-Intel's contribution rules.

## Constraints Inherited From the Host

Elite-Intel's `DEVELOPERS.md` sets rules that apply to everything here:

- **Java 21, Gradle, Swing.** Pure Java; no external runtime dependencies beyond configured APIs.
- **Event-driven.** New features subscribe to journal events or custom events via `@Subscribe`.
- **No AFK automation**
- **No game controller configuration file modification** beyond what the player explicitly asks for.
- **No JNI to unsigned libraries** unavailable on both Windows and Linux;
- **No reading or modifying in-game memory**. Both are automatic PR rejections.

Open questions and unresolved design decisions live in [conflicts-and-open-questions.md](conflicts-and-open-questions.md); scope decisions live in [v1.2-scope.md](v1.2-scope.md).

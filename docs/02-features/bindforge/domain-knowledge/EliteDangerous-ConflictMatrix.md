# Elite Dangerous — Subgroup Conflict Matrix (Results)

**Scope:** Raw results of in-game conflict testing, section by section. For the analysis of what
these results mean and how BindForge's own conflict-detection algorithm should use them, see
`EliteDangerous-ConflictRules.md`. This doc is data only.

**Method:** for each pair of subgroups listed below, bind the same key to one action in each
subgroup via the in-game rebind dialog and observe whether the game warns. Tested 2026-07-30
using the interactive matrices in this folder (`ConflictMatrix-General.html`,
`ConflictMatrix-Ship.html`, `ConflictMatrix-SRV.html`, `ConflictMatrix-OnFoot.html`).

**Open caveat — not yet resolved, see `EliteDangerous-ConflictRules.md` §3:** these four tables
assume the game's top-level file sections (General / Ship / SRV / On Foot) never conflict with
each other, only subgroups *within* the same section do. There is at least one unconfirmed
observation of a conflict between Ship and SRV during testing. Until that's confirmed or ruled
out, treat the boundaries between these four tables as provisional, not settled.

---

## General Controls

| | Interface Mode | Galaxy Map | Camera Suite | Free Camera | Holo-Me | Playlist | Store Camera | System Colonization Facility Placement |
|---|---|---|---|---|---|---|---|---|
| **Interface Mode** | — | no | no | no | **YES** | **YES** | **YES** | no |
| **Galaxy Map** | no | — | no | no | no | **YES** | no | no |
| **Camera Suite** | no | no | — | no | **YES** | **YES** | **YES** | no |
| **Free Camera** | no | no | no | — | **YES** | **YES** | **YES** | no |
| **Holo-Me** | **YES** | no | **YES** | **YES** | — | **YES** | no | no |
| **Playlist** | **YES** | **YES** | **YES** | **YES** | **YES** | — | no | no |
| **Store Camera** | **YES** | no | **YES** | **YES** | no | no | — | no |
| **System Colonization Facility Placement** | no | no | no | no | no | no | no | — |

---

## Ship Controls

| | Mouse Controls | Flight Rotation | Flight Thrust | Alternate Flight Controls | Flight Throttle | Flight Landing Overrides | Flight Miscellaneous | Targeting | Weapons | Cooling | Miscellaneous | Mode Switches | Headlook Mode | Multi-Crew | Fighter Orders | Full Spectrum System Scanner | Detailed Surface Scanner |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **Mouse Controls** | — | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | **YES** | no | no |
| **Flight Rotation** | **YES** | — | **YES** | no | **YES** | no | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | no | no |
| **Flight Thrust** | **YES** | **YES** | — | no | **YES** | no | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | **YES** | no | no | no |
| **Alternate Flight Controls** | **YES** | no | no | — | no | no | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | no | no |
| **Flight Throttle** | **YES** | **YES** | **YES** | no | — | no | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | no | no |
| **Flight Landing Overrides** | **YES** | no | no | no | no | — | no | no | no | no | no | **YES** | no | **YES** | no | no | no |
| **Flight Miscellaneous** | **YES** | **YES** | **YES** | **YES** | **YES** | no | — | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | no | no |
| **Targeting** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | — | **YES** | **YES** | **YES** | **YES** | no | **YES** | no | no | no |
| **Weapons** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | **YES** | — | **YES** | **YES** | **YES** | no | **YES** | no | no | **YES** |
| **Cooling** | **YES** | **YES** | **YES** | **YES** | **YES** | no | **YES** | **YES** | **YES** | — | **YES** | **YES** | no | **YES** | no | no | no |
| **Miscellaneous** | **YES** | **YES** | no | **YES** | **YES** | no | **YES** | **YES** | **YES** | **YES** | — | **YES** | no | **YES** | no | no | no |
| **Mode Switches** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | — | **YES** | **YES** | no | no | **YES** |
| **Headlook Mode** | no | no | no | no | no | no | no | no | no | no | no | **YES** | — | **YES** | no | no | no |
| **Multi-Crew** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | **YES** | — | no | no | no |
| **Fighter Orders** | **YES** | no | no | no | no | no | no | no | no | no | no | no | no | no | — | no | no |
| **Full Spectrum System Scanner** | no | no | no | no | no | no | no | no | no | no | no | no | no | no | no | — | no |
| **Detailed Surface Scanner** | no | no | no | no | no | no | no | no | **YES** | no | no | **YES** | no | no | no | no | — |

---

## SRV Controls

| | Driving | Driving Targeting | Driving Turret Controls | Drive Throttle | Driving Miscellaneous | Driving Mode Switches |
|---|---|---|---|---|---|---|
| **Driving** | — | **YES** | no | **YES** | **YES** | **YES** |
| **Driving Targeting** | **YES** | — | no | **YES** | **YES** | **YES** |
| **Driving Turret Controls** | no | no | — | no | no | **YES** |
| **Drive Throttle** | **YES** | **YES** | no | — | **YES** | **YES** |
| **Driving Miscellaneous** | **YES** | **YES** | no | **YES** | — | **YES** |
| **Driving Mode Switches** | **YES** | **YES** | **YES** | **YES** | **YES** | — |

---

## On Foot Controls

| | On Foot | On Foot Mode Switches | On Foot Emotes |
|---|---|---|---|
| **On Foot** | — | **YES** | **YES** |
| **On Foot Mode Switches** | **YES** | — | **YES** |
| **On Foot Emotes** | **YES** | **YES** | — |

Fully connected — every subgroup conflicts with every other subgroup here, unlike Ship and SRV
which both have isolated pockets (Multi-Crew/FSS/DSS in Ship; Driving Turret Controls in SRV).

# Vendored OpenVR

`openvr_capi.h` — the C ("flat") binding for Valve's OpenVR SDK, used by
`overlay/src/platform_openvr.c` to talk to SteamVR.

| | |
|---|---|
| Upstream | https://github.com/ValveSoftware/openvr |
| Pinned to | **v2.15.6** (`headers/openvr_capi.h`) |
| Vendored on | 2026-08-01 |
| Interface | `IVROverlay_028`, `IVRSystem_026` |
| Licence | BSD-3-Clause, see `LICENSE` |

The C header rather than
`openvr.h` because the overlay is C11 and the C++ header would drag a C++ toolchain into a build that does not otherwise need one.

Included with `-isystem`, not `-I`: it is generated, and its ~40 unused
`static const char *` version constants would otherwise bury our own `-Wextra`
warnings.

## The matching runtime library ships with the app

`distribution/overlays/openvr_api.dll` and `libopenvr_api.so` come from the
**same v2.15.6 release** and are loaded at runtime (`LoadLibrary`/
`dlopen`), never linked. Keeping the three in lockstep is not optional — see below.

**When bumping the SDK, replace all three together**: this header, the DLL and the
`.so`. A header newer than the shipped library means asking a library for an interface it does not have.

## Do NOT add a "try an older interface version" fallback

The obvious idea for supporting older SteamVR — ask for `IVROverlay_028`, and on failure ask for
`_027` using the same struct — is **unsafe**, and was measured rather than guessed. Diffing the function tables:

```
IVROverlay_028 vs _027:  +CreateSubviewOverlay (slot 3), +SetSubviewPosition (slot 43)
                         80 functions -> 82
```

The inserts are near the front, so of the functions this overlay calls, only
`CreateOverlay` keeps its slot:

| function | slot in _027 | slot in _028 |
|---|---:|---:|
| `CreateOverlay` | 2 | 2 |
| `DestroyOverlay` | 3 | 4 |
| `SetOverlayWidthInMeters` | 22 | 23 |
| `SetOverlayTransformTrackedDeviceRelative` | 35 | 36 |
| `ShowOverlay` | 42 | 44 |
| `PollNextOverlayEvent` | 47 | 49 |
| `SetOverlayRaw` | 61 | 63 |

Calling a `_027` table through the
`_028` struct would invoke whatever function happens to sit at the shifted offset, with the wrong arguments. Supporting both means vendoring both headers and both structs, not reusing one.

Shipping our own matching library is what makes this moot: the loader prefers the copy beside the binary, so the interface version is always one the library knows, whatever SteamVR the commander has.

## `VR_IsInterfaceVersionValid` is not a pre-flight check

It queries the
*runtime*, so it answers "no" whenever SteamVR is simply not running yet — which would turn the ordinary "app started before SteamVR" case into a hard version error. The interface version is settled where it can actually be known: when the function table is requested after a successful init.

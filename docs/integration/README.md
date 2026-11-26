# Partner Integrations

Other programs on the user's PC integrate with Elite Intel through one local WebSocket,
`ws://127.0.0.1:7497` (`elite.intel.integration.IntegrationSocket`). It listens on loopback only, refuses web pages
from other hosts by their `Origin` header, and carries only typed JSON messages with a `version` and a `type`. The
socket rules are written once, in the first integration's spec below. A new partner reuses them and adds its own
message types.

Each partner integration has one spec here:

| Partner | Spec | What it does |
|---|---|---|
| FrameShift Command | [FrameShiftIntegration.md](FrameShiftIntegration.md) | Opens FrameShift pages by voice, and speaks FrameShift's announcements in the ship's voice |

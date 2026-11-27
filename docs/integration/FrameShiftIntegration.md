# FrameShift Command Integration

**Status:** built for V1.2 (branch `V1.2-frameshift-integration`), 2026-11-26. FrameShift Command's side is
theirs: they open the connection from an optional setting on their own Settings page, and they own the code that
opens the page. Elite Intel has no UI for this.

It works in two directions:

- **Page opening.** The user says "open materials in FrameShift", or "show the manufactured materials tab in
  FrameShift". VEGA calls one tool, `frameshift_open_page`. Elite Intel sends the call to FrameShift over a local
  WebSocket and waits for the answer. Then it says what happened.
- **Announcements.** FrameShift sends a line of text, and Elite Intel speaks it at once in the active ship's voice.

---

## The socket

`elite.intel.integration.IntegrationSocket` listens on **`ws://127.0.0.1:7497`**, on loopback only. Connect to
`127.0.0.1`, not `localhost`: on some systems `localhost` resolves to `::1` first, and nothing listens there.

This socket used to stream debug dumps: LLM prompts, journal events, and Spansh/EDSM requests and replies. That was
left over from V1.0 debugging, nobody used it, and it is gone. The socket now carries only the typed messages
below. Anything else a client sends is ignored.

It refuses a handshake whose `Origin` header names a web page served from another host. A browser lets any site
open a socket to localhost, so the origin is how such a page gives itself away. A desktop program sends no origin,
`null`, a non-web scheme (`file:`, `app:`), or a page on `localhost`/`127.0.0.1`/`[::1]`. All of those connect.

## Messages

Every message is one JSON object with `"version": 1` and a `type`.

### FrameShift registers

```json
{"version": 1, "type": "integration.register", "client": "frameshift-command",
 "pages": [{"id": "materials", "label": "Materials",
            "tabs": [{"id": "raw", "label": "Raw"}, {"id": "manufactured", "label": "Manufactured"}]},
           {"id": "dashboard", "label": "Dashboard"}]}
```

`tabs` is optional. A page that has none opens as it is.

The reply:

```json
{"version": 1, "type": "integration.registered", "client": "frameshift-command", "ok": true}
```

The page list goes into the LLM prompt, so Elite Intel keeps it short and plain:

- An `id` must match `[A-Za-z0-9_-]{1,32}`. A page with any other id, or a repeated id, is dropped.
- A `label` keeps letters, digits, spaces and `& ' . / ( ) -`, and is cut to 40 characters. A missing or empty
  label becomes the id.
- At most 32 pages are kept.
- A tab follows the same `id` and `label` rules. A page keeps at most 8 tabs, and a repeated tab id is dropped.
- Pages and tabs together are capped at 64, because each one is a value the model is offered. Once the cap is
  reached, later tabs and pages are dropped.

A refused registration replies `"ok": false` with `"error": {"code", "message"}`:

| code | when |
|---|---|
| `unknown_client` | `client` is not `frameshift-command` |
| `unsupported_version` | `version` is not `1` |
| `no_valid_pages` | no page survived the rules above |

If FrameShift registers again, the new registration replaces the old one, even on another connection. When the
registered connection closes, the registration is forgotten.

The user hears when FrameShift connects and when it drops (`handler.frameshift.connected` /
`.disconnected`), in the active ship's voice, because the page tool exists only while FrameShift is registered.
Without that, "open materials in FrameShift" just goes to another tool and nothing says why. Only a real change is
spoken: a registration while none is held is "connected", and FrameShift closing the registered connection is
"disconnected". A re-registration, a replacement, an old replaced connection closing, or Elite Intel closing the
socket on shutdown says nothing. Both are also logged at info (`elite.intel.integration`), so a support bundle shows
whether FrameShift was connected.

### Elite Intel calls a tool

```json
{"version": 1, "type": "tool_call", "id": "<uuid>", "target": "frameshift-command",
 "tool": "frameshift_open_page", "arguments": {"page": "materials", "tab": "manufactured"}}
```

`tab` is sent only when the user named a tab. Without it, FrameShift opens the page and leaves whatever tab it
was on.

### FrameShift answers

```json
{"version": 1, "type": "tool_result", "id": "<same uuid>", "source": "frameshift-command", "ok": true,
 "data": {"page": "materials", "label": "Materials"}}
```

or, if it failed:

```json
{"version": 1, "type": "tool_result", "id": "<same uuid>", "source": "frameshift-command", "ok": false,
 "error": {"code": "page_unavailable", "message": "..."}}
```

Replies are matched to calls by `id`. A reply counts only if it arrives on the connection the call was sent on.

### FrameShift sends an announcement

```json
{"version": 1, "type": "announce", "id": "a-17", "source": "frameshift-command",
 "text": "Materials restocked. You can craft the module now."}
```

Elite Intel speaks `text` word for word, in the active ship's voice, through the same path as its own system
speech. No LLM sees it, and nothing rewords or translates it, so FrameShift sends it in the language the user
should hear. It can be interrupted, like any of Elite Intel's system speech.

The reply means the line was accepted and queued for speech, not that it has been heard:

```json
{"version": 1, "type": "announce_result", "id": "a-17", "ok": true}
```

`id` is optional, and is echoed when it is sent. The rules:

- Control characters become spaces, runs of white space become one space, and the text is trimmed.
- At most 300 characters after that. A longer line is refused, not cut.
- At most 5 accepted announcements in any 10 seconds. A line past that is refused. Nothing is queued for
  later.

A refused line replies `"ok": false` with `"error": {"code", "message"}`:

| code | when |
|---|---|
| `not_registered` | the connection has not registered, or another one has registered since |
| `empty_text` | `text` is missing, or nothing is left after cleaning |
| `text_too_long` | more than 300 characters |
| `rate_limited` | 5 lines were already accepted in the last 10 seconds |

## What the user hears after a tool call

Elite Intel queues nothing. It speaks one line from `responses*.properties`, `handler.frameshift.*`:

| outcome | when |
|---|---|
| `opened` | `ok: true` |
| `refused` | `ok: false`. The error code and message are logged, not spoken, because they are English text from another program |
| `offline` | no FrameShift registered, or the connection dropped while the call waited |
| `timedOut` | no reply within 5 seconds |
| `unknownPage` | the page, or the tab of that page, is not in the registered list |

The spoken name is the page's label, or the page's and the tab's joined: "Materials, Manufactured".

`FrameShiftLink` publishes an announcement as an `AiVoxResponseEvent`, with no voice named, so the Mouth uses
the active ship's voice. The link takes the announcer and the clock as constructor arguments, so its tests can
check what was spoken and step the rate window without a TTS engine or a wait.

## How the tool reaches the model

`FrameShiftOpenPageCommand` is an ordinary `@RegisterCommand`:

- `isVisibleForLLM` is true only while FrameShift is registered. A user without FrameShift is never offered
  the tool.
- `parameters()` is read every turn. The model picks **one** value, a page or a page's tab, and never pairs two
  arguments: the `page` parameter's `enum` is every page id, each followed by its tabs as `page/tab`
  (`materials`, `materials/raw`, `materials/manufactured`, ...). One value means a small local model cannot pair a
  tab with the wrong page. Elite Intel splits the value again before it sends the call. The description spells out
  `id = label` only where the id does not already say the label (`navigation = Route Planner`), to keep the prompt
  short.
- Alias phrases (`frameshift_open_page` in `ai_action_aliases*.properties`) carry a variable `{page:X}`. The
  reflex never fires for this command, so the LLM always picks the page.

# FrameShift Command Integration

**Status:** built for V1.2 (branch `V1.2-frameshift-integration`), 2026-11-26. FrameShift Command's side is
theirs: they open the connection from an optional setting on their own Settings page, and they own the code that
opens the page. Elite Intel has no UI for this.

The user says "open materials in FrameShift". VEGA calls one tool, `frameshift_open_page`. Elite Intel sends
the call to FrameShift over a local WebSocket and waits for the answer. Then it says what happened.

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
 "pages": [{"id": "materials", "label": "Materials"}, {"id": "dashboard", "label": "Dashboard"}]}
```

The reply:

```json
{"version": 1, "type": "integration.registered", "client": "frameshift-command", "ok": true}
```

The page list goes into the LLM prompt, so Elite Intel keeps it short and plain:

- An `id` must match `[A-Za-z0-9_-]{1,32}`. A page with any other id, or a repeated id, is dropped.
- A `label` keeps letters, digits, spaces and `& ' . / ( ) -`, and is cut to 40 characters. A missing or empty
  label becomes the id.
- At most 32 pages are kept.

A refused registration replies `"ok": false` with `"error": {"code", "message"}`:

| code | when |
|---|---|
| `unknown_client` | `client` is not `frameshift-command` |
| `unsupported_version` | `version` is not `1` |
| `no_valid_pages` | no page survived the rules above |

If FrameShift registers again, the new registration replaces the old one, even on another connection. When the
registered connection closes, the registration is forgotten.

### Elite Intel calls a tool

```json
{"version": 1, "type": "tool_call", "id": "<uuid>", "target": "frameshift-command",
 "tool": "frameshift_open_page", "arguments": {"page": "materials"}}
```

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

## What the user hears

Elite Intel queues nothing. It speaks one line from `responses*.properties`, `handler.frameshift.*`:

| outcome | when |
|---|---|
| `opened` | `ok: true` |
| `refused` | `ok: false`. The error code and message are logged, not spoken, because they are English text from another program |
| `offline` | no FrameShift registered, or the connection dropped while the call waited |
| `timedOut` | no reply within 5 seconds |
| `unknownPage` | the page is not in the registered list |

## How the tool reaches the model

`FrameShiftOpenPageCommand` is an ordinary `@RegisterCommand`:

- `isVisibleForLLM` is true only while FrameShift is registered. A user without FrameShift is never offered
  the tool.
- `parameters()` is read every turn. The `page` parameter's `enum` is the registered page ids, and its description
  lists `id = label` pairs, so the model can map "materiales" to `materials`.
- Alias phrases (`frameshift_open_page` in `ai_action_aliases*.properties`) carry a variable `{page:X}`. The
  reflex never fires for this command, so the LLM always picks the page.

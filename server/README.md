# FrameCraft Voice Assistant — Server

The backend "brain" for the FrameCraft voice assistant iOS app. The iOS app does
speech-to-text and text-to-speech on-device; this service just takes the
transcribed phrase, runs it through Claude with the FrameKraft MCP tools, and
returns a short speakable reply.

```
POST /api/assistant/query
Headers: x-assistant-key: <ASSISTANT_API_KEY>
Body:    { "transcript": "what's the status on the Rice University order" }
Reply:   { "reply": "...", "toolCallsUsed": ["orders_get", "..."] }
```

## Why it's built this way

- **No STT/TTS API keys needed.** iOS's built-in Speech framework and
  AVSpeechSynthesizer handle voice in both directions for free, on-device.
  This server only does reasoning + tool calls.
- **Real tools, not a rebuild of them.** Rather than re-implementing
  FrameKraft's order/customer/invoice logic here, this service connects to
  your existing **FrameKraft MCP server** as a remote MCP connector inside
  the Claude API call (the same mechanism Claude uses in chat). One set of
  tools, one source of truth.
- **Single-user, real actions → real auth.** This isn't a demo — it can send
  emails, touch orders, and change invoice data through your live tools, so
  every request requires a shared-secret header (`x-assistant-key`). Treat
  that value like a password. Rotate it if it ever leaks.

## Setup

### 1. Point it at FrameKraft's MCP server

This service expects FrameKraft's MCP server to be reachable over HTTP
(the MCP "url"/SSE transport), the same way the `Framekraft_MCP` tools are
reachable in a Claude chat session. If that MCP server isn't already
deployed with a public URL:

- Check whether the `framekraft` repo already runs its MCP server as part
  of the main app (many setups expose it at `/mcp` on the same Railway
  service) — if so, `FRAMEKRAFT_MCP_URL` is just that app's URL + `/mcp`.
- If it's only ever been used locally / through a Claude session config,
  it needs its own public Railway deployment before this service can
  reach it. That's a one-time step on the `framekraft` repo, not this one.

Set `FRAMEKRAFT_MCP_URL` (and `FRAMEKRAFT_MCP_TOKEN` if that server requires
auth) in Railway → Variables.

If you'd rather ship v1 without live FrameKraft tools (just a general
Claude voice assistant) leave `FRAMEKRAFT_MCP_URL` unset — the server runs
fine without it and simply won't attach tools until it's configured.

### 2. Environment variables

Copy `.env.example` to `.env` for local dev, or set these directly in
Railway → Variables for deployment:

| Variable | Required | Notes |
|---|---|---|
| `ANTHROPIC_API_KEY` | yes | From console.anthropic.com |
| `CLAUDE_MODEL` | no | Defaults to `claude-sonnet-4-5` |
| `ASSISTANT_API_KEY` | yes | Long random string; the iOS app must send this exact value |
| `FRAMEKRAFT_MCP_URL` | no* | *Required for the assistant to actually do anything with FrameKraft data |
| `FRAMEKRAFT_MCP_TOKEN` | no | Only if FrameKraft's MCP server requires a bearer token |

### 3. Local dev

```bash
npm install
cp .env.example .env   # fill in values
npm run dev
```

### 4. Deploy on Railway

1. Push this repo to GitHub (already done if you're reading this from the repo).
2. In Railway: New Project → Deploy from GitHub repo → select
   `framecraft-voice-assistant`, set the root directory to `server/`.
3. Set the environment variables above in the Railway service's Variables tab.
4. Railway auto-detects `railway.json` and deploys via Nixpacks.
5. Once deployed, copy the generated `*.up.railway.app` URL — the iOS app
   needs it (see `ios/README.md`).
6. Confirm it's live: `curl https://YOUR-APP.up.railway.app/health`

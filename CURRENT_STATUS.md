# Current Status

English: `CURRENT_STATUS.md` | 中文：`CURRENT_STATUS.zh-CN.md`

## License

- License: Apache License 2.0 (`LICENSE`)

## Completion

- Android-native nanobot core: functionally near-complete.
- Python nanobot parity target: close on core agent behavior, not complete on ecosystem layers.

## Stable Capabilities

- Persistent chat sessions with memory-aware prompting
- Workspace-restricted mode with policy-enforced tool access
- Local orchestration through `delegate_task`
- Background heartbeat and reminder automation
- Dynamic MCP tools with cached discovery and partial-refresh retention
- Image attachments stored locally and visible to the OpenAI-compatible provider path

## Known Non-Blockers

- MCP still needs stronger production hardening for auth, timeout, health-check, and retry/backoff.
- Dynamic MCP cache freshness is not yet surfaced with timestamps or "last successful refresh" metadata.
- Multimodal support is still image-only and only one provider path is wired for payload delivery.
- Channels / bridge / CLI ecosystem features are not implemented.

## Terminology Guide

- `restrictToWorkspace`: the persisted config flag.
- `workspace-restricted mode`: the user-facing and runtime-facing description of that flag being enabled.
- `local orchestration tools`: tools like `delegate_task` that coordinate local child runs without being plain read-only tools.
- `image attachments`: message-level image references stored in app-private storage and optionally prepared for provider payloads.
- `dynamic MCP tools`: cached tool definitions discovered from enabled MCP servers and exposed through the standard tool registry.

## Next Directions

1. Continue MCP hardening.
2. Improve multimodal provider coverage and diagnostics.
3. Do final UX/documentation polish before any ecosystem expansion.

# Android Nanobot Development Progress And Roadmap

English: `DEVELOPMENT_PROGRESS_AND_ROADMAP.md` | 中文：`DEVELOPMENT_PROGRESS_AND_ROADMAP.zh-CN.md`

## Current Snapshot

- Project state: late-stage Android-native nanobot recreation with stable core agent loops, background automation, local skills, subagents, dynamic MCP tools, workspace tools, web tools, and first-stage multimodal image support.
- Architecture: single-module Android app using Kotlin, Compose, Room, DataStore, Hilt, WorkManager, Retrofit/OkHttp, and kotlinx.serialization.
- License: Apache License 2.0 (`LICENSE`).
- Verification baseline: `:app:testDebugUnitTest` and `:app:assembleDebug` are the standard local confidence checks and currently pass.

## What Is Already In Place

- Core chat agent: persistent sessions, session switching, prompt presets, memory context, tool loop execution, progress events, and cancel support.
- Tooling: local read-only tools, workspace sandbox read/write tools, local orchestration via `delegate_task`, external web tools, and policy-based blocking.
- Safety model: `restrictToWorkspace` keeps local read-only tools, local orchestration tools, and workspace sandbox read/write tools available while blocking external web access, dynamic MCP tools, and non-workspace side effects.
- Background automation: heartbeat decision flow, reminder delivery flow, recurring scheduler logic, notification sinks, and worker tests for enqueue/cancel/skip/deliver/fail paths.
- Local skills: built-in skill catalog, persisted enablement, settings toggles, prompt injection, and runtime visibility.
- Subagents: isolated child sessions, summary return, artifact path return, parent-session stability, and recursion guard.
- MCP dynamic tools: remote HTTP JSON-RPC discovery, cached dynamic tool exposure, policy integration, settings management, refresh retention on partial failure, and endpoint validation.
- Multimodal image baseline: image attachment import into app-private storage, message-level attachment persistence, image attachment mapping into LLM messages, and OpenAI-compatible multimodal payload support.
- Memory system: realtime consolidation, layered recall, conflict-aware fact governance, ranked lookup, and in-app fact / summary management.

## What Is Intentionally Limited Right Now

- Multimodal support only covers image attachments. PDF, audio, and broader file-type ecosystems are still out of scope.
- Provider-side image support is only wired through the OpenAI-compatible path. Unsupported providers fail explicitly instead of silently dropping attachments.
- MCP remains remote HTTP-oriented only. No local stdio server startup, installation marketplace, or dependency ecosystem is implemented.
- Tool semantics are now cleaner, but `ToolRegistry` still uses cached dynamic tool exposure rather than a richer capability service layer.

## High-Value Non-Blockers Still Open

- MCP production hardening: auth options, timeouts, health-check visibility, retry/backoff, and possibly last-success timestamps for cache freshness display.
- Multimodal hardening: richer provider capability negotiation, optional image preparation diagnostics, and eventual expansion beyond one provider path.
- Runtime polish: more explicit debug surfaces for attachment support, MCP cache freshness, and orchestration visibility.
- Ecosystem work: channels / bridge / CLI remain outside the current Android-native core.

## Recent Stabilization Work

- Settings page moved from direct persistence-flow projection to a baseline/draft editor model with reset/save behavior.
- Tool semantics now distinguish local orchestration from local read-only execution.
- `ToolRegistry` visibility and definition queries were converted to suspend APIs so cached dynamic tool lookups no longer require internal `runBlocking` bridges.
- Memory was upgraded with realtime post-turn refresh, layered recall, conflict-aware fact replacement, ranked lookup, and Memory-screen management actions for facts and summaries.
- High-complexity integration coverage now spans:
  - dynamic MCP tools plus workspace-restricted mode
  - `delegate_task` plus workspace artifact writeback
  - image attachments plus supported / unsupported provider behavior

## Recommended Next Phase

1. Polish documentation and debug wording so project state is self-explanatory without chat history.
2. Add clearer memory provenance / confidence presentation and optional richer correction workflows.
3. Harden MCP further with auth/timeout/health-check/backoff support.
4. Decide whether to stop at the Android-native core or continue toward channels / bridge / CLI parity with the broader Python ecosystem.

# Android Nanobot 开发进度与路线图

## 当前快照

- 项目状态：Android 原生 nanobot 复刻已进入后期阶段，核心 agent loop、后台自动化、本地 skills、subagent、动态 MCP 工具、workspace 工具、web 工具，以及第一阶段的多模态图片支持都已具备稳定基础。
- 技术架构：单模块 Android 应用，使用 Kotlin、Compose、Room、DataStore、Hilt、WorkManager、Retrofit/OkHttp 与 kotlinx.serialization。
- 开源协议：Apache License 2.0（见 `LICENSE`）。
- 本地验证基线：`:app:testDebugUnitTest` 与 `:app:assembleDebug`，当前均可通过。

## 已经具备的能力

- 核心聊天 agent：持久化会话、会话切换、prompt preset、memory context、tool loop、progress event、cancel 支持。
- 工具体系：本地只读工具、workspace 沙箱读写工具、本地编排能力 `delegate_task`、外部 web 工具，以及基于策略层的统一阻断。
- 安全模型：`restrictToWorkspace` 开启后，允许本地只读工具、本地编排工具和 workspace 沙箱读写工具；阻断外部 web 访问、动态 MCP 工具以及非 workspace 副作用工具。
- 后台自动化：heartbeat 决策链、reminder 投递链、循环调度逻辑、notification sink，以及 enqueue/cancel/skip/deliver/fail 路径测试。
- 本地 skills：内置 skill catalog、持久化启用状态、设置页开关、prompt 注入与 runtime 展示。
- Subagent：隔离子会话、摘要回传、artifact path 回传、父会话稳定性与递归保护。
- MCP 动态工具：远程 HTTP JSON-RPC discovery、动态工具缓存暴露、策略接入、设置页管理、局部刷新失败时保留缓存、endpoint 校验。
- 多模态图片基础层：图片导入 app-private storage、消息级附件持久化、附件映射进 LLM message，以及 OpenAI-compatible provider 多模态 payload 支持。

## 当前刻意保持的限制

- 多模态目前只覆盖图片附件，PDF、音频与更广文件类型仍未纳入范围。
- 图片 provider 支持目前只接通 OpenAI-compatible 路径；不支持的 provider 会显式失败，而不是静默丢弃附件。
- MCP 目前仍是远程 HTTP 取向；不支持本地 stdio server 启动、安装市场或依赖生态。
- 工具语义已经整理得更清楚，但系统整体仍以缓存驱动的动态工具暴露为主，而不是更重的 capability service 层。

## 仍然值得继续做的高价值非阻塞项

- MCP 生产级硬化：auth 选项、timeout、health-check 可见性、retry/backoff，以及缓存新鲜度时间戳等。
- 多模态进一步硬化：更丰富的 provider capability negotiation、图片准备过程诊断，以及未来超出单 provider 路径的扩展。
- 运行时打磨：更明确的 attachment support、MCP cache freshness、orchestration visibility 调试面板或元数据展示。
- 生态层工作：channels / bridge / CLI 仍在当前 Android 原生核心之外。

## 最近完成的稳定化工作

- 设置页从“持久化流直接回灌 UI”重构为 baseline/draft 编辑模型，支持 reset/save 且不会覆盖未保存草稿。
- 工具语义层已将 local orchestration 与 local read-only 区分开。
- `ToolRegistry` 的工具可见性与定义查询已改成 suspend API，动态 MCP 工具查询不再依赖内部 `runBlocking` 桥接。
- 高复杂度集成测试已覆盖：
  - dynamic MCP tools + workspace-restricted mode
  - `delegate_task` + workspace artifact writeback
  - image attachments + supported / unsupported provider behavior

## 建议的下一阶段

1. 继续做文档与调试文案整理，让项目状态无需依赖聊天历史也能快速理解。
2. 进一步硬化 MCP，包括 auth / timeout / health-check / backoff。
3. 明确是停留在 Android 原生核心阶段，还是继续推进到 channels / bridge / CLI 与更广 Python 生态对齐。

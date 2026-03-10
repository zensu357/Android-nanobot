# 当前状态

## 开源协议

- 协议：Apache License 2.0（见 `LICENSE`）

## 完成度

- Android 原生 nanobot 核心：功能上已接近完成。
- 与 Python nanobot 的对齐目标：核心 agent 行为已较接近，但生态层仍未完全补齐。

## 稳定能力

- 持久化聊天会话与 memory-aware prompting
- 实时 memory consolidation，并保留 worker 周期兜底
- 分层 memory 召回：session summary、当前会话 facts、长期 facts
- 冲突感知的 fact 替换、排序式 memory lookup，以及内置 fact / summary 管理 UI
- 由策略层强制执行的 workspace-restricted mode
- 通过 `delegate_task` 实现的本地编排能力
- Heartbeat 与 reminder 后台自动化
- 带缓存发现与局部刷新保留能力的动态 MCP 工具
- 存储于本地并能走 OpenAI-compatible provider 路径的图片附件

## 已知非阻塞项

- MCP 仍需更强的生产级硬化，包括 auth、timeout、health-check 和 retry/backoff。
- 动态 MCP 缓存的新鲜度还没有通过时间戳或“上次成功刷新时间”显式展示出来。
- Memory 目前还缺少更明确的置信度 / 来源说明；现在主要依赖 session 关联、时间戳与轻量冲突规则。
- 多模态仍然只覆盖图片，且目前只有一条 provider 路径接通 payload 投递。
- Channels / bridge / CLI 生态能力尚未实现。

## 术语说明

- `restrictToWorkspace`：持久化配置中的原始开关名。
- `workspace-restricted mode`：面向用户和运行时说明时，对该开关开启状态的描述。
- `local orchestration tools`：像 `delegate_task` 这样用于协调本地子运行、但不是简单只读工具的能力。
- `image attachments`：保存在 app-private storage 中、并可在需要时准备为 provider payload 的消息级图片引用。
- `dynamic MCP tools`：从已启用 MCP server 发现并缓存后，再通过标准 tool registry 暴露的工具定义。

## 下一阶段方向

1. 继续硬化 MCP。
2. 补充更清晰的 memory 来源说明 / 置信度展示，以及可选的更丰富纠正流程。
3. 扩展 multimodal 的 provider 覆盖与诊断信息。
4. 做最终一轮 UX / 文档打磨，再决定是否扩展生态层。

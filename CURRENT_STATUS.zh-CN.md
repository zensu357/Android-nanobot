# 当前状态

## 开源协议

- 协议：Apache License 2.0（见 `LICENSE`）

## 完成度

- Android 原生 nanobot 核心：功能上已接近完成。
- 与 Python nanobot 的对齐目标：核心 agent 行为已较接近，但生态层仍未完全补齐。

## 稳定能力

- 持久化聊天会话与 memory-aware prompting
- 由策略层强制执行的 workspace-restricted mode
- 通过 `delegate_task` 实现的本地编排能力
- Heartbeat 与 reminder 后台自动化
- 带缓存发现与局部刷新保留能力的动态 MCP 工具
- 存储于本地并能走 OpenAI-compatible provider 路径的图片附件

## 已知非阻塞项

- MCP 仍需更强的生产级硬化，包括 auth、timeout、health-check 和 retry/backoff。
- 动态 MCP 缓存的新鲜度还没有通过时间戳或“上次成功刷新时间”显式展示出来。
- 多模态目前仍然是 image-only，且只有一条 provider payload 路径已接通。
- Channels / bridge / CLI 生态能力尚未实现。

## 术语说明

- `restrictToWorkspace`：持久化配置中的原始开关名。
- `workspace-restricted mode`：面向用户和运行时说明时，对该开关开启状态的描述。
- `local orchestration tools`：像 `delegate_task` 这样用于协调本地子运行、但不是简单只读工具的能力。
- `image attachments`：保存在 app-private storage 中、并可在需要时准备为 provider payload 的消息级图片引用。
- `dynamic MCP tools`：从已启用 MCP server 发现并缓存后，再通过标准 tool registry 暴露的工具定义。

## 下一阶段方向

1. 继续硬化 MCP。
2. 扩展 multimodal 的 provider 覆盖与诊断信息。
3. 在继续扩生态层前，完成最终的 UX / 文档收口整理。

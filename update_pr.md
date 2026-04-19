增加了更多的 AI 工具调用和 Prompt 强化，使其更像一个能够自主探索和调用工具的 Agent。

| 文件 | 变更 |
|------|---------|
| src/main/java/baritone/process/AIProcess.java | - 将提示词更新为包含详细可用工具（Actions）列表的 Agent 设定<br>- 增加了 `get_player_info` 工具动作，用于获取玩家位置、血量和饥饿值<br>- 增加了 `get_env_info` 工具动作，用于获取环境的时间和天气<br>- 增加了 `execute_command` 工具动作，允许 AI 自主执行更多的 Baritone 指令（如 explore 等） |

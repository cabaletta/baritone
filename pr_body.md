此次合并修复了与 `AIProcess` 相关的多项编译错误，确保它正确实现必需的接口方法，并通过 `BaritoneAPI` 或 `BaritoneProcessHelper` 访问设置及相关功能。同时修正了 `AICommand` 中的参数解析错误，增强了代码的整体兼容性和稳定性。

| 文件 | 变更 |
|------|---------|
| src/main/java/baritone/Baritone.java | - 添加了 `getAIProcess()` 方法的实现，返回 `aiProcess` 实例 |
| src/main/java/baritone/command/defaults/AICommand.java | - 移除了 `IArgParserManager` 的引入和使用<br>- 在 `execute` 方法中抛出 `CommandNotEnoughArgumentsException` 异常替代普通的 `CommandException`<br>- 调整 `tabComplete` 和 `execute` 方法的参数签名，移除多余的解析器管理器 |
| src/main/java/baritone/process/AIProcess.java | - 引入 `BaritoneProcessHelper` 和 `BaritoneAPI`<br>- 修改类签名继承 `BaritoneProcessHelper` 并实现 `AbstractGameEventListener`<br>- 将设置和组件访问（如 `aiApiKey`、`aiBaseUrl`、`aiModel` 等）切换为使用 `BaritoneAPI`<br>- 使用 `logDirect` 替代原有的日志打印方法 |

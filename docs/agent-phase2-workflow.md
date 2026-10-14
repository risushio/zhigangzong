# Agent V1 第二阶段：可替换 LLM 接口

更新日期：2026-10-13。用户通过“继续完成 agent 开发”确认进入第二阶段；随后选择先验收接口、本地真实联调与云端 HTTP Mock，真实 DeepSeek 待服务端配置提供后单独验收。

## 范围与配置

`LlmClient.chat(LlmRequest)` 使用独立 DTO，返回内容、模型名称、结束原因、可选 token 用量与结构化工具调用；模型接入层不依赖学生实体、Service、Mapper 或 SQL。`OpenAiCompatibleLlmClient` 是唯一 HTTP 实现，现有 `HttpModelClient` 只做旧 `ModelClient` 与新接口之间的转换，避免两套网络代码。

| 设置 | Ollama Profile | DeepSeek Profile |
| --- | --- | --- |
| Spring Profile | `ollama` | `deepseek` |
| 默认启用 | 关闭；显式 `LLM_ENABLED=true` | 关闭；显式 `LLM_ENABLED=true` |
| 模型覆盖 | `OLLAMA_MODEL`，默认 `qwen2.5:3b` | `DEEPSEEK_MODEL`，默认 `deepseek-flash` |
| 完整接口地址覆盖 | `OLLAMA_ENDPOINT`，默认 `http://127.0.0.1:11434/v1/chat/completions` | `DEEPSEEK_ENDPOINT`，默认 `https://api.deepseek.com/chat/completions` |
| 密钥 | 本地协议占位值 `ollama`，非云端凭据 | `DEEPSEEK_API_KEY`，无内置密钥 |
| 连接 / 单次 / 总期限 | 5 / 120 / 150 秒 | 10 / 120 / 150 秒 |
| 输出 token 参数 | `max_tokens=2000` | `max_tokens=2000` |
| JSON 模式 | 开启能力标记；仅请求 JSON 时发送 | 同左 |
| 工具能力标记 | 默认关闭，未验收本模型的工具可靠性 | 开启；本轮通过 HTTP Mock |
| 严格工具 / 并行参数 | 默认均不发送 | 默认均不发送 |
| 思考参数 | 不发送 | `reasoning_effort=none` |

Ollama 兼容接口只支持协议子集，本地服务忽略该占位 key，见 [Ollama 官方兼容说明](https://docs.ollama.com/api/openai-compatibility)。DeepSeek 默认模型名和思考参数按 [官方 Chat Completions 文档](https://api-docs.deepseek.com/api/create-chat-completion/) 核对；JSON 模式要求提示词明确要求 JSON。协议声明与 Mock 验证不能替代真实云端测试。

`application-ollama.yml`、`application-deepseek.yml` 绑定 `app.llm`。环境变量也可通过 Spring 标准的 `APP_LLM_*` 覆盖单项配置。只选择一个模型 Profile，切换无需修改学生业务代码。新增协议时实现 `LlmClient` 并在 `LlmConfiguration` 注册适配器。

没有任何 `app.llm` 配置时，整体复用既有 `app.agent` 的模型配置。出现新配置时整体采用新配置，不从旧配置补密钥；新配置显式关闭也不会被旧开关开启。旧 Agent 界面的 `app.agent.model-enabled` 开关仍保留；第二阶段只验收独立接口，聊天字段提取和追问集成属于第三阶段。

已构建 JAR 的 Profile 启动示例（使用本轮已初始化的隔离库，不自动建表）：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'ollama' # 可换成 deepseek 后重启
$env:LLM_ENABLED = 'false' # 开启独立接口需模型服务/密钥已就绪
java -jar target/zhigangzong-1.2.0.jar '--server.port=18085' '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/zhigangzong_agent_phase1_test_20261013_llm_f3e205?createDatabaseIfNotExist=false' '--spring.sql.init.mode=never' '--app.bootstrap.enabled=false' '--app.agent.model-enabled=false'
```

## 调用示例

```java
var request = LlmRequest.builder()
    .messages(List.of(
        LlmMessage.system("只返回 JSON 对象；字符串编号保留前导零，缺失信息不得编造。"),
        LlmMessage.user("虚构样例：编号000123，年份2026。字段code为字符串，year为整数。")))
    .responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT)
    .temperature(0.0)
    .timeout(Duration.ofSeconds(90))
    .build();
LlmResponse response = llmClient.chat(request);
JsonNode object = LlmJson.object(response.content());
```

`LlmRequest` 可指定模型、消息角色、temperature、JSON 输出、工具定义与调用策略。`LlmMessage` 能保存 assistant 的工具调用和后续 TOOL 结果；允许 `content:null` 的工具调用消息。接口只返回工具意图，不执行任何工具。

`LlmException` 区分禁用、配置错误、不支持能力、请求错误、连接、认证、限流、超时、模型不存在、服务故障、空回复、非法响应、超大响应及中断；保留安全状态码和可重试标记。旧桥接层映射为稳定的 `MODEL_*` 业务错误。

## 本地验证

在应用根目录执行。Windows 安装与服务配置参考 [Ollama 官方 Windows 说明](https://docs.ollama.com/windows)。以下命令只针对本地服务和固定虚构输入，不创建学生档案。

```powershell
ollama pull qwen2.5:3b
$env:RUN_LLM_LIVE_TESTS = 'true'
.\scripts\maven.ps1 '-Dtest=LlmProviderIntegrationTest' '-Dlive.llm.profile=ollama' verify
```

服务未启动时，可在另一个终端执行 `ollama serve`。默认端口为 11434；自定义端口可通过 `-Dlive.llm.endpoint=http://127.0.0.1:11436/v1/chat/completions` 指定完整地址。日常默认不运行真实模型测试，需要显式启用 `RUN_LLM_LIVE_TESTS`，测试报告会如实显示条件跳过。

本轮使用已安装的 Ollama 0.13.1，测试服务在 11436，模型为 `qwen2.5:3b`。进程级 `OLLAMA_MODELS` 指向被忽略的 `.local/llm-phase2/models`，未修改系统级设置。首次加载模型约 43 秒；实际接口返回的 JSON 保留 `code="000123"`，`year=2026` 为整数。

## DeepSeek 后续真实验收入口

由操作者在本机服务端配置或进程环境安全设置 `DEEPSEEK_API_KEY`，不要将值写进源码、提交记录、命令历史或聊天。已配置后才手动执行：

```powershell
$env:RUN_LLM_LIVE_TESTS = 'true'
.\scripts\maven.ps1 '-Dtest=LlmProviderIntegrationTest' '-Dlive.llm.profile=deepseek' verify
```

该命令会调用真实云端并可能产生 API 费用。当前未执行，不能标记为真实 DeepSeek 通过。开启 Profile 但保持 `LLM_ENABLED=false` 时允许无密钥启动；显式启用且未配置密钥时启动失败，报安全配置错误。

## 请求边界

只允许服务端 HTTPS 地址或本机 HTTP；禁止 URL 内凭据、片段和自动重定向。请求、响应默认各限制 128 KiB。请求期限只能缩短配置总期限，每次请求与重试共享总预算；超时和中断取消异步请求，中断标记保留。

默认最多重试一次，仅连接失败、超时、429 和 5xx 可重试；遵守 `Retry-After` 和最大等待时间。认证、模型不存在、参数、解析和超大响应错误不重试。适配器不会发送未经配置支持的 strict、parallel 或 token 参数；必要能力不支持时在请求发出前明确失败。

响应只解析受支持的字段，丢弃未使用的服务商字段。JSON 禁止重复键、尾随内容、非对象结果及未完整结束的 JSON 输出；工具参数也必须是 JSON 对象，工具名必须来自请求白名单。不记录 key、Authorization、提示词、回复正文或工具参数；DTO 的 `toString()` 隐去内容，异常不携带服务商正文或底层原始异常。

## 测试证据

2026-10-13 最终验收通过：

- 完整 Maven/MySQL verify：135 项测试，0 失败、0 错误、0 跳过，候选 JAR 构建成功。使用已批准的 `zhigangzong_agent_phase1_test_20261012_b7a640`。
- 新增 38 项测试：28 项协议/异常/期限/取消/大小限制测试、5 项配置测试、4 项 Profile HTTP Mock 测试和 1 项真实模型测试。旧桥接与原学生业务测试继续通过。
- 本地真实模型测试独立执行一次，并在完整回归中再次执行：同一 `LlmClient` 使用 Ollama 0.13.1 + `qwen2.5:3b` 返回有效 JSON；字符串前导零和整数类型正确。
- DeepSeek Profile 的本地 HTTP Mock 验证认证、模型名、JSON、max_tokens、reasoning_effort、工具调用及省略未支持参数；401/403、404、429、5xx、非法响应、空回复、超时和重试在公共适配器测试中验证。真实云端未执行。
- 前端语法检查和 15 项测试通过，0 失败、0 跳过。
- 候选 JAR 使用新隔离库 `zhigangzong_agent_phase1_test_20261013_llm_f3e205`，禁用 SQL 初始化。实际日志确认先在 Ollama Profile 启动，通过 REST、桌面/移动手动建档、丢失响应后的幂等重试、29 项 smoke 与登录注销，再切到 DeepSeek Profile 重启并读回。全程模型开关关闭，未新增学生账号。
- 独立只读 JDBC 确认测试学生确实提交，学号前导零、年级和班级一致，user_id 为 NULL，对应草稿为 CREATED，学校只有测试管理员账号。该库保留 2 条虚构学生作验收依据，未写入真实业务库。
- 业务库最终只读核查仍为原结构、2 条学生和 7 条用户，未执行迁移或新增 Agent 表。

新增适配器与 Profile Mock 测试不启动数据库，不需要真实云端密钥；测试服务只监听随机本地端口。第三阶段尚未开始，须先报告结果并取得用户确认。

最终全量命令：

```powershell
$env:RUN_LLM_LIVE_TESTS = 'true'
.\scripts\test-mysql.ps1 '-Dmaven.repo.local=C:\Users\yc200\.m2\repository' '-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/zhigangzong_agent_phase1_test_20261012_b7a640?createDatabaseIfNotExist=true' '-Dlive.llm.profile=ollama' '-Dlive.llm.endpoint=http://127.0.0.1:11436/v1/chat/completions'
```

运行检查复用 `scripts/test-student-drafts-api.mjs`，新增可选 `STUDENT_DRAFT_TEST_OUTPUT_DIR`（必须在 `.local/` 内）与 `STUDENT_DRAFT_TEST_RESTART_PROFILE`（只允许 ollama/deepseek），原默认调用不变。该脚本要求新隔离库，不能重复用已保存随机测试管理员账号的库执行初始化登录。

日志位于被忽略的 `.local/llm-phase2/`，包括 `maven-verify.log`、`live-ollama.log`、`mysql-verify.log`、`runtime/result.json`、`runtime/backend.log`、`runtime/smoke.log`、`runtime/draft-desktop.png`、`runtime/created-mobile.png` 与 `independent-jdbc.txt`。首轮纯代码验证有 47 项通过、82 项条件跳过，未把跳过计为通过；最终报告使用明确启用后的 135 项零跳过结果。临时测试服务在验收后关闭，模型缓存仍保存在忽略目录。

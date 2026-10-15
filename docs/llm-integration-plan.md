# Agent V1 分阶段实施计划（数据库优先）

更新日期：2026-10-14。本计划按用户最新指令调整为数据库优先、业务优先、最后接入 LLM，取代此前模型层先行的顺序。

2026-10-14：提取器与 DTO 已补齐，当前后端构建和 240 项测试通过（无失败、无跳过），包含 25 条本地 Qwen 真实提取样例及 DeepSeek HTTP Mock；前端语法检查和 15 项测试通过。按用户要求停止继续调整模型输入。最新修复后的实际聊天与页面完整流程未重跑，不登记第三阶段完整验收通过；真实 DeepSeek 仍待配置。本轮实现纳入 codex/agent-v1-wip 分支；业务库未升级。

每阶段验收通过后先报告并等待用户确认，才能进入下一阶段；核心错误必须修复并重验。任何数据库结构变更先检查目标表、给出迁移 SQL 和兼容性分析，经用户确认后执行。

## 第一阶段：MySQL 与学生档案业务

状态：已在用户批准的新隔离库验收通过；用户通过“继续完成 agent 开发”确认继续第二阶段。详细实现、文件范围、接口与测试见 [第一阶段验收](agent-phase1-workflow.md)，实际库检查和迁移见 [数据库审查](agent-phase1-database-review.md)。

- 复用 Spring Boot 3.5.8 / Java 17 / MyBatis 3.0.5 / MySQL 8.0.45，以及既有 student_profile；没有另建学生表。
- 检查真实业务库后发现旧表尚未应用此前 Agent 扩展，因此将旧结构迁移与新环境建表分开。用户只批准隔离库执行，本机业务库未升级。
- 增加可空 grade_year、class_name，保留历史数据；不建立班级字典或 class_id 外键。学校内学号唯一索引和 Java 查重共同保护。
- StudentDraftService 复用 AgentStore 的 MySQL 草稿、会话、字段来源、版本、权限和幂等存储；状态为 COLLECTING、READY、CREATED、CANCELLED、EXPIRED，兼容旧状态。
- 明确确认后通过 StudentService 创建独立学生档案；必需事务、管理员权限、学号/学院检查与审计，禁止创建学生用户或登录账号。
- 不依赖模型的普通 REST API 完成创建草稿 → 多次补充/清空 → 校验 → 确认 → MySQL → 查询档案；候选 JAR 的手动页面和重启持久化也已验证。
- 验证：97 项全量后端测试、15 项前端测试无失败/无跳过；迁移夹具、真实 REST、权限、重复学号/确认、并发及事务回滚通过；候选运行、29 项 smoke 与登录注销、桌面/移动表单及独立 JDBC 查询通过。

测试学生保留在新的隔离库。业务库原 2 条学生、7 条用户和旧结构未变；未经单独确认，不把第一阶段的新构建启用到业务库。

## 第二阶段：可替换 LLM 接口

状态：已按本轮约定范围完成并验证，等待用户确认第三阶段。135 项完整后端回归、15 项前端测试、本地 Qwen 真实调用、两种 Profile 的候选启动/无模型建档/smoke/重启与独立 JDBC 核查通过，见 [第二阶段验收](agent-phase2-workflow.md)。用户明确选择真实 DeepSeek 待配置提供后另行验收，本轮未调用云端。

| 范围 | 实现内容 | 验证与主要风险 |
| --- | --- | --- |
| 统一接口 | 在 agent/llm/ 中定义 LlmClient、LlmRequest、LlmResponse、LlmMessage、LlmToolDefinition、LlmToolCall、LlmException | 明确角色、可选模型/temperature、responseFormat、tools/toolChoice、工具结果、finishReason/model/usage；兼容缺失 usage |
| 配置 | agent/config/LlmProperties、LlmConfiguration；app.llm；application-ollama.yml、application-deepseek.yml | Spring Profile/环境覆盖、能力与新旧配置边界、密钥不硬编码或泄漏 |
| HTTP | agent/llm/adapter/OpenAiCompatibleLlmClient；复用现有 Java HttpClient 的期限、响应上限和取消能力 | HTTP Mock：正常/空/非法响应、401/404/429/5xx、超时、工具调用、有限重试；不默认发送未支持的参数 |
| 旧接口兼容 | 现有 ModelClient/HttpModelClient 作为映射桥接，HTTP 只实现一次 | 保留已有工具调用协议与测试替身；不复制 Agent 架构 |
| 本地/云端 | Ollama + 本地 Qwen、DeepSeek；仅配置切换，同一客户端接口 | 真实调用显式启用、仅用虚构样例；缺少部署/凭据时不得把 Mock 当作真实联调 |
| 文档 | 配置、Windows 安装、模型下载、启动、错误处理与新 Adapter 扩展说明 | 命令与当前项目一致，明确实际支持能力与未经验证项 |

业务层不依赖供应商、地址、模型名或协议 DTO；更换模型不修改 StudentService、Mapper 或数据库。此阶段不集成聊天建档，先独立证明统一模型接口可用，再报告并等待用户确认。

现有 AgentProperties 的模型期限与新配置的读取期限必须统一到总任务预算；必要 JSON/工具能力不支持时明确报错，不在适配器静默删除能力或绕过确认。模型异常不包含完整 Prompt、学生敏感资料、Authorization 或 API Key。

供应商文档于 2026-10-13 再次核对：Ollama 只兼容部分 OpenAI 协议；DeepSeek 当前官方模型名称与原任务书 deepseek-chat 示例不同，配置默认 deepseek-flash 并允许覆盖。参考 [Ollama 兼容协议](https://docs.ollama.com/api/openai-compatibility)、[DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)、[DeepSeek JSON 输出](https://api-docs.deepseek.com/guides/json_mode/)。本地 JSON 已真实验证；DeepSeek 配置和工具协议只经过 HTTP Mock，真实云端尚未执行。

## 第三阶段：Agent 与数据库业务集成

状态：用户已确认开始，现按用户要求暂停。已有部分编排、草稿合并、字典解析、请求期限和测试脚本；StudentTextExtractor 及相关 DTO 尚未实现，当前构建与第三阶段完整验收未完成。

- 在 agent/extraction/ 中新增 StudentFields、StudentFieldPatch、StudentExtractionResult、StudentTextExtractor；模型只做自然语言结构化提取。
- 统一 DTO 的 majorName 映射既有 major；学号/手机号为字符串、年级为整数、班级为文本。不猜缺失数据或字典 ID。
- 明确区分未提及、SET 和 CLEAR；DTO 中的 null 不能直接覆盖现有草稿。
- AgentOrchestrator 通过 StudentDraftService 保存/补充草稿；Java 校验、查重及状态判断使用第一阶段的既有业务。
- 资料缺失时追问，完整时展示包含年级/班级的草稿确认卡片；只有管理员确认接口才能调用 StudentService，返回数据库真实结果。
- 复用现有 AgentToolRegistry 和 AgentStore 的白名单、参数/权限校验、调用限制、事务和去重；LLM 不能执行 SQL或通过工具直接创建正式学生。
- 验证至少 20 条虚构中文指令的提取/多轮意图、错误输出零误写、两模型同入口结果、完整 MySQL/权限/并发回归及真实页面确认。

完成后报告实际文件变更、测试数量、失败/跳过及真实联调范围；未经执行的检查不登记完成。

## 当前下一步

按用户要求停止进一步模型输入调整，保留当前实现。后端与本地提取测试已通过；后续完整验收仍需复验最新候选的实际聊天与页面确认流程。真实 DeepSeek 待配置提供后另验，不执行版本发布或业务库升级。

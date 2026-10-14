# AI 学生档案 Agent V1

> 2026-10-14：第三阶段已获用户确认开始，随后按用户要求暂停。当前 WIP 缺少提取器/DTO，未通过构建与第三阶段验收；下列阶段结果是此前代码快照的记录，最新状态见 [开发交接](NEXT-STEPS.md)。

> 2026-10-13 最新：按数据库与业务 → LLM 接口 → Agent 集成的顺序执行，第一阶段已通过无模型隔离库验收，见 [第一阶段说明](agent-phase1-workflow.md)；第二阶段统一模型接口、本地 Qwen 真实 JSON 与 DeepSeek HTTP Mock 已验证，见 [第二阶段验收](agent-phase2-workflow.md)。第三阶段等待用户确认，真实 DeepSeek 待配置提供后另验；业务库尚未升级，下文模型替身验收为此前已有 Agent 的记录。

## 仓库检查与映射（2026-10-12）

- 应用：Java 17 / Spring Boot 3.5.8 / MyBatis 3.0.5 / MySQL，前端位于 `src/main/resources/static/js/`，沿用 Session、CSRF、`ApiResponse` 和 `BusinessException`。
- 学生链路：`StudentController` → `StudentServiceImpl` → `StudentProfileMapper` → `student_profile`。原路径只关联已有 STUDENT 用户资料；登录凭据另存于 `auth_account`。
- 原规则：学号（40 字）和专业（100 字）必填，日期范围及每周天数校验；Excel 导入已有本校学号查重。没有软删除、班级表或班级字典。
- V1 映射：姓名、学号、专业必填；学院为本校可选字典，手机号为可选字段。复用学生表，增加独立学校/姓名/学院/手机号字段并允许 `user_id` 为空；最新新增可空年级 `grade_year`、文本班级 `class_name`，不建立班级字典或 class_id。Agent 不写 `user_account`、`auth_account`，不调用账号服务。
- 学号唯一范围为学校；迁移前检查历史重复，发现重复则停止，不删除或合并历史数据。
- 权限使用真实的 SCHOOL_ADMIN 角色，数据库每次重新读取已启用登录账号和角色；会话和草稿仅创建者可访问，学院必须属于本校。
- 修改范围：学生实体/Mapper/兼容迁移；新增 `agent/` 业务、模型和工具模块及 Agent 表；新增现有管理界面的聊天与草稿页面；新增模型替身、权限、事务及 MySQL 并发测试。
- 基线：前端语法与 12 项契约测试通过；后端普通 verify 通过（66 项，58 项 MySQL 测试条件跳过）。数据库检查另行显式启用，条件跳过不算业务验收。

## 使用

学校管理员登录后，从左侧“学生建档 → AI 学生档案助手”进入 `/#/student-agent`。

1. 输入一名学生资料；缺少信息时继续补充，也可使用右侧表单建草稿。
2. 学院名称有重名时，按授权候选回复“学院编号 123”；模型不能自行猜选。原项目没有班级管理，不把班级名称作为学院落库。
3. 核对草稿的全部字段和版本，可修正或清空字段；缺少必填、重复学号、非法日期/手机号时禁止提交。
4. 点击“确认创建学生档案”。聊天里的“确认创建”“好的”不提交。
5. 网络失败后显示“结果待确认”，查询或重试沿用原幂等键与版本；版本冲突会刷新草稿，必须重新核对。

会话 ID、待处理消息 ID 与确认键暂存于浏览器 sessionStorage，按操作者区分；不保存密码或模型密钥。模型回复不能构造成功卡片，成功仅来自确认接口。

独立档案没有用户或登录账号，暂不能使用需要用户身份的学生门户、投递和实习过程功能。原人工录入和账号开通路径仍保留；原 Excel 格式只导出关联用户的学生，学号查重会包含独立档案。账号关联属于后续业务，不在本次自动创建。

新独立档案在已有 `/students` 列表、详情及 `/catalog/students` 入口也限制为已启用的本校学校管理员。历史关联用户的业务接口仍沿用项目已有权限约定，本次不宣称完成整个系统的学校隔离；部署继续遵循项目单校试点边界。

## 接口与目录

所有接口沿用 `/api/agent`、Session、CSRF、`ApiResponse`；所有写操作及工具/建档服务重新读取数据库中的已启用 SCHOOL_ADMIN 身份。

| 接口 | 请求及行为 |
| --- | --- |
| GET `/settings` | 功能状态、输入上限；不返回密钥或模型地址 |
| POST `/sessions` | 创建本校本人会话 |
| GET `/sessions/{id}` | 当前草稿、消息处理状态 |
| POST `/sessions/{id}/messages` | `clientMessageId, content`；保存并稳定重放处理结果 |
| GET `/sessions/{id}/messages?page=1&size=20` | 分页历史；过期资料显示清理提示 |
| POST `/sessions/{id}/drafts` | `fields`，用于手动建立草稿 |
| GET `/drafts/{id}` | 后端字段、字典名称、状态、版本与校验 |
| PATCH `/drafts/{id}` | `expectedVersion, fields`；省略不覆盖、null 清空 |
| POST `/drafts/{id}/cancel` | `expectedVersion` |
| POST `/drafts/{id}/confirm` | `expectedVersion`，请求头 `Idempotency-Key`；不接受替换字段 |

资源、版本、重复及幂等冲突分别返回对应业务码。模型失败保存在消息结果的 `processingStatus=FAILED` 和 `code`，仍可查询同一消息及已有草稿；相同 clientMessageId 不会偷偷重新运行。发送新消息才开始新的编排。

- `src/main/java/com/zhigangzong/agent/`：AgentController、StudentDraftService、AgentStore、StudentProfileCreationService（委托 StudentService）、StudentDraftRules/Status、AgentOrchestrator、ModelClient/HttpModelClient、AgentToolRegistry、AgentMapper、配置及清理任务。新草稿状态为 COLLECTING/READY/CREATED/CANCELLED/EXPIRED，兼容旧 EDITING/COMMITTED。
- `src/main/resources/db/agent-tables.sql`：会话、消息、草稿、最终动作及工具去重表。
- `src/main/resources/db/migrations/20261012-agent-profile.sql`：原学生表兼容升级与历史回填。
- `src/main/resources/static/js/agent.js`：聊天、手动修正和明确确认页面。
- `src/test/java/com/zhigangzong/Agent*Test.java`、`agent/*Test.java`、`frontend-tests/agent.test.mjs`：模型替身、MySQL、传输与前端测试。

## 模型与约束

适配 Chat Completions 的 `tools` 协议，设置 `parallel_tool_calls=false`，只开放 `search_departments`、`check_student_duplicate`、`upsert_student_draft`、`get_student_draft`。没有创建学生、账号、SQL 或外部请求工具。

工具 Schema 允许省略未修改字段（`strict=false`），服务端独立严格校验未知属性、类型、长度、归属和学院选择来源；不会把模型参数当作身份、学校或授权。HTTP 响应限制 128 KiB，非法 JSON、超时、未知工具会保留已存草稿并结束本次处理。工具写入与去重同事务；崩溃过期消息转为 FAILED，不重新应用原字段。

旧 Agent 工具协议依据 [OpenAI 官方工具调用文档](https://developers.openai.com/api/docs/guides/function-calling)。所选供应商/模型必须支持上述 Chat Completions 工具协议；新配置的能力标记和参数规则见 [第二阶段说明](agent-phase2-workflow.md)。需要其他协议的模型须实现 LlmClient 并在 LlmConfiguration 注册适配器，不能仅替换名称。Ollama Profile 当前未开启工具能力，第三阶段将使用独立字段提取入口。

模型只看到当前输入、当前草稿和本校少量候选，不传认证令牌、密码或全量学生库。提交事务采用 READ_COMMITTED，加学校、会话及草稿锁，授权和幂等结果使用当前读取；模型网络访问位于事务之外。学校锁是 V1 的保守串行化选择，高并发部署前需评估容量。

## 启用配置

默认关闭功能和模型，不自动发送个人资料。环境变量：

| 变量 | 默认 / 用途 |
| --- | --- |
| `AGENT_ENABLED` | false；完成数据库升级后开启 |
| `AGENT_MODEL_ENABLED` | false；模型未开启时可手动建草稿 |
| `AGENT_MODEL_ENDPOINT` | `https://api.openai.com/v1/chat/completions`；只允许 HTTPS 或本机 HTTP 测试地址，不跟随跳转 |
| `AGENT_MODEL` | 空；填入实际可用且支持工具调用的模型 |
| `AGENT_MODEL_API_KEY` | 空；仅服务端环境或安全配置注入 |

`app.agent` 还可配置 `max-tool-calls=5`、`max-input-length=4000`、`max-context-length=16000`、`request-timeout-seconds=45`、`model-timeout-seconds=12`、`tool-timeout-seconds=5`、`requests-per-minute=12`、`draft-ttl-hours=24`、`retention-days=7`、`cleanup-enabled=true`。这些值有服务端上下界校验。

消息原文和响应按最近更新时间保留 7 天；草稿字段在有效期结束 7 天后清空。每小时清理一次，启动后延迟 1 分钟；草稿过期由服务端时间即时判定。审计、提交幂等凭证及学生档案继续保留，成功审计同事务，拒绝/失败记录进入独立安全日志。日志仅包含操作者/学校 ID、工具名、耗时、结果码和 traceId，不记录完整资料、手机号、密钥或隐藏推理。

## 数据库升级与回退

1. 备份数据库，在隔离测试库演练。使用迁移脚本前置查询检查“学校 + TRIM(学号)”重复；结果非空立即停止，由业务人员确定处理方案，不自动删历史数据。
2. 一次执行 `20261012-agent-profile.sql`：允许 user_id 为空，增加学校/姓名/学院/手机号，回填旧关联用户资料，再建立学校内学号唯一约束与外键。
3. 已完成原独立建档迁移的旧库执行新增 `20261012-student-grade-class.sql`，然后执行 `agent-tables.sql`。现有库不能靠 `CREATE TABLE IF NOT EXISTS` 自动升级学生表；正常应用启动不会自动执行迁移脚本。测试入口会显式初始化 Agent 表。执行前必须检查字段及取得用户确认，新环境建表已包含两列，不再执行 ADD COLUMN。
4. 先部署和验证关闭模型的手动草稿及确认；第一阶段报告并经用户确认后才开发第二阶段统一模型接口。第二阶段验收并经确认后，再进行聊天集成和真实供应商联调。
5. 回退时关闭 `AGENT_ENABLED` 和模型开关，保留扩展字段、新表、幂等记录及已创建的学生。不要删除学生、改回 user_id NOT NULL 或直接回退到不兼容的旧二进制；MySQL DDL 不能整体事务回滚。

本次没有执行业务库/生产升级、发布、tag 或推送；已有本地服务不因此自动升级。真实供应商凭据及项目对外发送个人信息的要求由部署环境提供。

## 验证

前端：`npm run check`、`npm test`。

后端与数据库使用隔离库，测试脚本支持转发 Maven 参数，避免修改 `.local` 数据库配置：

```powershell
.\scripts\test-mysql.ps1 '-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/zhigangzong_agent_test_20261012?createDatabaseIfNotExist=true'
```

不要把带 `&` 的 JDBC URL 直接转发给 Windows mvn.cmd；此测试连接仅需 createDatabaseIfNotExist。凭据沿用本地安全配置，不写入命令或报告。

浏览器验收脚本 `scripts/test-agent-ui.mjs` 使用新建的独立测试库、随机临时管理员密码、本机模型替身和现有 Edge。配置 `AGENT_TEST_JDBC_URL`（必须符合脚本的隔离库命名限制）、`JAVA_EXECUTABLE`、`PLAYWRIGHT_MODULE_PATH` 后运行：

```powershell
node .\scripts\test-agent-ui.mjs
```

每次选择新的 `zhigangzong_agent_ui_test_...` 库名；脚本不重置已有登录账号。测试通过后结束自己启动的后端/模型/浏览器，资料仅留在隔离库；截图与验收结果保存在被 Git 忽略的 `.local/agent-ui/`。

2026-10-12 最终验证：

- `npm run check`、`npm test`：JavaScript 语法检查通过，14 项测试通过，无跳过。
- 上述隔离 MySQL verify：89 项测试通过，0 失败、0 错误、0 跳过；构建成功。覆盖原 66 项回归及新 Agent 的模型协议、非法输出、超时、字段规则、字典消歧、限流、消息/工具去重、授权、跨学校旧入口访问、迁移回填、唯一约束、并发及真实事务回滚。
- `node scripts/test-agent-ui.mjs`：最终 JAR 在新隔离库及 18080 端口启动；Edge 中完成文字录入 → 补专业 → 修正姓名 → v3 明确确认 → 丢失成功响应 → 原幂等键重试 → 同一学生 ID。用户资料总数不变、档案 user_id 为空、页面错误为 0，桌面与 390px 移动布局无横向溢出。
- 同一候选进程上执行 `scripts/smoke.ps1`：29 个 GET 检查及认证 Session/CSRF/注销通过。
- 截图：`.local/agent-ui/draft-desktop.png`、`success-mobile.png`；浏览器结果：`.local/agent-ui/result.json`；测试和 smoke 日志保存在 `.local/`，不提交敏感本地配置或日志。

以上 2026-10-12 的旧 Agent 模型替身验收未调用真实供应商。2026-10-13 第二阶段已通过本地 Ollama + Qwen 的独立 JSON 接口验收，但聊天字段提取质量属于第三阶段，DeepSeek 真实网络及配额联调仍待配置后验收。本次没有执行业务库升级或生产发布；启用前必须完成前述迁移和配置。模型替身/HTTP 协议测试不能代替真实模型联调。

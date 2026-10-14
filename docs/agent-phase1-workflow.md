# Agent V1 第一阶段：无模型学生档案业务验收

验收日期：2026-10-12。第一阶段已在用户批准的新隔离 MySQL 库通过验收。第二、第三阶段等待用户确认；本次未开发 Ollama、DeepSeek 或统一 LLM 接口。

## 数据库检查与执行范围

实际项目为 Java 17 / Spring Boot 3.5.8 / MyBatis 3.0.5 / MySQL 8.0.45，复用 `student_profile`。业务库检查和迁移 SQL、历史兼容性见 [数据库审查方案](agent-phase1-database-review.md)。

- 本机 `zhigangzong` 仍为旧学生结构，2 条历史学生、7 条用户；没有执行结构升级或测试写入。验收后只读复查数量与字段/索引结构相同。
- 用户只批准新隔离库执行。全量测试库：`zhigangzong_agent_phase1_test_20261012_b7a640`；最终运行验收库：`zhigangzong_agent_phase1_test_20261012_d68e03`。
- 隔离库复用最终 `schema.sql` 和已有 Agent 表定义；独立的旧结构夹具依次执行原独立建档迁移与新增年级/班级迁移。新环境学生表直接含两列，不重复执行 ADD COLUMN。
- 新增 `grade_year INT NULL`、`class_name VARCHAR(100) NULL`；不建立班级字典或 class_id。旧档案的两字段为 null，保留 ID、用户关联、学号字符串及其他资料；不推算年级或班级。
- 学号唯一范围为学校，Java 查重加 `uk_student_school_no(school_id, student_no)` 数据库兜底；学号仍为字符串，前导零不变。

最终运行库保留 2 条虚构测试学生：REST 档案 ID 1、页面档案 ID 2。只有测试初始化管理员的用户/登录账号，没有为学生新增账号；测试数据与业务库隔离，测试进程已退出。

## 业务实现与复用

调用链：普通 REST → AgentController → StudentDraftService → 原有 AgentStore/MyBatis 草稿存储；明确确认 → StudentProfileCreationService → StudentService.createStandaloneStudentProfile → StudentProfileMapper → MySQL。

- StudentDraftService 为草稿创建、查询、更新、取消、确认的业务入口，复用现有 AgentStore，不另建草稿表或复制事务逻辑。
- StudentService 新增内部独立建档方法。该方法必须加入已有事务，重新核查数据库中的已启用 SCHOOL_ADMIN，按学校锁顺序检查身份、学号及学院，校验字段后插入并审计；不接受调用方 schoolId/userId，不写 user_account/auth_account。原关联已存在 STUDENT 用户的接口保持原契约。
- 确认仍受当前管理员身份、本人草稿、本校范围、CSRF、草稿有效期、expectedVersion 和 Idempotency-Key 保护。学生、草稿 CREATED 状态、确认凭证与审计同事务提交；失败一起回滚。
- 草稿持久化于原有 agent_student_draft，字段以 JSON 保存。省略字段保留原值，显式 null 清空；管理员可分多次补充。
- 姓名、学号、专业必填；年级和班级可选。年级为 1900–2100 的整数，班级名称最长 100 字符；手机号、日期、每周天数和学院仍执行既有校验。业务查重失败或资料未齐保持 COLLECTING。
- 正式创建结果来自数据库生成的学生 ID；重复确认返回同一档案，不再调用创建方法。不会因为聊天文字或模型回复直接建档。

## 草稿状态

| 状态 | 含义 |
| --- | --- |
| COLLECTING | 缺少必填项或字段校验未通过，允许补充 |
| READY | 字段校验通过，等待管理员明确确认 |
| CREATED | 已确认并提交正式学生档案，禁止编辑 |
| CANCELLED | 管理员取消，禁止继续确认 |
| EXPIRED | 已过有效期，禁止编辑/确认 |

新记录保存上述状态。旧 EDITING/COMMITTED 分别兼容为 COLLECTING/CREATED，读取时同步规范化持久化状态；旧幂等结果也返回 CREATED。查询过期活动草稿时将 EXPIRED 写入 MySQL；保留原版本保护。

## 普通 REST 接口

所有接口沿用 Session、CSRF 和 `{code,message,data,timestamp}` 返回结构。

| 方法与路径 | 主要参数与结果 |
| --- | --- |
| POST `/api/agent/sessions` | 创建管理员本校本人会话，返回 sessionId |
| POST `/api/agent/sessions/{id}/drafts` | `{fields:{...}}`，允许不完整资料，返回持久化草稿 |
| GET `/api/agent/drafts/{id}` | 返回字段、版本、状态、缺失项和错误 |
| PATCH `/api/agent/drafts/{id}` | `{expectedVersion,fields}`；省略不覆盖，null 清空 |
| POST `/api/agent/drafts/{id}/cancel` | `{expectedVersion}` |
| POST `/api/agent/drafts/{id}/confirm` | `{expectedVersion}`，请求头 Idempotency-Key；不接受替换字段 |
| GET `/api/students/{id}` | 通过现有 StudentService/MyBatis 查询真实档案，包含 gradeYear/className |

示例字段：

```json
{
  "name": "虚构学生",
  "studentNo": "00060001",
  "major": "软件工程",
  "gradeYear": 2026,
  "className": "软件工程1班"
}
```

更新示例 `{"expectedVersion":1,"fields":{"major":"软件工程"}}` 不覆盖学号；`{"expectedVersion":2,"fields":{"className":null}}` 清空班级。确认时使用最新返回版本与稳定幂等键。网络丢失响应时使用同一键重试。

启用手动草稿只需 `app.agent.enabled=true`，保持 `app.agent.model-enabled=false`。页面仍从“AI 学生档案助手”进入，填写右侧表单；关闭模型时聊天发送按钮禁用，手动表单和确认仍工作。业务库未迁移前不能直接启用新构建，请先单独确认业务库升级。

## 自动化和运行验收

实际结果：

| 检查 | 结果 |
| --- | --- |
| 前端 `npm run check` | 通过，退出码 0 |
| 前端 `npm test` | 15 项，0 失败、0 跳过 |
| 隔离 MySQL 全量 Maven verify | 97 项，0 失败、0 错误、0 跳过，BUILD SUCCESS，退出码 0 |
| 新增无模型 REST/MySQL 集成测试 | 7 项，真正提交与回滚；另外在新运行库初始化前再次全部通过 |
| 旧学生结构迁移演练 | 原建档迁移加两列迁移通过，旧 ID/用户/学号保留，两字段初始 null，唯一约束仍生效 |
| 候选 JAR 的真实 HTTP/Session/CSRF | 草稿创建、补专业、班级清空/修改、确认、查询、重复学号、重复确认、取消及未登录拒绝通过 |
| Edge 手动表单 | 年级/班级修改、明确确认、丢失响应原键重试、桌面/390px 移动布局通过，页面错误 0，无触发模型的 POST 消息请求 |
| 候选进程 smoke | 29 项 GET 与认证 Session/CSRF/注销通过 |
| 候选进程重启 | 原 studentId、学号、年级、班级及 CREATED 草稿仍可读 |
| 独立 JDBC 查询 | 正式档案已提交，前导零保留，grade_year/class_name 正确，user_id 为 NULL，一个对应 CREATED 草稿，无学生用户/登录账号 |

测试覆盖缺失字段、错误类型/年级、身份字段注入、跨学校和其他管理员草稿访问、无权限、CSRF、停用账号、版本冲突、重复学号、并发确认、幂等冲突和事务回滚。故意让最后的确认审计失败，REST 返回 500 后学生、通用创建审计和确认凭证都不存在，草稿保持 READY；撤销故障后同一键可成功重试。

首轮发现显式 null 测试请求被 NON_NULL 序列化省略，修正为保留 null 的请求 JSON，并核对真实 fields_json；重跑全量通过。运行脚本也修正了进程 signalCode 判断和模型调用计数：历史 GET 不等于触发处理的 POST。最终脚本成功退出后才登记验收完成。

## 复验命令

使用一个新的隔离库名，不把以下命令指向业务库。已有本机安全配置提供数据库凭据，不在命令中写密码。

```powershell
npm run check
npm test
.\scripts\test-mysql.ps1 '-Dmaven.repo.local=C:\Users\yc200\.m2\repository' '-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/zhigangzong_agent_phase1_test_20261012_newrun?createDatabaseIfNotExist=true'

$env:STUDENT_DRAFT_TEST_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/zhigangzong_agent_phase1_test_20261012_newrun?createDatabaseIfNotExist=true'
$env:JAVA_EXECUTABLE = 'D:\java\bin\java.exe'
$env:STUDENT_DRAFT_TEST_BROWSER = 'true'
$env:PLAYWRIGHT_MODULE_PATH = 'C:\Users\yc200\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules\playwright'
node .\scripts\test-student-drafts-api.mjs

$taskResult = Get-Content '.local\student-drafts-api\result.json' -Raw | ConvertFrom-Json
& 'D:\java\bin\java.exe' '-Dfile.encoding=UTF-8' --class-path 'C:\Users\yc200\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar' 'scripts\VerifyStudentDraftDatabase.java' $taskResult.studentId $taskResult.studentNo $taskResult.gradeYear $taskResult.className
```

上述运行脚本不建表，要求测试初始化已成功，并且库内没有此前登录账号；通过随机临时管理员密码启动自己的后端，再自行结束进程。重复运行时选用新隔离库，既有密码不会被重置。关闭浏览器选项可仅做 REST/重启/smoke；未开启的浏览器检查不能记作通过。其他电脑按实际 Java、Maven 缓存、Connector/J 和 Playwright 路径调整。

日志、虚构资料结果和截图均保存在被忽略的 `.local/`：全量日志 `.local/phase1/maven-verify.log`；运行结果 `.local/student-drafts-api/result.json`；截图 `draft-desktop.png`、`created-mobile.png`；独立查询 `.local/phase1/independent-jdbc-check.txt`。不提交数据库密码、临时管理员密码或日志。

## 修改范围

- 新增 StudentDraftService、StudentDraftStatus、CreateStandaloneStudentProfileRequest、StudentDraftBusinessIntegrationTest、年级/班级迁移 SQL，以及普通 REST/浏览器和独立 JDBC 验收脚本。
- 修改现有 StudentService/StudentServiceImpl、StudentProfile/Mapper、AgentController/Store/Mapper、StudentProfileCreationService、StudentDraftRules、学生表新环境定义和草稿页面。
- 更新原后端/前端回归测试、[开发顺序](llm-integration-plan.md)、[交接清单](NEXT-STEPS.md)和根目录 complete.md；保留已有未提交成果与版本历史。
- 没有开发模型适配器、增加模型 SDK、运行真实 LLM、执行业务库迁移、提交、推送或发布。

第一阶段报告后等待用户确认。确认进入第二阶段才开发统一模型接口；第三阶段必须等第二阶段验收与用户确认后再开始。

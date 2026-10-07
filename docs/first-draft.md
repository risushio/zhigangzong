> 历史阶段记录（2026-10-05）。文中的无前端、未实现登录及旧测试数量仅描述当时版本。当前功能和运行方式见 [本地运行与联调](local-workflow.md)，本轮验收见 [2026-10-06 验证](verification-2026-10-06.md)。

# 第一版后端草稿说明

验证日期：2026-10-05（Asia/Shanghai）。本次依据 README 全文建立基础工程，保留全部 20 项 P0 需求的对应位置。README 没有指定前端技术栈，因此当前只交付 REST API，不创建前端工程。

## 1. 技术与范围

- Java 17、Spring Boot 3.5.8、MyBatis Spring Boot Starter 3.0.5、Maven。
- MySQL Connector/J 由 Spring Boot 依赖管理统一选择；本机实际连接 MySQL 8.0.45。
- 单个 Spring Boot 应用，按 Controller → Service → Mapper → MySQL 分层。
- 使用本机已有 JDK `D:\java`、Maven `D:\maven` 和 MySQL80 服务，不修改系统 PATH 或数据库账号。
- 本地草稿默认监听 `127.0.0.1:8080`。登录认证和数据权限尚未实现，当前用于本机开发验证；接入真实师生数据前先实现访问控制。
- 未添加 Docker、Redis、MQ、微服务、复杂模型或 UI。

版本选择参考：[Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[MyBatis Starter 兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

## 2. 当前目录

```text
zhigangzong/
├── README.md                       原需求及运行入口
├── pom.xml                         Maven 与依赖管理
├── .gitignore                      排除本机密码、日志、构建产物
├── config/
│   └── application-local.properties.example
├── docs/
│   ├── first-draft.md               模块、接口、表结构和边界
│   ├── verification.md              实际验证结果及排错记录
│   └── api.http                     示例 HTTP 请求
├── scripts/
│   ├── maven.ps1                    查找并复用本机 Maven/JDK
│   ├── start.ps1                    构建并启动（或 -SkipBuild）
│   ├── test-mysql.ps1               启用真实 MySQL 集成测试
│   └── smoke.ps1                    验证运行中的 HTTP 服务
├── src/main/java/com/zhigangzong/
│   ├── ZhigangzongApplication.java
│   ├── config/                     MyBatis 扫描
│   ├── common/                     ApiResponse、PageQuery、PageResult
│   ├── exception/                  业务异常与统一 HTTP 异常处理
│   ├── controller/                 按业务领域组织的控制器
│   ├── service/
│   │   └── impl/                   分页、基础创建及事务
│   ├── mapper/                     参数化 SQL 与 MyBatis 映射
│   ├── entity/                     25 个核心表的实体
│   ├── dto/                        7 个基础创建请求及校验
│   └── vo/                         健康、模块状态、统计、推荐契约
├── src/main/resources/
│   ├── application.yml
│   └── db/schema.sql               25 张表、外键、索引及基础约束
├── src/test/java/com/zhigangzong/
│   ├── common/PageQueryTest.java
│   ├── controller/ApiContractTest.java
│   ├── service/StudentServiceTest.java
│   └── MySqlIntegrationTest.java
├── .local/                         本机私有配置和日志，Git 忽略
└── target/                         构建产物，Git 忽略
```

主要业务类按 Organization、Student、Enterprise、Job、Recruitment、Internship、Tracking、Risk、Evaluation、Workflow、Matching、Statistics 命名。实体与 Mapper 一一对应，业务 Service 按领域聚合。

## 3. README 功能对应

“查询骨架”表示真实查询 MySQL 中对应表，支持分页和详情；并不表示相关审批、自动化或状态流转已完成。未提供的写操作不会修改业务状态。

| P0 | 模块 | 已有能力 | 后续实现 |
| --- | --- | --- | --- |
| 01 | 身份与权限 | 学校/学院/角色用户基础读写 | 登录认证、职责权限、学校学院及指导关系的数据范围 |
| 02 | 学生档案 | 档案创建、分页与详情 | 档案编辑、简历文件上传与分享控制 |
| 03 | 企业与基地审核 | 企业资料创建、分页与详情，初始 PENDING | 审核、考察、暂停合作流程 |
| 04 | 岗位管理 | 岗位创建、分页与详情，初始 PENDING/DRAFT | 审核、发布、下架 |
| 05 | 岗位搜索 | 岗位分页与收藏数据查询骨架 | 多条件搜索、收藏操作 |
| 06 | 基础智能匹配 | 推荐接口契约及反馈查询骨架，推荐返回 501 | 可解释规则评分、硬条件冲突与能力缺口 |
| 07 | 投递与录用 | 申请及状态历史查询骨架 | 投递、面试、录用、学生确认 |
| 08 | 自主实习申报 | placement.source=SELF 与资料结构、查询骨架 | 自主申报提交与审核 |
| 09 | 实习计划 | 批次创建、分页与详情；目标/任务/材料/评分字段 | 双导师分配、计划编辑 |
| 10 | 入岗审批与材料 | 独立学校审批状态、审批历史与材料查询骨架 | 材料上传、审核、到岗确认 |
| 11 | 过程记录 | 周报/阶段任务及教师反馈查询骨架 | 草稿保存、提交、批阅与退回 |
| 12 | 出勤与变更 | 出勤、补签、申诉类型及保留原始/申请快照的变更查询骨架 | 按需签到、请假、变更审批与执行 |
| 13 | 指导与联系 | 双导师联系记录查询骨架 | 新增指导记录、定期联系提醒 |
| 14 | 异常预警与求助 | 责任人、处理结果及独立跟进记录查询骨架 | 规则预警、求助提交、分派、处理及复核 |
| 15 | 多方评价 | 自评/教师/企业评价查询骨架 | 提交、评分权重、退回与成绩复核 |
| 16 | 结项与归档 | 结项记录查询骨架 | 完成审核、档案打包与材料批量导出 |
| 17 | 通知与待办 | 通知、截止时间及完成时间查询骨架 | 业务事件生成通知、标记已读/完成 |
| 18 | 统计与报表 | 基础数据计数概览 | 带批次范围及明确分母的业务比率、Excel 导入导出 |
| 19 | 流程配置 | 学院/专业/批次规则查询骨架 | 配置编辑、规则校验与流程执行 |
| 20 | 数据保护与操作留痕 | 凭据隔离、基础创建操作日志查询 | 访问审计、隐私告知、授权分享、保留期限与备份恢复 |

P1 的简历解析、教师辅助、校园系统对接、小程序、电子签约、转就业跟踪、多学校隔离等继续保留为后续范围，本阶段不实现。实体中的 schoolId 仅是组织归属，不构成已完成的多租户隔离。

## 4. 数据库结构

默认数据库为 `zhigangzong`。JDBC URL 的 `createDatabaseIfNotExist=true` 会在数据库缺失且账号有权限时创建该项目数据库。启动时执行 `src/main/resources/db/schema.sql`，仅包含 `CREATE TABLE IF NOT EXISTS`。

脚本不含 DROP、TRUNCATE、DELETE 或 ALTER；已有表不被覆盖。后续修改字段时应增加明确的迁移脚本，不能依赖 IF NOT EXISTS 自动升级已有表。当前初始化失败会终止启动，避免提供一个无法访问数据库的假健康服务。

| 表 | 内容 | 业务领域 |
| --- | --- | --- |
| `school` | 学校 | Organization |
| `department` | 学院 | Organization |
| `user_account` | 用户角色 | Organization |
| `student_profile` | 学生档案 | Student |
| `enterprise` | 企业与基地 | Enterprise |
| `job_position` | 岗位 | Job |
| `job_favorite` | 岗位收藏 | Job |
| `job_application` | 投递与录用 | Recruitment |
| `recruitment_event` | 招聘状态历史 | Recruitment |
| `internship_batch` | 实习计划批次 | Internship |
| `internship_placement` | 自主申报与学校入岗审批 | Internship |
| `approval_record` | 审批历史 | Internship |
| `internship_material` | 协议保险等入岗材料 | Internship |
| `progress_report` | 周报与阶段任务 | Tracking |
| `attendance_record` | 出勤与补签 | Tracking |
| `change_request` | 实习变更历史 | Tracking |
| `guidance_record` | 双导师指导与联系 | Tracking |
| `risk_alert` | 异常预警与求助 | Risk |
| `risk_follow_up` | 异常跟进记录 | Risk |
| `internship_evaluation` | 多方评价 | Evaluation |
| `archive_record` | 结项与归档 | Evaluation |
| `notification` | 通知与待办 | Workflow |
| `workflow_rule` | 专业批次流程配置 | Workflow |
| `audit_log` | 操作留痕 | Workflow |
| `match_feedback` | 推荐解释反馈 | Matching |

关键建模约定：

1. `job_application.recruitment_status` 记录招聘状态；`internship_placement.school_approval_status` 独立记录学校批准，不自动同步。
2. 新企业审核状态为 PENDING；新岗位为 PENDING / DRAFT，创建岗位不表示已发布或学校认可。
3. `internship_placement.source` 支持 PLATFORM / SELF。SELF 可以没有平台岗位和投递记录，但仍关联企业资料，并进入同样的学校审批范围。
4. `change_request` 保存原始和申请快照，`approval_record` 和 `recruitment_event` 保存历史。本阶段不提供直接覆盖历史的修改接口。
5. `risk_alert` 允许尚未入岗的学生求助；责任人、结果、处理时间与 `risk_follow_up` 分开保存。
6. 三类评价分别记录，不提前计算最终成绩；流程规则预留签到开关、周报频率和评价权重。
7. 表之间使用外键且不做级联删除；基础日期、角色、人数、评分等有必要约束。
8. 7 类基础创建与 `audit_log` 在同一事务内提交。当前未认证，日志 actor_id 为 NULL，明确记录为本地草稿创建行为，未冒充真实操作者。
9. 所有表有 id / created_at。文件字段仅存引用，不代表已实现上传或电子签约；created_at 也不能替代未来的状态历史。

## 5. API

基础地址：`http://127.0.0.1:8080/api`。

统一返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "data": { "items": [], "total": 0, "page": 1, "size": 20 },
  "timestamp": "2026-10-05T12:05:25Z"
}
```

分页参数为 `page=1&size=20`，size 范围 1–100，按 id 倒序。接口返回数据库真实数据；刚初始化时业务表为空。

| 路径 | 方法 | 当前行为 |
| --- | --- | --- |
| `/health` | GET | 通过 MyBatis 执行 SELECT 1，返回数据库名、MySQL 版本与表数；连接失败返回 503 |
| `/health/live` | GET | 应用存活检查 |
| `/modules` | GET | README 全部 P0 模块的实现与待办状态 |
| `/statistics/overview` | GET | 学生、企业、岗位、实习记录、学校已批准记录及未处理完异常的计数 |
| `/recommendations?studentId=1` | GET | 预留契约，明确返回 501 NOT_IMPLEMENTED |
| `/schools`、`/schools/{id}` | GET | 学校分页、详情 |
| `/departments`、`/departments/{id}` | GET | 学院分页、详情 |
| `/users`、`/users/{id}` | GET | 用户角色分页、详情 |
| `/students`、`/students/{id}` | GET | 学生档案分页、详情 |
| `/enterprises`、`/enterprises/{id}` | GET | 企业与基地分页、详情 |
| `/jobs`、`/jobs/{id}` | GET | 岗位分页、详情 |
| `/job-favorites`、`/job-favorites/{id}` | GET | 岗位收藏分页、详情 |
| `/applications`、`/applications/{id}` | GET | 投递与录用分页、详情 |
| `/recruitment-events`、`/recruitment-events/{id}` | GET | 招聘状态历史分页、详情 |
| `/batches`、`/batches/{id}` | GET | 实习计划批次分页、详情 |
| `/placements`、`/placements/{id}` | GET | 自主申报与学校入岗审批分页、详情 |
| `/approvals`、`/approvals/{id}` | GET | 审批历史分页、详情 |
| `/materials`、`/materials/{id}` | GET | 协议保险等入岗材料分页、详情 |
| `/reports`、`/reports/{id}` | GET | 周报与阶段任务分页、详情 |
| `/attendance`、`/attendance/{id}` | GET | 出勤与补签分页、详情 |
| `/changes`、`/changes/{id}` | GET | 实习变更历史分页、详情 |
| `/guidance`、`/guidance/{id}` | GET | 双导师指导与联系分页、详情 |
| `/alerts`、`/alerts/{id}` | GET | 异常预警与求助分页、详情 |
| `/alert-follow-ups`、`/alert-follow-ups/{id}` | GET | 异常跟进记录分页、详情 |
| `/evaluations`、`/evaluations/{id}` | GET | 多方评价分页、详情 |
| `/archives`、`/archives/{id}` | GET | 结项与归档分页、详情 |
| `/notifications`、`/notifications/{id}` | GET | 通知与待办分页、详情 |
| `/workflow-rules`、`/workflow-rules/{id}` | GET | 专业批次流程配置分页、详情 |
| `/audit-logs`、`/audit-logs/{id}` | GET | 操作留痕分页、详情 |
| `/match-feedback`、`/match-feedback/{id}` | GET | 推荐解释反馈分页、详情 |
| `/schools` | POST | 创建学校，成功返回 HTTP 201 |
| `/departments` | POST | 创建学院，成功返回 HTTP 201 |
| `/users` | POST | 创建用户角色，成功返回 HTTP 201 |
| `/students` | POST | 创建学生档案，成功返回 HTTP 201 |
| `/enterprises` | POST | 创建企业与基地，成功返回 HTTP 201 |
| `/jobs` | POST | 创建岗位，成功返回 HTTP 201 |
| `/batches` | POST | 创建实习计划批次，成功返回 HTTP 201 |

统计口径：当前数据库全部记录的计数；approvedPlacements 仅统计 school_approval_status=APPROVED；openAlerts 统计 OPEN / IN_PROGRESS。尚未计算落实率或完成率，也未进行学院、批次或时间过滤。

状态码：参数错误 400；记录不存在 404；不支持的方法 405；唯一键、外键或约束冲突 409；不支持的请求类型 415；未实现能力 501；数据库访问失败 503。错误响应不向调用方暴露 SQL、密码或堆栈。

7 个创建接口的字段见对应 `dto/Create*Request.java`。创建顺序建议：学校 → 学院 → 用户 → 学生档案；企业 → 岗位；学院 → 实习批次。学生档案必须关联 STUDENT 角色用户。

所有其他模块本阶段仅提供查询骨架。审批、录用、到岗、变更、归档等状态不可通过这些查询接口直接推进。

## 6. 本机运行

在项目根目录运行 PowerShell。

当前电脑的本地数据库配置已保存在 `.local/application-local.properties`，该文件被 Git 忽略。其他电脑可将 `config/application-local.properties.example` 复制到此路径后填写自己的数据库凭据，或设置进程环境变量 DB_USERNAME / DB_PASSWORD。

支持 DB_HOST、DB_PORT、DB_NAME、DB_USERNAME、DB_PASSWORD、SERVER_ADDRESS、SERVER_PORT。启动脚本默认使用端口 8080，可传 `-Port 8081`。可使用 JAVA_HOME / MAVEN_HOME 指定其他现有安装；脚本只临时设置当前进程环境，不修改系统环境。

```powershell
# 构建与不依赖 MySQL 的测试
.\scripts\maven.ps1 verify

# 启用真实 MySQL 集成测试并构建
.\scripts\test-mysql.ps1

# 构建后启动，Ctrl+C 停止
.\scripts\start.ps1

# 已经构建时直接启动
.\scripts\start.ps1 -SkipBuild

# 在另一个终端检查运行中的接口
.\scripts\smoke.ps1
Invoke-RestMethod http://127.0.0.1:8080/api/health
```

也可使用标准 Maven：`mvn verify` / `mvn spring-boot:run`。直接运行 JAR 时保持工作目录为项目根目录，以便读取 `.local` 配置。仅使用环境变量时无此相对路径限制。

普通 `mvn verify` 执行 8 个无数据库测试并跳过 4 个 MySQL 集成测试；`test-mysql.ps1` 启用全部 12 个测试。集成测试使用实际 MySQL，不使用 H2；测试写入放在事务中回滚，不清库、不删表。建表 DDL 和自增序列变化不会因测试回滚而撤销。

## 7. 下一阶段

先补简单登录与学校/学院/指导关系的数据范围，再实现一条纵向流程：企业/岗位审核 → 学生投递或自主申报 → 学校审批 → 双导师分配与到岗确认。随后实现周报批阅及异常求助闭环，再做规则推荐、多方评价和归档。业务状态变更应继续采用专用操作接口、权限校验、事务与历史记录，避免通用 CRUD 随意改状态。

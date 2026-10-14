# Agent V1 第一阶段数据库审查与迁移方案

日期：2026-10-12。开发顺序调整为数据库与学生业务 → LLM 接口 → 聊天集成；每阶段验收后等待用户确认再继续。本文为执行前审查，尚未执行数据库结构变更。

## 已检查的实际数据库

通过本机安全连接配置建立只读 JDBC 连接，MySQL 8.0.45，当前业务库 `zhigangzong`。未输出凭据、学生字段值或完整私密配置。

| 检查项 | 实际结果 |
| --- | --- |
| 学生表 | 只有 `student_profile`，不另建 student 表 |
| 历史学生数量 | 2 条；`user_id` 为 NOT NULL |
| 当前学生字段 | 学号、专业、技能、项目经历、简历引用、城市、可实习日期、每周天数及创建时间 |
| 缺失学生字段 | 学校、独立姓名、学院、手机号、年级、班级名称 |
| 学生唯一索引 | PRIMARY(id)、uk_student_user(user_id)；尚无学校内学号唯一索引 |
| 规范化学号冲突 | 按关联用户学校及 TRIM(学号)检查，重复组 0 个 |
| 学院关联索引 | 已有 department(id, school_id) 唯一索引，可供学校范围外键引用 |
| 草稿/会话相关表 | 业务库尚无 Agent 的 5 张表 |
| 历史用户数量 | 7 条；本阶段不为学生建档新增用户或登录账号 |

源码中的 `schema.sql` 已含此前 Agent 的学生表扩展，业务库尚未应用；`CREATE TABLE IF NOT EXISTS` 不会把旧表自动升级。因此不能将源码定义或隔离库的旧验收记录当作当前业务库结构。

## 已实现、待完善与待验证

| 范围 | 现有情况 | 第一阶段处理 |
| --- | --- | --- |
| 数据源/MyBatis | Spring Boot 已配置 MySQL，两个 Mapper 包已扫描；只读 JDBC 连接与查询成功 | 再以真实 Spring/MyBatis 和 REST 验证查询、插入、更新及回滚 |
| 学生业务 | 已有 StudentService、Mapper、独立建档服务 | 确认后统一通过 StudentService 建档，保留原关联用户接口 |
| 草稿 | AgentStore 已持久化会话、字段、来源、版本和状态，已有增改查/取消 | 建立复用 AgentStore 的 StudentDraftService 业务入口，不新增第二套草稿表 |
| 状态 | 已有 EDITING、READY、COMMITTED、CANCELLED、EXPIRED | 对外及新持久化状态改为 COLLECTING、READY、CREATED、CANCELLED、EXPIRED；兼容读取旧状态 |
| 确认保护 | 已有 Session/CSRF、当前数据库权限、学校范围、版本、幂等、审计和事务 | 保留并覆盖无模型 REST 测试、重复学号及并发、审计失败回滚 |
| 年级/班级 | 源码与实际库均无对应字段 | 增加 grade_year、class_name，支持草稿补充、修改、清空、预览及正式保存 |
| 本轮数据库验收 | 尚未执行写入或迁移 | 用户确认隔离库方案后执行，不用模型调用代替业务验收 |

## 待确认的迁移内容

### 旧版业务库升级顺序

1. 备份后重新检查真实库字段与规范化学号重复；发生冲突时停止，由业务人员处理，不自动删除或合并学生。
2. 执行已有 [20261012-agent-profile.sql](../src/main/resources/db/migrations/20261012-agent-profile.sql)：允许 user_id 为 null；增加 school_id/name/department_id/phone；通过现有用户资料回填历史字段；TRIM 学号，保留前导零；增加学校内学号唯一约束和学校/学院外键。
3. 执行新增 [20261012-student-grade-class.sql](../src/main/resources/db/migrations/20261012-student-grade-class.sql)：

   ```sql
   ALTER TABLE student_profile
       ADD COLUMN grade_year INT NULL,
       ADD COLUMN class_name VARCHAR(100) NULL;
   ```

4. 执行已有 [agent-tables.sql](../src/main/resources/db/agent-tables.sql)，建立缺失的会话、消息、草稿、确认幂等和工具记录表，不重复创建学生表。
5. 部署通过测试的第一阶段构建。模型保持关闭；管理员以普通草稿 REST API 操作。每阶段报告后仍须用户确认，才进入下一阶段。

上述业务库步骤仅供审查；未经单独确认不对 `zhigangzong` 执行。新建隔离库直接使用最终 `schema.sql` 与 `agent-tables.sql`；旧库升级 SQL 会另外在隔离库的旧结构夹具上演练。

### 兼容性分析

- 保留现有学生主键、user_id 关联、其他业务外键和学号字符串；不删除、重建或重编号学生。
- `grade_year` 与 `class_name` 可空，旧数据保持 null，不推算；姓名、学号、专业仍为必填，年级/班级为可选字段。
- 学号唯一范围为学校，按现有学校身份判定；应用在创建时检查，数据库唯一索引兜底并发。不能将不存在的学校身份交给调用方填写。
- 不建立班级字典、class_id、专业字典或新的学生账号。班级名称为长度受限的文本。
- 草稿状态列已有 VARCHAR(16)，足以容纳新状态，不需要修改列类型；应用兼容旧 EDITING/COMMITTED 记录与幂等响应。
- MySQL DDL 不能由业务事务整体撤销，脚本为一次性迁移；重复执行前核对字段和索引，不采用静默忽略错误。
- 新二进制不能直接运行在未迁移的旧库。业务库升级前保持现有运行进程与数据；回退时关闭助手，保留新增列、草稿与确认记录，不删除已建档学生。

## 请求执行范围

先批准在**新的隔离测试库**执行最终建表定义和迁移夹具演练，例如 `zhigangzong_agent_phase1_test_20261012_<唯一后缀>`。测试只使用虚构数据；旧业务库 `zhigangzong` 仅只读检查。

隔离验收包括：无模型 REST 草稿增改查/取消、五种状态、缺失/错字段、无权限/CSRF、重复学号、重复确认、版本冲突、真实 Mapper 插入/更新/查询、审计及事务回滚、旧数据迁移兼容。提交成功后通过独立数据库查询确认正式档案已存在，并核对 user_account/auth_account 数量没有增加；测试数据保留在隔离库或按测试夹具清理。

以上为执行前提供的审查记录。随后用户已批准仅在新隔离库执行；第一阶段已完成隔离验收，实际结果见 [验收说明](agent-phase1-workflow.md)。业务库迁移仍未经批准，第二、第三阶段保持待确认，不开发模型接口。

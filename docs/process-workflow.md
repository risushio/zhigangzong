> 2026-10-07 已补齐自主申报及私有文件模块，最新范围见 [自主申报与文件说明](self-files-workflow.md)。下文保留前一轮验收记录。

# 双导师与周报过程闭环

更新：2026-10-06。沿用 Spring Boot、MyBatis、本机 MySQL、原生 JavaScript 及既有深蓝黑粒子主题。

## 页面与流程

1. 管理员在“组织与用户”新增 TEACHER 或 ENTERPRISE_MENTOR 用户，再在“业务办理 → 账号开通”建立登录账号。教师无需企业，企业导师必须绑定企业；已存在账号不能重复开通或覆盖密码。
2. 实习经过独立学校审批批准后，在“审批与导师分配 → 导师与周报”选择本校已开通教师及该企业已开通导师，填写分配说明。改派追加历史；旧导师立即失去该实习及周报访问权。
3. 企业导师登录进入“指导学生与周报”，只能看到同校、所属企业且具体分配给自己的实习。打开“导师与周报 → 确认学生到岗”，填写实际日期和核实说明；日期在实习期间且不晚于今天。重复到岗确认被拒绝。
4. 学生进入“审批与过程跟踪 → 导师与周报 → 新建周报草稿”，填写标题、日期和内容。已批准且到岗后才可填报；首次草稿仅学生本人和本校管理员可查看。保存不等于提交。
5. 学生打开草稿，“提交教师批阅”。提交后锁定编辑，指导教师收到通知。
6. 教师打开所指导实习的周报，“批阅或退回”。通过为 REVIEWED；退回为 RETURNED，必须填写指导反馈。
7. 学生修改退回周报，保存修改后再次提交；教师可以再次批阅。各次保存、提交、退回和通过保留内容、反馈、操作人及时间快照。

周报状态：DRAFT → SUBMITTED → REVIEWED / RETURNED；RETURNED → 保存修改（仍为 RETURNED）→ SUBMITTED。已通过周报不能再次编辑或重复批阅。企业导师可查看提交过的周报及反馈，批阅权归当前校内指导教师。

## 接口

统一前缀 `/api/portal`；使用现有 Session 和动态 CSRF 请求头。

| 方法与路径 | 功能与角色 |
| --- | --- |
| POST /accounts | 本校管理员开通学生、教师、招聘、企业导师账号 |
| GET /placements | 学生本人、本校管理员、当前获分配双导师的实习分页 |
| GET /placements/{id}/mentors | 本校管理员查询可分配导师 |
| POST /placements/{id}/mentors | 本校管理员分配/改派双导师 |
| POST /placements/{id}/arrival | 获分配企业导师确认到岗 |
| GET /placements/{id}/process | 范围内过程历史和周报 |
| POST /placements/{id}/reports | 学生本人新建草稿 |
| GET /reports/{id} | 范围内周报内容和快照历史 |
| PUT /reports/{id} | 学生本人修改草稿或退回周报 |
| POST /reports/{id}/submit | 学生提交或重提 |
| POST /reports/{id}/review | 当前指导教师通过或退回 |
| GET /notifications、POST /notifications/{id}/read | 各业务角色本人通知和已读 |

仅新增 `placement_process_event` 与 `progress_report_event` 两张历史表，数据库共 29 张表。已有 placement 导师/到岗字段及 progress_report 表直接复用。未删除表、文件、业务记录，未提交或推送。

## 本机演示与验证证据

入口：`http://127.0.0.1:8080/#/login`；启动 `.\scripts\start.ps1`。避免与已有 8080 进程重复启动。Windows 构建前需停止正在运行的本项目 JAR，否则打包重命名可能被文件锁阻止。

- 原管理员凭据 `.local/frontend-login.txt`；学生/招聘 `.local/portal-demo-accounts.json`。
- 教师 `demo.process.teacher`、企业导师 `demo.process.mentor`；随机初始密码仅保存在忽略的 `.local/process-demo-accounts.json`。
- 本轮通过真实接口新增“过程闭环联调岗位”和“2026过程闭环联调”批次，完成招聘确认与学校批准；随后所有分配、到岗、周报和批阅操作均通过真实浏览器页面完成。
- 最终 Maven verify：25 tests，0 failures、0 errors、0 skipped；前端语法检查和 10 tests 通过；29 个基础 GET、管理员登录和注销通过。
- 新增 5 项数据库集成测试覆盖退回重提快照、首次草稿访问、异校/他人/他企业隔离、改派撤销旧访问权、错误角色、未到岗填报、日期、重复状态、教师/导师账号开通及绑定。测试事务回滚。
- MySQL 只读核验：实习 26 为 APPROVED / ARRIVED，教师 365、企业导师 366；周报 10 为 REVIEWED，批阅人 365；2 条过程事件、6 条周报事件，退回意见及修改前后内容均保留。
- 原示例实习 5 仍为 APPROVED，未被本轮修改；账号、周报和历史在服务重启后继续保留。
- 教师实际收到分配、到岗及两次提交通知，通过页面标记最新周报通知已读；最终浏览器错误日志为空。
- 最终真实浏览器显示已到岗、已批阅及历史；截图 `.local/process-workflow-verified.png`。构建日志 `.local/process-tests.log`，服务日志 `.local/process-runtime.log`。

## 当前边界

当前仍为单校本地试点。新 portal 接口按学校、本人、企业和指导关系校验；历史通用管理接口尚未全面隔离学校，不能描述为完整多租户系统。

双导师逐条分配；周报列表按实习显示，尚未实现大量周报独立分页、批量批阅、周报频率和截止规则、阶段任务、附件或指导联系记录。企业导师到岗确认尚无管理员代办/撤销流程。自主申报、文件管理、考勤变更、异常评价归档、推荐及统计仍按 NEXT-STEPS 顺序推进。

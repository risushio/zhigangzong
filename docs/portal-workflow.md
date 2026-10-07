> 2026-10-07 已补齐自主申报及私有文件模块，最新范围见 [自主申报与文件说明](self-files-workflow.md)。下文保留前一轮验收记录。

> 本轮已补齐教师/企业导师与周报闭环，最新接口和验收见 [过程闭环说明](process-workflow.md)。下文保留此前招聘审批验收。

# 多角色招聘与学校审批流程

更新：2026-10-06。此页补充 local-workflow.md，是当前最新实现和验收范围。

## 已实现

- 学校管理员开通本校学生、企业招聘账号；学生必须已有学生档案，招聘账号必须绑定企业。
- 学生独立导航：寻找已审核发布的岗位、编辑本人档案、投递、查看申请及流转历史、确认或撤回申请、提交学校审批、查看本人通知并标记已读。
- 企业招聘独立导航：仅处理同校且所属企业的候选人，邀请面试、发出录用或拒绝；不能代替学生确认。
- 学校管理员在“业务办理 → 学校审批办理”审批本校实习申请，支持批准、退回补充、不予批准；学生可补充日期后重新提交，既有审批历史保留。
- 招聘与审批事件写入通知、业务历史和审计日志；通知接收范围按本人、企业及学校限定。
- 已有申请或实习记录的岗位不能改换企业，避免候选人资料被转移给另一家企业。

招聘流程：APPLIED → INTERVIEW → OFFERED → ACCEPTED；允许直接从 APPLIED 发出录用。未结束申请可由学生撤回，企业可拒绝已投递或面试中的申请。终态不允许重复处理。

学校流程：学生确认录用后独立提交 → PENDING → APPROVED / RETURNED / REJECTED。RETURNED 可以补充后重新提交。录用不会自动创建学校批准结果。

## 本机演示账号

访问 http://127.0.0.1:8080/#/login 。

- 管理员：admin，密码见 `.local/frontend-login.txt`。
- 联调学生：demo.student。
- 联调招聘人员：demo.recruiter。
- 后两者随机密码见被 Git 忽略的 `.local/portal-demo-accounts.json`。这是本机联调数据，不在版本库中保存密码。

普通账号的开通路径：管理员先在组织与用户建立用户档案；学生另建学生档案；再进入“账号开通”。相同账号或用户再次开通返回冲突，不重置原密码。

## 新接口

统一前缀 `/api/portal`，Session + CSRF：

| 方法与路径 | 功能 |
| --- | --- |
| POST /accounts | 管理员开通学生或招聘账号 |
| GET /profile、PUT /profile | 本人学生档案查看、编辑 |
| GET /jobs | 已发布、已审核且未截止岗位搜索分页 |
| GET /applications、POST /applications | 按身份查询申请、学生投递 |
| GET /applications/{id} | 申请详情和状态历史 |
| POST /applications/{id}/transition | 面试、录用、确认、拒绝、撤回 |
| GET /batches | 学生本学院批次 |
| GET /placements、POST /placements | 范围内实习申请、学生提交审批 |
| GET /placements/{id} | 实习详情和审批历史 |
| POST /placements/{id}/approval | 学校审批 |
| POST /placements/{id}/resubmit | 退回后补充提交 |
| GET /notifications | 本人通知分页 |
| POST /notifications/{id}/read | 本人通知标记已读 |

新增表 `enterprise_member` 保存招聘用户与企业归属，共 27 张表；未删除或覆盖既有表。原管理员接口保持兼容，学生和招聘人员不能访问旧的管理接口。

## 验证结果

- 最终 `scripts/test-mysql.ps1`：Maven verify BUILD SUCCESS，20 tests、0 failures、0 errors、0 skipped。
- `npm run check`、10 个前端测试通过。
- `scripts/smoke.ps1` 29 个基础 GET + 登录注销通过。
- 真实浏览器切换三个角色，跑通投递 → 面试 → 录用 → 学生确认 → 提交学校审批 → 管理员批准 → 学生查看批准结果。
- 服务重启后原账号仍可登录，业务结果保留。MySQL 只读核验申请 id=9，ACCEPTED、student_confirmed=1；实习 id=5，APPROVED；四条招聘历史的操作人正确。
- 学生从页面修改本人档案，刷新显示新值；学校审批通知从页面标记已读，MySQL read_at 有实际时间。
- 浏览器最终错误日志为空。截图 `.local/portal-approval-verified.png`。
- 自动化测试另覆盖其他学生、其他企业、其他学校无法读取或办理记录，缺少权限返回 403，越权记录返回 404，重复投递/开通不覆盖数据；退回重提保留两条审批记录。
- 企业归属变更保护、通知接收范围由新增数据库测试验证。
- 本轮只使用明显标注的联调示例，没有真实学生个人资料，没有外发邮件或消息。

## 当前边界

仍是单校本地试点：新增 portal 接口已做本人/企业/学校范围校验；历史管理员通用查询尚未全面按多学校隔离。不要将此轮描述为完整多租户系统。

教师/企业导师、双导师分配、到岗和周报闭环已实现（见过程闭环说明）。学院管理员端、材料文件上传、签到变更、预警评价归档、智能匹配和报表导出仍待实现。简历字段仍是引用，没有实际文件上传或下载。

## 本轮改动位置

后端：PortalController、PortalService、PortalMapper、PortalRequests、SecurityConfig、ManagementMapper/Service、ModuleController、schema.sql。

前端：新增 portal.js，复用公共表格、表单、弹窗、状态、错误提示及原粒子系统主题；app.js 按角色展示入口并路由到相应工作空间。

测试：新增 PortalIntegrationTest 六项数据库测试，沿用其余测试；文档同步本轮结果。

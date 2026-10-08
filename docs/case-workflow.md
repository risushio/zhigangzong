# 学生求助闭环验收

2026-10-08，T05 已验证。独立求助支持尚未落实单位的学生；本人、本校管理员和明确分派的本校责任人可读，教师未分派或改派后不能访问，企业导师不自动获得求助内容。

学生提交 → 学校分派本校已开通教师/管理员 → 责任人跟进及提交结果 → 学生确认或退回 → 学校复核关闭或退回。关闭后记录只读，每次原因、责任人、结果和反馈保存独立快照。引用终止实习仅作为背景，不修改其只读过程记录。

接口：GET/POST `/api/portal/cases`，GET `/api/portal/cases/{id}`，POST 同路径下的 `assign`、`follow`、`resolve`、`confirm`、`review`，GET `/api/portal/case-owners`。写请求要求 Session 和 CSRF；列表分页按访问范围查询。新增 `student_case` 和 `student_case_event`，保留旧异常查询结构。

前端语法与 12 项测试通过，MySQL Maven verify 49 项全通过、无跳过并构建成功；29 项 smoke 查询及登录注销通过。真实浏览器走通学生求助、学校分派、教师跟进/结果、学生确认和学校复核关闭；合成案例已在 MySQL 保存 CLOSED 和 6 条事件。自动化另验证学生退回、学校退回重办、改派撤销访问、跨校/学生/角色边界与关闭后禁止写入。截图 `.local/case-workflow-verified.png`，凭据不写入文档。

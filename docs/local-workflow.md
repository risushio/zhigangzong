> 最新自主申报及私有文件管理见 [自主申报与文件说明](self-files-workflow.md)，本地上传存储位于 `.local/uploads/`，备份时须同时保存数据库和该目录。

> 最新进展：已增加教师/企业导师端、双导师分配、到岗与周报闭环，参见 [过程闭环说明](process-workflow.md)；招聘审批见 [多角色流程说明](portal-workflow.md)。下文管理员流程仍可使用。

# 本地运行与联调说明

更新日期：2026-10-06。当前支持学校管理员、学生、招聘、教师与企业导师，保留粒子系统主题。

## 打开项目

在项目根目录运行：

```powershell
.\scripts\start.ps1
```

浏览器打开 http://127.0.0.1:8080/#/login 。Spring Boot 同时提供前端页面和 API，不必另外启动前端。

- 用户名：`admin`。
- 本机密码见被 Git 忽略的 `.local/frontend-login.txt`，不在 README 或代码中公布。
- MySQL 配置位于 `.local/application-local.properties`，使用本机现有 MySQL。
- 已构建时可以使用 `.\scripts\start.ps1 -SkipBuild`。
- 若需前端即时预览，另开终端执行 `.\scripts\start-frontend.ps1`，访问 http://127.0.0.1:5173 。该服务将 `/api` 代理到 8080；后端仍需运行。
- 本地进程不会开机自启；端口已占用时先确认是否已有本项目进程。

## 首个管理员

其他电脑先复制 `config/application-local.properties.example` 到 `.local/application-local.properties`，填写该电脑的数据库凭据。

空 `auth_account` 表时，将 `app.bootstrap.enabled` 设为 `true`，并设置自己的 `app.bootstrap.password`（至少 12 字符、最多 72 UTF-8 字节），再启动服务。初始化器事务内建立本地学校、SCHOOL_ADMIN 用户档案和 BCrypt 账号。用户名为 `admin`。

已有任意登录账号时，初始化器直接跳过，不新增、不重置密码。初始化成功后可以关闭 `app.bootstrap.enabled`。本机已有账号，不需要重复开通。

## 当前可操作流程

1. 登录，进入“企业与基地”，新增企业。
2. 打开详情、编辑资料，通过“审核企业”填写意见并审核。
3. 在“岗位管理”新增岗位，选择该企业；填写城市、人数、说明等。
4. 编辑岗位后重新进入待审核草稿；打开详情执行“审核岗位”。
5. 审核通过后发布，或将已发布岗位下架。
6. 用关键词、审核状态、城市筛选，再刷新页面核对保存结果。

企业修改资料会重置审核状态并下架关联岗位；岗位修改后必须重新审核。未审核通过的企业或岗位不能发布。审核意见最多 480 字，保留日志状态前缀空间。

## 验证命令

```powershell
npm run check
npm test
.\scripts\test-mysql.ps1
# 后端已启动时：
.\scripts\smoke.ps1
```

数据库集成测试使用事务回滚，不清空已有业务表。smoke 脚本从本地配置读取管理员密码，也支持进程环境变量 `ZGZ_ADMIN_PASSWORD`，仅允许访问回环主机。

## 接口与范围

- GET `/api/auth/csrf`，POST `/api/auth/login`（表单编码），GET `/api/auth/me`，POST `/api/auth/logout`。
- 业务接口要求 SCHOOL_ADMIN；写请求携带 CSRF 返回的动态请求头与 Session Cookie。登录后重新取 CSRF。
- GET `/api/catalog/{resource}` 支持关键词、状态、分页；岗位另支持城市。
- 原有 25 类资源查询及 7 类基础新增接口保留。
- PUT `/api/enterprises/{id}`、`/api/jobs/{id}`。
- POST `/api/enterprises/{id}/review`、`/api/jobs/{id}/review`、`/api/jobs/{id}/publication`。
- 原 27 张表加 placement_process_event、progress_report_event 两张历史表（共 34 张表（含本轮 5 张自主申报/文件表））。未删除表或已有数据。

投递录用、学校审批和本人通知已补充。考勤、预警评价、学院管理员与完整数据权限、智能推荐仍待开发。推荐接口仍明确返回 501。

## IntelliJ IDEA 的本机 Maven 配置

2026-10-07 已在当前项目的 IDEA 设置中指定 Maven 主路径 `D:\maven`（3.9.9），导入器和运行器均使用项目 SDK Java 17（`D:\java`）。本地仓库保留 `C:\Users\yc200\.m2\repository`，依赖镜像沿用该 Maven 的 `conf/settings.xml`。Lombok 注解处理已由 Maven 导入启用。

从项目根目录的 `pom.xml` 打开并导入，不要单独把 `src/` 或 `src/main/` 当成工程。右侧 Maven 工具窗口中可重新同步项目、执行 Lifecycle → compile；编译成功后再运行 `ZhigangzongApplication`，运行工作目录为项目根目录。8080 已运行时先停止旧服务，避免重复启动。

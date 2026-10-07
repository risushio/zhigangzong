> 历史阶段记录（2026-10-05）。文中的无前端、未实现登录及旧测试数量仅描述当时版本。当前功能和运行方式见 [本地运行与联调](local-workflow.md)，本轮验收见 [2026-10-06 验证](verification-2026-10-06.md)。

# 第一版实际验证记录

验证日期：2026-10-05，Asia/Shanghai。执行位置：项目根目录 `C:\Users\yc200\Documents\ChatGPT\创业大赛\zhigangzong`。

## 验收结果

| 检查 | 实际结果 |
| --- | --- |
| README | 已完整读取；20 项 P0 在模块状态、代码和数据库结构中有对应位置 |
| 本机环境 | Java 17.0.12；Maven 3.9.9；MySQL80 服务运行 |
| Maven 构建 | `mvn -B -ntp verify`，BUILD SUCCESS |
| 自动化测试 | 12 tests，0 failures，0 errors，0 skipped（启用 RUN_MYSQL_TESTS=true） |
| 独立 JAR 启动 | 通过 `scripts/start.ps1 -SkipBuild` 启动成功，日志记录 Started ZhigangzongApplication |
| Web 服务 | Tomcat 在 127.0.0.1:8080 启动 |
| 数据库连接 | HikariPool 启动成功，SELECT 1 成功 |
| 数据库版本 | MySQL 8.0.45，端口 3306 |
| 数据库与表 | 新建项目数据库 zhigangzong，25 张核心表正常 |
| HTTP 检查 | `scripts/smoke.ps1` 的 29 个 GET 请求全部通过 |
| 基础写入 | 学校、学院、角色用户、学生档案、企业、岗位、批次创建成功；返回生成 id |
| 初始状态 | 企业 PENDING；岗位 PENDING / DRAFT |
| 异常路径 | 参数、JSON、日期、分页、角色、记录不存在、重复记录及外键冲突等检查通过 |
| 未实现能力 | 推荐端点实际返回 501 NOT_IMPLEMENTED |
| 测试数据 | 集成测试事务回滚；逐表查询确认全部 25 张业务表均为 0 行 |
| 重复初始化 | 集成测试建表后独立 JAR 再次启动成功，IF NOT EXISTS 未影响已有表 |
| 本机配置隔离 | git check-ignore 确认密码配置、日志和 target 均被忽略 |
| 凭据扫描 | 可被 Git 纳入的项目文件中未发现本机数据库密码 |
| Git 操作 | 未提交、未推送、未修改历史；README 原业务需求被保留 |

健康接口实际响应（省略请求时间）：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "status": "UP",
    "database": "zhigangzong",
    "databaseVersion": "8.0.45",
    "tableCount": 25
  }
}
```

## 已排查并解决的问题

1. **Maven 未加入 PATH**：发现已有 `D:\maven` 安装，使用现有 Maven；项目脚本提供 PATH、MAVEN_HOME 和本机安装路径检测，无需修改系统环境。
2. **受限执行环境无法正常调用本机工具或网络**：通过针对本任务的授权执行 Java、Maven、MySQL 和本地 HTTP 验证；未更改系统安全配置。
3. **MySQL 认证失败**：空密码及首次提供的密码均出现 ERROR 1045。经用户更正密码后，客户端与 JDBC 均连接成功；未重置账号密码。
4. **PowerShell 解析 Java 参数**：未加引号的 `-Dfile.encoding=UTF-8` 导致主类解析错误。改为 `'-Dfile.encoding=UTF-8'`，启动脚本使用该写法。
5. **缺少必填查询参数误报 500**：自动化测试发现 `GET /api/recommendations` 缺少 studentId 时落入通用异常处理。增加 MissingServletRequestParameterException 的 400 映射后重新测试，12 项全部通过。
6. **依赖解析**：复用本机 Maven 镜像与缓存，下载缺失依赖后构建成功，没有替换系统 Maven 配置。

## 覆盖范围及限制

- 4 个测试类合计 12 个测试：分页 2 个、接口契约 4 个、学生角色与日期约束 2 个、实际 MySQL 集成测试 4 个。
- 集成测试覆盖全部模块列表与缺失详情、基础创建链路、操作审计、唯一键和外键异常、默认状态、健康检查与统计接口。
- 外部 HTTP 检查覆盖应用存活、真实数据库健康、模块清单、统计和 25 个业务列表接口。
- 不代表已完成全部业务流程、认证授权、前端或生产部署。详细范围见 [第一版说明](first-draft.md)。
- 服务在交付验证时保持运行；电脑重启或进程结束后，执行 `scripts/start.ps1` 重新启动。
- 测试记录保存在被忽略的 `.local/verify.log`、`.local/runtime.log` 与 `target/surefire-reports/`，不将运行日志或数据库凭据纳入版本库。

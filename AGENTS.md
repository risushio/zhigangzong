# Project Instructions

## Project Context

智岗踪采用 Java 17、Spring Boot、MyBatis/MySQL 与原生 JavaScript。应用根目录包含 `pom.xml`、`package.json` 和独立 `.git/`。从此目录执行项目命令；不要在外层工作区仓库发布。

运行与验证方法见 `docs/local-workflow.md`。开发计划继续维护于 `docs/NEXT-STEPS.md`，已发布的 1.0.0 说明位于 `docs/releases/1.0.0.md`。尊重已有未提交修改。

## Project Progress

This project uses the `project-progress` skill at `.agents/skills/project-progress/SKILL.md`.

- 开发开始时读取该 Skill；每完成并验证一个独立功能、改进、Bug 修复或文档变更，立即更新根目录 `complete.md`，然后继续开发。
- Only verified completed work may be recorded. 验证失败、未执行或关键验证被跳过时，修复或报告阻塞，不得登记完成。
- 不写入 TODO、计划、未完成工作、调试日志、Agent 内部推理或逐文件修改流水账。
- 写入前读取现有内容；同一功能更新原条目，独立 Bug 才另记，避免重复。
- `complete.md` 只保存尚未发布的完成记录；`CHANGELOG.md` 永久保存版本历史，只新增版本，不覆盖旧记录。
- 收到明确的 `发布 vX.Y.Z` 或 `release vX.Y.Z` 指令时，使用该 Skill 完成验证、发布说明、CHANGELOG、提交/tag/推送、GitHub Release 和成功后的归档。
- Never publish a GitHub Release unless the user explicitly requests a release. 仅准备说明或讨论版本不授权发布。
- GitHub Release 创建失败或成功状态不明确时，保留 `complete.md`；只有确认对应版本已成功发布才清空本次快照中的条目。
- 不忽略失败测试，不泄露 API Key、Token、密码或 `.env` 内容，不强制推送、不删除远程分支、不覆盖已有 tag。

## Verification

- 前端：`npm run check`、`npm test`。
- 后端：`.\scripts\maven.ps1 verify`（包装器支持本机 Maven/Java 路径）。
- 数据库业务：`.\scripts\test-mysql.ps1`，显式启用 MySQL 集成测试。
- 运行验证：后端启动后执行 `.\scripts\smoke.ps1`；涉及交互的变更还需验证对应页面流程。
- 纯文档/Skill 修改：验证格式、路径、链接、流程一致性；Skill 使用内置 `quick_validate.py`。无需仅因文档改动启动整个应用。
- 发布要求前后端检查、MySQL 集成验证及候选构建运行检查全部通过，命令退出码和关键测试跳过状态均需核实。

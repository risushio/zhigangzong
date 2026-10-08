# 学生 Excel 导入导出验收

更新日期：2026-10-08。

学校管理员“Excel 导入导出”下载模板，填写已有本校 STUDENT 用户的 ID，为其新增学生档案。导入不创建用户/登录账号、不覆盖已有档案。其他角色拒绝访问，导出仅本校档案。

`GET /api/portal/student-sheet/export?template=true` 下载空模板，未提供 template 下载本校档案，最多 10000 行（超过则拒绝，不静默截断）。`POST /api/portal/student-sheet/import?apply=false` 校验上传的 `file`；`apply=true` 重新校验并提交整批。返回 `validRows`、逐行 `errors`、`imported` 与 `applied`。

模板单工作表 Students，表头顺序：userId、studentNo、major、skills、preferredCity、availableFrom、availableTo、daysPerWeek、projectExperience、resumeRef。用户 ID 与学号建议用文本；日期使用 YYYY-MM-DD，天数为 1–7。userId、studentNo、major 必填；其余可留空。仅接受非空、2 MB 内 XLSX，每批最多 1000 数据行。

每行校验字段长度、角色、本校归属、日期顺序、天数、文件内/本校学号重复、用户重复及已有档案。公式、错误/布尔单元格、宏、外部链接、额外工作表/列或错误表头拒绝。任一行错误则全批不写入，显示 Excel 行号与原因；成功写入事务与审计记录。确认前再次校验，避免预览后其他操作造成重复。导出全部采用文本单元格，避免把用户数据作为公式执行，也保留学号前导零。

验证：前端 12 项、MySQL verify 66 项全通过无跳过及构建成功；覆盖跨校用户、重复用户、错误行全批拒绝、预览不写入、公式拒绝、有效批次写入、已有档案重导拒绝、损坏文件、权限及导出文本单元格。

真实浏览器上传错误样例返回有效 1/错误 1/导入 0，第 3 行格式错误且确认按钮禁用；有效样例返回有效 1/错误 0，确认后新增用户 8760 的档案 1 行。实际下载 `students.xlsx`，用 openpyxl 检查表头、该用户的档案、学号 `000-THIRD-8760`、专业与天数，所有单元格无公式。截图 `.local/student-sheet-verified.png`。用户和文件为本地演示数据。

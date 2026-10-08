package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.vo.ModuleInfo;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {
    @GetMapping
    public ApiResponse<List<ModuleInfo>> modules() {
        return ApiResponse.ok(List.of(
                new ModuleInfo("01", "身份与权限", "管理员初始化、学生/教师/招聘/企业导师账号开通与登录、CSRF、本人/企业/指导关系业务隔离", "学院管理员、账号维护与历史管理接口学校隔离"),
                new ModuleInfo("02", "学生档案", "档案创建与本人编辑、分页详情、投递时确认简历引用分享", "个人简历上传、选用文件投递；撤回申请后终止招聘文件访问"),
                new ModuleInfo("03", "企业与基地审核", "资料新增/编辑、审核、暂停合作及关联岗位下架", "资质材料上传、企业端自主维护"),
                new ModuleInfo("04", "岗位管理", "新增/编辑、审核、发布/下架、编辑后重新审核", "企业端权限与招聘流程衔接"),
                new ModuleInfo("05", "岗位搜索", "关键词、专业、技能、城市和实习时间组合筛选、本人收藏", "企业端岗位自主维护"),
                new ModuleInfo("06", "基础智能匹配", "本人/本校可解释规则推荐、适配分/缺口/冲突、反馈提交与学校答复", "更细的专业映射与规则调优"),
                new ModuleInfo("07", "投递与录用", "投递、面试、录用、学生确认/撤回与完整状态历史", "招聘高级筛选、面试改期"),
                new ModuleInfo("08", "自主实习申报", "自主单位/岗位填报、草稿、提交、退回补充、学校审批与快照历史", "学院流程规则与自主单位核验规则配置"),
                new ModuleInfo("09", "实习计划", "批次创建、双导师分配/改派、范围规则与评分执行", "阶段任务与计划编辑"),
                new ModuleInfo("10", "入岗审批与材料", "平台/自主申报独立学校审批、协议/保险/成果上传审核、到岗确认", "材料批量审核与资质专项核验"),
                new ModuleInfo("11", "过程记录", "周报草稿保存、提交、教师批阅/退回、补充重提与内容快照历史", "阶段任务、周报附件与批量批阅"),
                new ModuleInfo("12", "出勤与变更", "按需签到/请假/补签、延期/换岗/换单位/终止审批与完整历史", "批量办理与复杂考勤日历"),
                new ModuleInfo("13", "指导与联系", "导师记录实际联系，支持联系阈值预警", "指导任务与联系附件"),
                new ModuleInfo("14", "异常预警与求助", "学生求助、责任分派、跟进结果、学生确认与学校复核、批次规则预警", "更多异常规则与批量处理"),
                new ModuleInfo("15", "多方评价", "三方草稿/提交、权重/及格线、退回补充与学校成绩复核", "评价量表与批量复核"),
                new ModuleInfo("16", "结项与归档", "结项门槛审核、冻结档案与原件 ZIP 导出", "批量归档与保留期限"),
                new ModuleInfo("17", "通知与待办", "业务事件通知、材料/周报截止定时提醒、本人已读及待办完成状态", "外部消息渠道"),
                new ModuleInfo("18", "统计与报表", "批次落实/完成/专业匹配/材料缺失统计及口径、本校学生 Excel 校验导入导出", "更多报表维度和历史冻结名册"),
                new ModuleInfo("19", "流程配置", "学院/专业/批次审批条件、周报周期、签到、材料和评分规则配置/执行/版本历史", "学院管理员与更复杂审批步骤"),
                new ModuleInfo("20", "数据保护与操作留痕", "私有文件授权下载、校验值、上传与访问审计、凭据隔离", "访问审计、隐私告知、授权分享、保留期限与备份恢复")
        ));
    }
}

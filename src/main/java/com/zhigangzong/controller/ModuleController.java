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
                new ModuleInfo("05", "岗位搜索", "关键词、城市、审核状态筛选与分页", "专业/技能/时间匹配筛选、收藏操作"),
                new ModuleInfo("06", "基础智能匹配", "推荐接口契约及反馈查询骨架，推荐返回 501", "可解释规则评分、硬条件冲突与能力缺口"),
                new ModuleInfo("07", "投递与录用", "投递、面试、录用、学生确认/撤回与完整状态历史", "招聘高级筛选、面试改期"),
                new ModuleInfo("08", "自主实习申报", "自主单位/岗位填报、草稿、提交、退回补充、学校审批与快照历史", "学院流程规则与自主单位核验规则配置"),
                new ModuleInfo("09", "实习计划", "批次创建、分页与详情；双导师分配、改派历史", "计划编辑、任务与评分规则执行"),
                new ModuleInfo("10", "入岗审批与材料", "平台/自主申报独立学校审批、协议/保险/成果上传审核、到岗确认", "材料要求配置与自动完整性检查"),
                new ModuleInfo("11", "过程记录", "周报草稿保存、提交、教师批阅/退回、补充重提与内容快照历史", "阶段任务、周报附件关联与频率配置"),
                new ModuleInfo("12", "出勤与变更", "出勤、补签、申诉类型及保留原始/申请快照的变更查询骨架", "按需签到、请假、变更审批与执行"),
                new ModuleInfo("13", "指导与联系", "双导师联系记录查询骨架", "新增指导记录、定期联系提醒"),
                new ModuleInfo("14", "异常预警与求助", "责任人、处理结果及独立跟进记录查询骨架", "规则预警、求助提交、分派、处理及复核"),
                new ModuleInfo("15", "多方评价", "自评/教师/企业评价查询骨架", "提交、评分权重、退回与成绩复核"),
                new ModuleInfo("16", "结项与归档", "结项记录查询骨架", "完成审核、档案打包与材料批量导出"),
                new ModuleInfo("17", "通知与待办", "招聘、审批、导师分配、到岗及周报事件通知、本人查询及标记已读", "材料截止与周报提醒、待办完成"),
                new ModuleInfo("18", "统计与报表", "基础数据计数概览", "带批次范围及明确分母的业务比率、Excel 导入导出"),
                new ModuleInfo("19", "流程配置", "学院/专业/批次规则查询骨架", "配置编辑、规则校验与流程执行"),
                new ModuleInfo("20", "数据保护与操作留痕", "私有文件授权下载、校验值、上传与访问审计、凭据隔离", "访问审计、隐私告知、授权分享、保留期限与备份恢复")
        ));
    }
}

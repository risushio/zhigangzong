export const labels={
  "id": "记录编号",
  "createdAt": "创建时间",
  "name": "名称",
  "code": "编码",
  "schoolId": "所属学校",
  "departmentId": "所属学院",
  "displayName": "姓名",
  "role": "用户角色",
  "email": "邮箱",
  "userId": "学生用户",
  "studentNo": "学号",
  "major": "专业",
  "skills": "技能标签",
  "projectExperience": "项目经历",
  "resumeRef": "简历引用",
  "preferredCity": "意向城市",
  "availableFrom": "可实习开始日期",
  "availableTo": "可实习结束日期",
  "daysPerWeek": "每周到岗天数",
  "creditCode": "统一社会信用代码",
  "contactName": "联系人",
  "contactPhone": "联系电话",
  "qualificationRef": "资质材料引用",
  "cooperationNotes": "合作说明",
  "inspectionNotes": "考察记录",
  "reviewStatus": "审核状态",
  "suspensionReason": "暂停原因",
  "enterpriseId": "合作企业",
  "title": "标题",
  "description": "详细说明",
  "requiredMajor": "专业要求",
  "requiredSkills": "技能要求",
  "city": "工作城市",
  "startDate": "开始日期",
  "endDate": "结束日期",
  "headcount": "招聘人数",
  "monthlyPay": "月报酬（元）",
  "workingHours": "工作时间",
  "applicationDeadline": "申请截止日期",
  "publishStatus": "发布状态",
  "studentId": "学生",
  "jobId": "岗位",
  "recruitmentStatus": "招聘状态",
  "interviewAt": "面试时间",
  "offerDetails": "录用说明",
  "studentConfirmed": "学生已确认",
  "applicationId": "申请编号",
  "actorId": "操作人",
  "fromStatus": "原状态",
  "toStatus": "新状态",
  "note": "备注",
  "learningObjectives": "培养目标",
  "taskRequirements": "任务要求",
  "materialRequirements": "材料要求",
  "gradingCriteria": "评分标准",
  "batchId": "实习批次",
  "source": "申报来源",
  "positionTitle": "实习岗位",
  "schoolApprovalStatus": "学校审批状态",
  "arrivalStatus": "到岗状态",
  "teacherId": "校内导师",
  "enterpriseMentorId": "企业导师",
  "placementId": "实习记录",
  "approverId": "审批人",
  "decision": "审批结果",
  "comment": "意见",
  "materialType": "材料类型",
  "fileRef": "材料引用",
  "reviewComment": "审核意见",
  "periodStart": "周期开始",
  "periodEnd": "周期结束",
  "content": "内容",
  "attachmentRef": "附件引用",
  "status": "状态",
  "reviewerId": "审核人",
  "feedback": "反馈",
  "attendanceDate": "出勤日期",
  "recordType": "记录类型",
  "changeType": "变更类型",
  "originalSnapshot": "变更前记录",
  "requestedSnapshot": "申请变更内容",
  "reason": "原因",
  "mentorId": "导师",
  "contactAt": "联系时间",
  "nextContactAt": "下次联系时间",
  "alertType": "问题类型",
  "ownerId": "负责人",
  "resolution": "处理结果",
  "resolvedAt": "解决时间",
  "alertId": "异常编号",
  "result": "跟进结果",
  "evaluatorId": "评价人",
  "evaluationType": "评价类型",
  "score": "评分",
  "summaryRef": "总结报告引用",
  "appraisalRef": "实习鉴定引用",
  "archivedAt": "归档时间",
  "recipientId": "接收人",
  "businessType": "业务类型",
  "businessId": "业务编号",
  "dueAt": "截止时间",
  "readAt": "阅读时间",
  "completedAt": "完成时间",
  "attendanceEnabled": "启用签到",
  "reportFrequencyDays": "周报周期（天）",
  "approvalSteps": "审批步骤",
  "materialTemplate": "材料模板",
  "evaluationWeights": "评价权重",
  "action": "操作类型",
  "resourceType": "资源类型",
  "resourceId": "资源编号",
  "feedbackType": "反馈类型",
  "studentName": "学生姓名",
  "enterpriseName": "企业名称"
};
export const resources={
  "jobs": {
    "route": "jobs",
    "name": "岗位管理",
    "icon": "briefcase",
    "columns": [
      "title",
      "enterpriseName",
      "city",
      "requiredMajor",
      "reviewStatus",
      "publishStatus"
    ],
    "status": "reviewStatus",
    "states": [
      "PENDING",
      "APPROVED",
      "REJECTED"
    ],
    "create": true,
    "edit": true
  },
  "enterprises": {
    "route": "enterprises",
    "name": "企业与基地",
    "icon": "building",
    "columns": [
      "name",
      "creditCode",
      "contactName",
      "contactPhone",
      "reviewStatus"
    ],
    "status": "reviewStatus",
    "states": [
      "PENDING",
      "APPROVED",
      "REJECTED",
      "SUSPENDED"
    ],
    "create": true,
    "edit": true
  },
  "students": {
    "route": "students",
    "name": "学生档案",
    "icon": "users",
    "columns": [
      "studentName",
      "studentNo",
      "major",
      "preferredCity",
      "daysPerWeek"
    ],
    "status": "",
    "states": [],
    "create": true,
    "edit": false
  },
  "applications": {
    "route": "applications",
    "name": "投递与录用",
    "icon": "briefcase",
    "columns": [
      "studentId",
      "jobId",
      "recruitmentStatus",
      "interviewAt",
      "studentConfirmed"
    ],
    "status": "recruitmentStatus",
    "states": [
      "APPLIED",
      "INTERVIEW",
      "OFFERED",
      "ACCEPTED",
      "REJECTED",
      "WITHDRAWN"
    ],
    "create": false,
    "edit": false
  },
  "recruitment-events": {
    "route": "recruitment-events",
    "name": "招聘历史",
    "icon": "clock",
    "columns": [
      "applicationId",
      "fromStatus",
      "toStatus",
      "note",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "placements": {
    "route": "placements",
    "name": "实习审批",
    "icon": "clipboard",
    "columns": [
      "studentName",
      "positionTitle",
      "enterpriseName",
      "source",
      "schoolApprovalStatus",
      "arrivalStatus"
    ],
    "status": "schoolApprovalStatus",
    "states": [
      "DRAFT",
      "PENDING",
      "APPROVED",
      "RETURNED",
      "REJECTED"
    ],
    "create": false,
    "edit": false
  },
  "batches": {
    "route": "batches",
    "name": "实习计划",
    "icon": "clipboard",
    "columns": [
      "name",
      "departmentId",
      "startDate",
      "endDate",
      "learningObjectives"
    ],
    "status": "",
    "states": [],
    "create": true,
    "edit": false
  },
  "approvals": {
    "route": "approvals",
    "name": "审批记录",
    "icon": "shield",
    "columns": [
      "placementId",
      "approverId",
      "decision",
      "comment",
      "createdAt"
    ],
    "status": "decision",
    "states": [
      "APPROVED",
      "RETURNED",
      "REJECTED"
    ],
    "create": false,
    "edit": false
  },
  "materials": {
    "route": "materials",
    "name": "入岗材料",
    "icon": "book",
    "columns": [
      "placementId",
      "materialType",
      "reviewStatus",
      "reviewComment",
      "createdAt"
    ],
    "status": "reviewStatus",
    "states": [
      "PENDING",
      "APPROVED",
      "REJECTED"
    ],
    "create": false,
    "edit": false
  },
  "reports": {
    "route": "reports",
    "name": "周报与任务",
    "icon": "book",
    "columns": [
      "title",
      "placementId",
      "periodStart",
      "periodEnd",
      "status"
    ],
    "status": "status",
    "states": [
      "DRAFT",
      "SUBMITTED",
      "REVIEWED"
    ],
    "create": false,
    "edit": false
  },
  "attendance": {
    "route": "attendance",
    "name": "出勤记录",
    "icon": "clock",
    "columns": [
      "placementId",
      "attendanceDate",
      "recordType",
      "status",
      "note"
    ],
    "status": "status",
    "states": [
      "PENDING",
      "APPROVED",
      "REJECTED"
    ],
    "create": false,
    "edit": false
  },
  "changes": {
    "route": "changes",
    "name": "变更申请",
    "icon": "clipboard",
    "columns": [
      "placementId",
      "changeType",
      "reason",
      "status",
      "createdAt"
    ],
    "status": "status",
    "states": [
      "PENDING",
      "APPROVED",
      "REJECTED"
    ],
    "create": false,
    "edit": false
  },
  "guidance": {
    "route": "guidance",
    "name": "指导与联系",
    "icon": "users",
    "columns": [
      "placementId",
      "mentorId",
      "contactAt",
      "content",
      "nextContactAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "alerts": {
    "route": "alerts",
    "name": "异常与求助",
    "icon": "shield",
    "columns": [
      "studentId",
      "alertType",
      "description",
      "ownerId",
      "status"
    ],
    "status": "status",
    "states": [
      "OPEN",
      "IN_PROGRESS",
      "RESOLVED",
      "CLOSED"
    ],
    "create": false,
    "edit": false
  },
  "alert-follow-ups": {
    "route": "alert-follow-ups",
    "name": "异常跟进",
    "icon": "clock",
    "columns": [
      "alertId",
      "actorId",
      "content",
      "result",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "evaluations": {
    "route": "evaluations",
    "name": "多方评价",
    "icon": "chart",
    "columns": [
      "placementId",
      "evaluatorId",
      "evaluationType",
      "score",
      "status"
    ],
    "status": "status",
    "states": [
      "DRAFT",
      "SUBMITTED",
      "REVIEWED"
    ],
    "create": false,
    "edit": false
  },
  "archives": {
    "route": "archives",
    "name": "结项归档",
    "icon": "book",
    "columns": [
      "placementId",
      "status",
      "reviewerId",
      "archivedAt"
    ],
    "status": "status",
    "states": [
      "PENDING",
      "APPROVED"
    ],
    "create": false,
    "edit": false
  },
  "notifications": {
    "route": "notifications",
    "name": "通知与待办",
    "icon": "bell",
    "columns": [
      "title",
      "recipientId",
      "dueAt",
      "readAt",
      "completedAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "schools": {
    "route": "schools",
    "name": "学校管理",
    "icon": "building",
    "columns": [
      "name",
      "code",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": true,
    "edit": false
  },
  "departments": {
    "route": "departments",
    "name": "学院管理",
    "icon": "building",
    "columns": [
      "name",
      "schoolId",
      "code",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": true,
    "edit": false
  },
  "users": {
    "route": "users",
    "name": "用户档案",
    "icon": "users",
    "columns": [
      "displayName",
      "role",
      "schoolId",
      "departmentId",
      "email"
    ],
    "status": "role",
    "states": [
      "STUDENT",
      "TEACHER",
      "RECRUITER",
      "ENTERPRISE_MENTOR",
      "DEPARTMENT_ADMIN",
      "SCHOOL_ADMIN"
    ],
    "create": true,
    "edit": false
  },
  "workflow-rules": {
    "route": "workflow-rules",
    "name": "流程配置",
    "icon": "settings",
    "columns": [
      "departmentId",
      "major",
      "attendanceEnabled",
      "reportFrequencyDays"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "audit-logs": {
    "route": "audit-logs",
    "name": "操作日志",
    "icon": "clock",
    "columns": [
      "action",
      "resourceType",
      "resourceId",
      "description",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "job-favorites": {
    "route": "job-favorites",
    "name": "岗位收藏",
    "icon": "briefcase",
    "columns": [
      "studentId",
      "jobId",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  },
  "match-feedback": {
    "route": "match-feedback",
    "name": "匹配反馈",
    "icon": "spark",
    "columns": [
      "studentId",
      "jobId",
      "feedbackType",
      "reason",
      "createdAt"
    ],
    "status": "",
    "states": [],
    "create": false,
    "edit": false
  }
};
export const forms={
  "schools": [
    {
      "name": "name",
      "type": "text",
      "required": true,
      "max": 120
    },
    {
      "name": "code",
      "type": "text",
      "required": true,
      "max": 40
    }
  ],
  "departments": [
    {
      "name": "schoolId",
      "type": "select",
      "required": true,
      "options": "schools"
    },
    {
      "name": "name",
      "type": "text",
      "required": true,
      "max": 120
    },
    {
      "name": "code",
      "type": "text",
      "required": true,
      "max": 40
    }
  ],
  "users": [
    {
      "name": "schoolId",
      "type": "select",
      "required": true,
      "options": "schools"
    },
    {
      "name": "departmentId",
      "type": "select",
      "required": false,
      "options": "departments"
    },
    {
      "name": "displayName",
      "type": "text",
      "required": true,
      "max": 80
    },
    {
      "name": "role",
      "type": "enum",
      "required": true,
      "options": [
        "STUDENT",
        "TEACHER",
        "RECRUITER",
        "ENTERPRISE_MENTOR",
        "DEPARTMENT_ADMIN",
        "SCHOOL_ADMIN"
      ]
    },
    {
      "name": "email",
      "type": "email",
      "required": false,
      "max": 160
    }
  ],
  "students": [
    {
      "name": "userId",
      "type": "select",
      "required": true,
      "options": "users"
    },
    {
      "name": "studentNo",
      "type": "text",
      "required": true,
      "max": 40
    },
    {
      "name": "major",
      "type": "text",
      "required": true,
      "max": 100
    },
    {
      "name": "preferredCity",
      "type": "text",
      "required": false,
      "max": 100
    },
    {
      "name": "skills",
      "type": "text",
      "required": false,
      "max": 1000
    },
    {
      "name": "daysPerWeek",
      "type": "number",
      "required": false
    },
    {
      "name": "availableFrom",
      "type": "date",
      "required": false
    },
    {
      "name": "availableTo",
      "type": "date",
      "required": false
    },
    {
      "name": "projectExperience",
      "type": "textarea",
      "required": false,
      "max": 10000
    },
    {
      "name": "resumeRef",
      "type": "text",
      "required": false,
      "max": 500
    }
  ],
  "enterprises": [
    {
      "name": "name",
      "type": "text",
      "required": true,
      "max": 160
    },
    {
      "name": "creditCode",
      "type": "text",
      "required": true,
      "max": 32
    },
    {
      "name": "contactName",
      "type": "text",
      "required": false,
      "max": 80
    },
    {
      "name": "contactPhone",
      "type": "text",
      "required": false,
      "max": 40
    },
    {
      "name": "qualificationRef",
      "type": "text",
      "required": false,
      "max": 500
    },
    {
      "name": "cooperationNotes",
      "type": "textarea",
      "required": false,
      "max": 10000
    },
    {
      "name": "inspectionNotes",
      "type": "textarea",
      "required": false,
      "max": 10000
    }
  ],
  "jobs": [
    {
      "name": "title",
      "type": "text",
      "required": true,
      "max": 120
    },
    {
      "name": "enterpriseId",
      "type": "select",
      "required": true,
      "options": "enterprises"
    },
    {
      "name": "city",
      "type": "text",
      "required": true,
      "max": 100
    },
    {
      "name": "headcount",
      "type": "number",
      "required": true
    },
    {
      "name": "requiredMajor",
      "type": "text",
      "required": false,
      "max": 100
    },
    {
      "name": "requiredSkills",
      "type": "text",
      "required": false,
      "max": 1000
    },
    {
      "name": "monthlyPay",
      "type": "decimal",
      "required": false
    },
    {
      "name": "daysPerWeek",
      "type": "number",
      "required": false
    },
    {
      "name": "startDate",
      "type": "date",
      "required": false
    },
    {
      "name": "endDate",
      "type": "date",
      "required": false
    },
    {
      "name": "applicationDeadline",
      "type": "date",
      "required": false
    },
    {
      "name": "workingHours",
      "type": "text",
      "required": false,
      "max": 120
    },
    {
      "name": "description",
      "type": "textarea",
      "required": true,
      "max": 10000
    }
  ],
  "batches": [
    {
      "name": "name",
      "type": "text",
      "required": true,
      "max": 120
    },
    {
      "name": "departmentId",
      "type": "select",
      "required": true,
      "options": "departments"
    },
    {
      "name": "startDate",
      "type": "date",
      "required": true
    },
    {
      "name": "endDate",
      "type": "date",
      "required": true
    },
    {
      "name": "learningObjectives",
      "type": "textarea",
      "required": false,
      "max": 10000
    },
    {
      "name": "taskRequirements",
      "type": "textarea",
      "required": false,
      "max": 10000
    },
    {
      "name": "materialRequirements",
      "type": "textarea",
      "required": false,
      "max": 10000
    },
    {
      "name": "gradingCriteria",
      "type": "textarea",
      "required": false,
      "max": 10000
    }
  ]
};
export const groups=[
  {
    "label": "工作空间",
    "items": [
      [
        "dashboard",
        "工作台",
        "grid"
      ],
      [
        "notifications",
        "通知与待办",
        "bell"
      ]
    ]
  },
  {
    "label": "实习业务",
    "items": [
      [
        "students",
        "学生档案",
        "users"
      ],
      [
        "enterprises",
        "企业与基地",
        "building"
      ],
      [
        "jobs",
        "岗位管理",
        "briefcase"
      ],
      [
        "matching",
        "智能匹配",
        "spark"
      ],
      [
        "applications",
        "投递与录用",
        "clipboard"
      ],
      [
        "placements",
        "实习管理",
        "clipboard"
      ],
      [
        "reports",
        "过程跟踪",
        "book"
      ],
      [
        "alerts",
        "异常与求助",
        "shield"
      ],
      [
        "evaluations",
        "评价与归档",
        "chart"
      ]
    ]
  },
  {
    "label": "管理中心",
    "items": [
      [
        "statistics",
        "统计概览",
        "chart"
      ],
      [
        "schools",
        "组织与用户",
        "building"
      ],
      [
        "workflow-rules",
        "系统管理",
        "settings"
      ]
    ]
  }
];
export const tabGroups=[["jobs","job-favorites"],["applications","recruitment-events"],["placements","batches","approvals","materials"],["reports","attendance","changes","guidance"],["alerts","alert-follow-ups"],["evaluations","archives"],["schools","departments","users"],["workflow-rules","audit-logs","modules"],["matching","match-feedback"]];
export function formBody(form,fields){
 const body={};
 for(const field of fields){
  const raw=String(form.get(field.name)??'').trim();
  if(field.required && !raw)throw new Error((labels[field.name]||field.name)+'不能为空');
  if(!raw){body[field.name]=null;continue;}
  if(['number','decimal','select'].includes(field.type)){
   const n=Number(raw);
   if(!Number.isFinite(n)||(field.type!=='decimal'&&!Number.isSafeInteger(n)))throw new Error('请输入有效的数值');
   if(field.type==='select'&&n<1)throw new Error('请选择有效记录');
   if(field.name==='daysPerWeek'&&(n<1||n>7))throw new Error('每周到岗天数应为 1–7');
   body[field.name]=n;
  }else body[field.name]=raw;
 }
 const from=body.startDate||body.availableFrom,to=body.endDate||body.availableTo;
 if(from && to && to<from)throw new Error('结束日期不能早于开始日期');
 return body;
}

package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface WorkflowService {
    PageResult<Notification> listNotification(int page, int size);
    Notification getNotification(long id);

    PageResult<WorkflowRule> listWorkflowRule(int page, int size);
    WorkflowRule getWorkflowRule(long id);

    PageResult<AuditLog> listAuditLog(int page, int size);
    AuditLog getAuditLog(long id);
}

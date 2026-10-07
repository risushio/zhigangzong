package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkflowServiceImpl implements WorkflowService {
    private final NotificationMapper notificationMapper;
    private final WorkflowRuleMapper workflowRuleMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<Notification> listNotification(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(notificationMapper.findPage(query.offset(), query.size()),
                notificationMapper.count(), page, size);
    }

    @Override
    public Notification getNotification(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        Notification entity = notificationMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("通知与待办");
        }
        return entity;
    }

    @Override
    public PageResult<WorkflowRule> listWorkflowRule(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(workflowRuleMapper.findPage(query.offset(), query.size()),
                workflowRuleMapper.count(), page, size);
    }

    @Override
    public WorkflowRule getWorkflowRule(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        WorkflowRule entity = workflowRuleMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("专业批次流程配置");
        }
        return entity;
    }

    @Override
    public PageResult<AuditLog> listAuditLog(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(auditLogMapper.findPage(query.offset(), query.size()),
                auditLogMapper.count(), page, size);
    }

    @Override
    public AuditLog getAuditLog(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        AuditLog entity = auditLogMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("操作留痕");
        }
        return entity;
    }
}

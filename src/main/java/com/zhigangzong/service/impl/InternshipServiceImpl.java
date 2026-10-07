package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.InternshipService;
import com.zhigangzong.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternshipServiceImpl implements InternshipService {
    private final InternshipBatchMapper internshipBatchMapper;
    private final InternshipPlacementMapper internshipPlacementMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final InternshipMaterialMapper internshipMaterialMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<InternshipBatch> listInternshipBatch(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(internshipBatchMapper.findPage(query.offset(), query.size()),
                internshipBatchMapper.count(), page, size);
    }

    @Override
    public InternshipBatch getInternshipBatch(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        InternshipBatch entity = internshipBatchMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("实习计划批次");
        }
        return entity;
    }

    @Override
    @Transactional
    public InternshipBatch createInternshipBatch(CreateInternshipBatchRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
        InternshipBatch entity = new InternshipBatch();
        entity.setDepartmentId(request.departmentId());
        entity.setName(request.name());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setLearningObjectives(request.learningObjectives());
        entity.setTaskRequirements(request.taskRequirements());
        entity.setMaterialRequirements(request.materialRequirements());
        entity.setGradingCriteria(request.gradingCriteria());
        internshipBatchMapper.insert(entity);
        auditLogMapper.recordCreation("internship_batch", entity.getId());
        return getInternshipBatch(entity.getId());
    }

    @Override
    public PageResult<InternshipPlacement> listInternshipPlacement(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(internshipPlacementMapper.findPage(query.offset(), query.size()),
                internshipPlacementMapper.count(), page, size);
    }

    @Override
    public InternshipPlacement getInternshipPlacement(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        InternshipPlacement entity = internshipPlacementMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("自主申报与学校入岗审批");
        }
        return entity;
    }

    @Override
    public PageResult<ApprovalRecord> listApprovalRecord(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(approvalRecordMapper.findPage(query.offset(), query.size()),
                approvalRecordMapper.count(), page, size);
    }

    @Override
    public ApprovalRecord getApprovalRecord(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        ApprovalRecord entity = approvalRecordMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("审批历史");
        }
        return entity;
    }

    @Override
    public PageResult<InternshipMaterial> listInternshipMaterial(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(internshipMaterialMapper.findPage(query.offset(), query.size()),
                internshipMaterialMapper.count(), page, size);
    }

    @Override
    public InternshipMaterial getInternshipMaterial(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        InternshipMaterial entity = internshipMaterialMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("协议保险等入岗材料");
        }
        return entity;
    }
}

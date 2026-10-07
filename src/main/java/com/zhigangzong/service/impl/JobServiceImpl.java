package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.JobService;
import com.zhigangzong.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {
    private final JobPositionMapper jobPositionMapper;
    private final JobFavoriteMapper jobFavoriteMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<JobPosition> listJobPosition(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(jobPositionMapper.findPage(query.offset(), query.size()),
                jobPositionMapper.count(), page, size);
    }

    @Override
    public JobPosition getJobPosition(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        JobPosition entity = jobPositionMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("岗位");
        }
        return entity;
    }

    @Override
    @Transactional
    public JobPosition createJobPosition(CreateJobPositionRequest request) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
        JobPosition entity = new JobPosition();
        entity.setEnterpriseId(request.enterpriseId());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setRequiredMajor(request.requiredMajor());
        entity.setRequiredSkills(request.requiredSkills());
        entity.setCity(request.city());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDaysPerWeek(request.daysPerWeek());
        entity.setHeadcount(request.headcount());
        entity.setMonthlyPay(request.monthlyPay());
        entity.setWorkingHours(request.workingHours());
        entity.setApplicationDeadline(request.applicationDeadline());
        jobPositionMapper.insert(entity);
        auditLogMapper.recordCreation("job_position", entity.getId());
        return getJobPosition(entity.getId());
    }

    @Override
    public PageResult<JobFavorite> listJobFavorite(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(jobFavoriteMapper.findPage(query.offset(), query.size()),
                jobFavoriteMapper.count(), page, size);
    }

    @Override
    public JobFavorite getJobFavorite(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        JobFavorite entity = jobFavoriteMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("岗位收藏");
        }
        return entity;
    }
}

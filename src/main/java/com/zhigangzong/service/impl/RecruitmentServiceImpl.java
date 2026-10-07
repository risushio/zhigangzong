package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentServiceImpl implements RecruitmentService {
    private final JobApplicationMapper jobApplicationMapper;
    private final RecruitmentEventMapper recruitmentEventMapper;

    @Override
    public PageResult<JobApplication> listJobApplication(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(jobApplicationMapper.findPage(query.offset(), query.size()),
                jobApplicationMapper.count(), page, size);
    }

    @Override
    public JobApplication getJobApplication(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        JobApplication entity = jobApplicationMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("投递与录用");
        }
        return entity;
    }

    @Override
    public PageResult<RecruitmentEvent> listRecruitmentEvent(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(recruitmentEventMapper.findPage(query.offset(), query.size()),
                recruitmentEventMapper.count(), page, size);
    }

    @Override
    public RecruitmentEvent getRecruitmentEvent(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        RecruitmentEvent entity = recruitmentEventMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("招聘状态历史");
        }
        return entity;
    }
}

package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationServiceImpl implements EvaluationService {
    private final InternshipEvaluationMapper internshipEvaluationMapper;
    private final ArchiveRecordMapper archiveRecordMapper;

    @Override
    public PageResult<InternshipEvaluation> listInternshipEvaluation(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(internshipEvaluationMapper.findPage(query.offset(), query.size()),
                internshipEvaluationMapper.count(), page, size);
    }

    @Override
    public InternshipEvaluation getInternshipEvaluation(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        InternshipEvaluation entity = internshipEvaluationMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("多方评价");
        }
        return entity;
    }

    @Override
    public PageResult<ArchiveRecord> listArchiveRecord(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(archiveRecordMapper.findPage(query.offset(), query.size()),
                archiveRecordMapper.count(), page, size);
    }

    @Override
    public ArchiveRecord getArchiveRecord(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        ArchiveRecord entity = archiveRecordMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("结项与归档");
        }
        return entity;
    }
}

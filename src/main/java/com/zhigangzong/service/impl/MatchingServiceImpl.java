package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingServiceImpl implements MatchingService {
    private final com.zhigangzong.service.JobMatchingService matching;
    @Override
    public java.util.List<com.zhigangzong.vo.JobRecommendation> recommendations(long studentId) {
        if (studentId < 1) {
            throw BusinessException.badRequest("studentId 必须大于 0");
        }
        return matching.recommendations(studentId);
    }

    private final MatchFeedbackMapper matchFeedbackMapper;

    @Override
    public PageResult<MatchFeedback> listMatchFeedback(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(matchFeedbackMapper.findPage(query.offset(), query.size()),
                matchFeedbackMapper.count(), page, size);
    }

    @Override
    public MatchFeedback getMatchFeedback(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        MatchFeedback entity = matchFeedbackMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("推荐解释反馈");
        }
        return entity;
    }
}

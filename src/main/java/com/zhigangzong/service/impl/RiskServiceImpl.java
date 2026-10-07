package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskServiceImpl implements RiskService {
    private final RiskAlertMapper riskAlertMapper;
    private final RiskFollowUpMapper riskFollowUpMapper;

    @Override
    public PageResult<RiskAlert> listRiskAlert(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(riskAlertMapper.findPage(query.offset(), query.size()),
                riskAlertMapper.count(), page, size);
    }

    @Override
    public RiskAlert getRiskAlert(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        RiskAlert entity = riskAlertMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("异常预警与求助");
        }
        return entity;
    }

    @Override
    public PageResult<RiskFollowUp> listRiskFollowUp(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(riskFollowUpMapper.findPage(query.offset(), query.size()),
                riskFollowUpMapper.count(), page, size);
    }

    @Override
    public RiskFollowUp getRiskFollowUp(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        RiskFollowUp entity = riskFollowUpMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("异常跟进记录");
        }
        return entity;
    }
}

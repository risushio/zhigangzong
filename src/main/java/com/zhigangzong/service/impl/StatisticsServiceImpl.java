package com.zhigangzong.service.impl;

import com.zhigangzong.mapper.StatisticsMapper;
import com.zhigangzong.service.StatisticsService;
import com.zhigangzong.vo.StatisticsOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public StatisticsOverview overview() {
        return mapper.overview();
    }
}

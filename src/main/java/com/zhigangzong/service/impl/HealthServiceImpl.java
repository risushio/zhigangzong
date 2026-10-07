package com.zhigangzong.service.impl;

import com.zhigangzong.mapper.HealthMapper;
import com.zhigangzong.service.HealthService;
import com.zhigangzong.vo.HealthStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {
    private final HealthMapper mapper;

    @Override
    public HealthStatus checkDatabase() {
        if (mapper.ping() != 1) {
            throw new DataAccessResourceFailureException("Database probe returned an unexpected value");
        }
        return new HealthStatus("UP", mapper.database(), mapper.version(), mapper.tableCount());
    }
}

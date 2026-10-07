package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.EnterpriseService;
import com.zhigangzong.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnterpriseServiceImpl implements EnterpriseService {
    private final EnterpriseMapper enterpriseMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<Enterprise> listEnterprise(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(enterpriseMapper.findPage(query.offset(), query.size()),
                enterpriseMapper.count(), page, size);
    }

    @Override
    public Enterprise getEnterprise(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        Enterprise entity = enterpriseMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("企业与基地");
        }
        return entity;
    }

    @Override
    @Transactional
    public Enterprise createEnterprise(CreateEnterpriseRequest request) {
        Enterprise entity = new Enterprise();
        entity.setName(request.name());
        entity.setCreditCode(request.creditCode());
        entity.setContactName(request.contactName());
        entity.setContactPhone(request.contactPhone());
        entity.setQualificationRef(request.qualificationRef());
        entity.setCooperationNotes(request.cooperationNotes());
        entity.setInspectionNotes(request.inspectionNotes());
        enterpriseMapper.insert(entity);
        auditLogMapper.recordCreation("enterprise", entity.getId());
        return getEnterprise(entity.getId());
    }
}

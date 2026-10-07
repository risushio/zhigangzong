package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.OrganizationService;
import com.zhigangzong.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {
    private final SchoolMapper schoolMapper;
    private final DepartmentMapper departmentMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<School> listSchool(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(schoolMapper.findPage(query.offset(), query.size()),
                schoolMapper.count(), page, size);
    }

    @Override
    public School getSchool(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        School entity = schoolMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("学校");
        }
        return entity;
    }

    @Override
    @Transactional
    public School createSchool(CreateSchoolRequest request) {
        School entity = new School();
        entity.setName(request.name());
        entity.setCode(request.code());
        schoolMapper.insert(entity);
        auditLogMapper.recordCreation("school", entity.getId());
        return getSchool(entity.getId());
    }

    @Override
    public PageResult<Department> listDepartment(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(departmentMapper.findPage(query.offset(), query.size()),
                departmentMapper.count(), page, size);
    }

    @Override
    public Department getDepartment(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        Department entity = departmentMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("学院");
        }
        return entity;
    }

    @Override
    @Transactional
    public Department createDepartment(CreateDepartmentRequest request) {
        Department entity = new Department();
        entity.setSchoolId(request.schoolId());
        entity.setName(request.name());
        entity.setCode(request.code());
        departmentMapper.insert(entity);
        auditLogMapper.recordCreation("department", entity.getId());
        return getDepartment(entity.getId());
    }

    @Override
    public PageResult<UserAccount> listUserAccount(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(userAccountMapper.findPage(query.offset(), query.size()),
                userAccountMapper.count(), page, size);
    }

    @Override
    public UserAccount getUserAccount(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        UserAccount entity = userAccountMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("用户角色");
        }
        return entity;
    }

    @Override
    @Transactional
    public UserAccount createUserAccount(CreateUserAccountRequest request) {
        UserAccount entity = new UserAccount();
        entity.setSchoolId(request.schoolId());
        entity.setDepartmentId(request.departmentId());
        entity.setDisplayName(request.displayName());
        entity.setRole(request.role());
        entity.setEmail(request.email());
        userAccountMapper.insert(entity);
        auditLogMapper.recordCreation("user_account", entity.getId());
        return getUserAccount(entity.getId());
    }
}

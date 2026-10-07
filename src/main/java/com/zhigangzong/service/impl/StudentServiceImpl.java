package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.StudentService;
import com.zhigangzong.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {
    private final StudentProfileMapper studentProfileMapper;
    private final AuditLogMapper auditLogMapper;
    private final UserAccountMapper userAccountMapper;

    @Override
    public PageResult<StudentProfile> listStudentProfile(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(studentProfileMapper.findPage(query.offset(), query.size()),
                studentProfileMapper.count(), page, size);
    }

    @Override
    public StudentProfile getStudentProfile(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        StudentProfile entity = studentProfileMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("学生档案");
        }
        return entity;
    }

    @Override
    @Transactional
    public StudentProfile createStudentProfile(CreateStudentProfileRequest request) {
        if (request.availableFrom() != null && request.availableTo() != null
                && request.availableTo().isBefore(request.availableFrom())) {
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
        UserAccount user = userAccountMapper.findById(request.userId());
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        if (!"STUDENT".equals(user.getRole())) {
            throw BusinessException.badRequest("学生档案必须关联 STUDENT 角色用户");
        }
        StudentProfile entity = new StudentProfile();
        entity.setUserId(request.userId());
        entity.setStudentNo(request.studentNo());
        entity.setMajor(request.major());
        entity.setSkills(request.skills());
        entity.setProjectExperience(request.projectExperience());
        entity.setResumeRef(request.resumeRef());
        entity.setPreferredCity(request.preferredCity());
        entity.setAvailableFrom(request.availableFrom());
        entity.setAvailableTo(request.availableTo());
        entity.setDaysPerWeek(request.daysPerWeek());
        studentProfileMapper.insert(entity);
        auditLogMapper.recordCreation("student_profile", entity.getId());
        return getStudentProfile(entity.getId());
    }
}

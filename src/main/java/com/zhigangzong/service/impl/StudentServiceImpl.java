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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import jakarta.validation.Validator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {
    private final StudentProfileMapper studentProfileMapper;
    private final AuditLogMapper auditLogMapper;
    private final UserAccountMapper userAccountMapper;
    private final Validator validator;

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

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = false)
    public StudentProfile createStandaloneStudentProfile(CreateStandaloneStudentProfileRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        var actor = username == null ? null : studentProfileMapper.administrator(username);
        if (actor == null) throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无学生建档权限");
        // Keep the same school -> administrator lock order as draft confirmation.
        studentProfileMapper.lockSchool(actor.schoolId());
        var current = studentProfileMapper.lockAdministrator(username);
        if (current == null || current.schoolId() != actor.schoolId())
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "学生建档权限已撤销");
        if (request == null || !validator.validate(request).isEmpty())
            throw BusinessException.badRequest("学生档案字段类型、长度或格式不合法");
        if (request.availableFrom() != null && request.availableTo() != null
                && request.availableTo().isBefore(request.availableFrom()))
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        if (request.departmentId() != null && studentProfileMapper.departmentInSchool(request.departmentId(), current.schoolId()) != 1)
            throw BusinessException.notFound("本校学院");
        String studentNo = request.studentNo().trim();
        if (studentProfileMapper.countStudentNumber(current.schoolId(), studentNo) > 0)
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_STUDENT_NO", "本校学号已存在");
        var entity = new StudentProfile();
        entity.setSchoolId(current.schoolId());
        entity.setName(request.name().trim());
        entity.setStudentNo(studentNo);
        entity.setMajor(request.major().trim());
        entity.setDepartmentId(request.departmentId());
        entity.setPhone(request.phone());
        entity.setGradeYear(request.gradeYear());
        entity.setClassName(request.className() == null ? null : request.className().trim());
        entity.setSkills(request.skills());
        entity.setProjectExperience(request.projectExperience());
        entity.setPreferredCity(request.preferredCity());
        entity.setAvailableFrom(request.availableFrom());
        entity.setAvailableTo(request.availableTo());
        entity.setDaysPerWeek(request.daysPerWeek());
        studentProfileMapper.insert(entity);
        auditLogMapper.recordCreation("student_profile", entity.getId());
        return entity;
    }
}

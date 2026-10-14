package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 仅建档入口；事务由显式确认服务持有，不接收调用方身份或学校。 */
@Service @RequiredArgsConstructor
public class StudentProfileCreationService {
    private final AgentMapper mapper;
    private final StudentService students;
    private final AgentProperties properties;
    @Transactional(propagation=Propagation.MANDATORY)
    public long create(ObjectNode candidate) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        var actor=auth==null?null:mapper.actor(auth.getName());
        if(!properties.isEnabled()||actor==null||!"SCHOOL_ADMIN".equals(actor.role())) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","无学生建档权限");
        var fields=StudentDraftRules.normalize(candidate);
        if(!StudentDraftRules.validate(fields,actor.schoolId(),mapper).ready()) throw AgentStore.conflict("DRAFT_INVALID","学生资料未通过建档校验");
        return students.createStandaloneStudentProfile(StudentDraftRules.creationRequest(fields)).getId();
    }
}

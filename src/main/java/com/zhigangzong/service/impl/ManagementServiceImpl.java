package com.zhigangzong.service.impl;
import com.zhigangzong.dto.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.service.*;
import com.zhigangzong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
@Service @RequiredArgsConstructor @Transactional
public class ManagementServiceImpl implements ManagementService {
    private final ManagementMapper mapper;
    private final AuthMapper auth;
    private final EnterpriseMapper enterprises;
    private final JobPositionMapper jobs;

    private void audit(String action,String resource,long id,String note) {
        var account=auth.find(SecurityContextHolder.getContext().getAuthentication().getName());
        mapper.audit(account==null?null:((Number)account.get("id")).longValue(),action,resource,id,note);
    }
    private Enterprise enterprise(long id) {
        var e=mapper.lockEnterprise(id);
        if(e==null) throw BusinessException.notFound("企业");
        return e;
    }
    private JobPosition job(long id) {
        var j=mapper.lockJob(id);
        if(j==null) throw BusinessException.notFound("岗位");
        return j;
    }
    @Override public Enterprise updateEnterprise(long id,CreateEnterpriseRequest r) {
        var entity=enterprise(id);
        entity.setName(r.name());
        entity.setCreditCode(r.creditCode());
        entity.setContactName(r.contactName());
        entity.setContactPhone(r.contactPhone());
        entity.setQualificationRef(r.qualificationRef());
        entity.setCooperationNotes(r.cooperationNotes());
        entity.setInspectionNotes(r.inspectionNotes());
        mapper.updateEnterprise(entity);
        mapper.offlineEnterpriseJobs(id);
        audit("UPDATE","enterprise",id,"企业资料更新，重新进入待审核，关联岗位下架待复核");
        return enterprises.findById(id);
    }
    @Override public JobPosition updateJob(long id,CreateJobPositionRequest r) {
        enterprise(r.enterpriseId());
        var entity=job(id);
        if(!entity.getEnterpriseId().equals(r.enterpriseId()) && mapper.jobReferences(id)>0)
            throw BusinessException.badRequest("已有申请或实习记录的岗位不能更换所属企业，请另建岗位");
        if(r.startDate()!=null && r.endDate()!=null && r.endDate().isBefore(r.startDate())) throw BusinessException.badRequest("结束日期不能早于开始日期");
        entity.setEnterpriseId(r.enterpriseId());
        entity.setTitle(r.title());
        entity.setDescription(r.description());
        entity.setRequiredMajor(r.requiredMajor());
        entity.setRequiredSkills(r.requiredSkills());
        entity.setCity(r.city());
        entity.setStartDate(r.startDate());
        entity.setEndDate(r.endDate());
        entity.setDaysPerWeek(r.daysPerWeek());
        entity.setHeadcount(r.headcount());
        entity.setMonthlyPay(r.monthlyPay());
        entity.setWorkingHours(r.workingHours());
        entity.setApplicationDeadline(r.applicationDeadline());
        mapper.updateJob(entity);
        audit("UPDATE","job_position",id,"岗位资料更新，重新进入待审核草稿");
        return jobs.findById(id);
    }
    @Override public Enterprise reviewEnterprise(long id,ReviewRequest r) {
        var e=enterprise(id);
        if(r.decision().equals(e.getReviewStatus())) throw BusinessException.badRequest("企业已处于该状态");
        mapper.reviewEnterprise(id,r.decision(),"SUSPENDED".equals(r.decision())?r.note():null);
        if(!"APPROVED".equals(r.decision())) mapper.offlineEnterpriseJobs(id);
        audit("REVIEW","enterprise",id,r.decision()+": "+r.note());
        return enterprises.findById(id);
    }
    @Override public JobPosition reviewJob(long id,ReviewRequest r) {
        var snapshot=jobs.findById(id);
        if(snapshot==null) throw BusinessException.notFound("岗位");
        var e=enterprise(snapshot.getEnterpriseId());
        var j=job(id);
        if(!j.getEnterpriseId().equals(e.getId())) throw BusinessException.badRequest("岗位关联企业已变化，请刷新重试");
        if("SUSPENDED".equals(r.decision())) throw BusinessException.badRequest("岗位不支持此审核状态");
        if(!"PENDING".equals(j.getReviewStatus())) throw BusinessException.badRequest("仅待审核岗位可执行审核");
        if("APPROVED".equals(r.decision()) && !"APPROVED".equals(e.getReviewStatus())) throw BusinessException.badRequest("请先完成企业审核");
        mapper.reviewJob(id,r.decision());
        audit("REVIEW","job_position",id,r.decision()+": "+r.note());
        return jobs.findById(id);
    }
    @Override public JobPosition publication(long id,PublicationRequest r) {
        var snapshot=jobs.findById(id);
        if(snapshot==null) throw BusinessException.notFound("岗位");
        var e=enterprise(snapshot.getEnterpriseId());
        var j=job(id);
        if(!j.getEnterpriseId().equals(e.getId())) throw BusinessException.badRequest("岗位关联企业已变化，请刷新重试");
        if("PUBLISHED".equals(r.status())) {
            if(!"APPROVED".equals(j.getReviewStatus()) || !"APPROVED".equals(e.getReviewStatus())) throw BusinessException.badRequest("企业及岗位均审核通过后才能发布");
            if(j.getApplicationDeadline()!=null && j.getApplicationDeadline().isBefore(LocalDate.now())) throw BusinessException.badRequest("岗位申请截止日期已过");
        }
        mapper.publish(id,r.status());
        audit("PUBLICATION","job_position",id,r.status());
        return jobs.findById(id);
    }
}

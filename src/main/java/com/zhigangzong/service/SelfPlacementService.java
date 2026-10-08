package com.zhigangzong.service;
import com.zhigangzong.dto.PortalRequests.SelfPlacement;
import com.zhigangzong.entity.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class SelfPlacementService {
    private final PortalService portal;
    private final PortalMapper accounts;
    private final SelfPlacementMapper mapper;
    private final ManagementMapper audit;
    private void dates(SelfPlacement r,PortalMapper.Actor a) {
        var b=accounts.batch(r.batchId());
        if(b==null || !Objects.equals(b.getDepartmentId(),a.departmentId()))throw BusinessException.badRequest("请选择本学院批次");
        if(r.endDate().isBefore(r.startDate()) || r.startDate().isBefore(b.getStartDate()) || r.endDate().isAfter(b.getEndDate()))throw BusinessException.badRequest("实习日期必须有序且在批次范围内");
    }
    private long enterprise(SelfPlacement r) {
        var e=mapper.enterprise(r.creditCode());
        if(e!=null) {
            if(!e.getName().equals(r.enterpriseName().trim()))throw BusinessException.badRequest("该单位代码已存在，请核对单位全称");
            if("SUSPENDED".equals(e.getReviewStatus()) || "REJECTED".equals(e.getReviewStatus()))throw BusinessException.badRequest("该单位已暂停合作或审核未通过，请联系学校");
            return e.getId();
        }
        e=new Enterprise();e.setName(r.enterpriseName().trim());e.setCreditCode(r.creditCode());e.setContactName(r.contactName());e.setContactPhone(r.contactPhone());mapper.createEnterprise(e);return e.getId();
    }
    public InternshipPlacement create(SelfPlacement r) {
        var a=portal.actor("STUDENT");var s=accounts.student(a.id());if(s==null)throw BusinessException.badRequest("请先建立学生档案");
        accounts.lockStudent(s.getId());dates(r,a);
        if(accounts.duplicatePlacement(0,s.getId(),r.batchId())>0)throw BusinessException.badRequest("该批次已有实习记录，请查看已有记录");
        var p=new InternshipPlacement();p.setStudentId(s.getId());p.setBatchId(r.batchId());p.setEnterpriseId(enterprise(r));fill(p,r);mapper.create(p);mapper.insertDetail(p.getId(),r);mapper.event(p.getId(),a.id(),"DRAFT_SAVED");
        audit.audit(a.id(),"SELF_DRAFT","internship_placement",p.getId(),"保存自主实习申报草稿");return accounts.lockPlacement(p.getId());
    }
    private void fill(InternshipPlacement p,SelfPlacement r){p.setPositionTitle(r.positionTitle());p.setStartDate(r.startDate());p.setEndDate(r.endDate());}
    private void editable(InternshipPlacement p){portal.requireCurrent(p);if(!"SELF".equals(p.getSource()) || !List.of("DRAFT","RETURNED").contains(p.getSchoolApprovalStatus()) || !"NOT_ARRIVED".equals(p.getArrivalStatus()))throw BusinessException.badRequest("仅自主申报草稿和退回申请可修改或提交");}
    public InternshipPlacement save(long id,SelfPlacement r) {
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);editable(p);dates(r,a);
        if(!Objects.equals(p.getBatchId(),r.batchId()))throw BusinessException.badRequest("补充申报不能更换批次");
        p.setEnterpriseId(enterprise(r));fill(p,r);mapper.update(p);mapper.updateDetail(id,r);mapper.event(id,a.id(),"DRAFT_SAVED");audit.audit(a.id(),"SELF_DRAFT","internship_placement",id,"保存自主申报补充资料");return accounts.lockPlacement(id);
    }
    public InternshipPlacement submit(long id) {
        var a=portal.actor("STUDENT");var p=portal.accessiblePlacement(id,a);editable(p);
        var detail=mapper.detail(id);if(detail==null)throw BusinessException.badRequest("自主申报资料尚未填写");
        var e=mapper.enterprise((String)detail.get("creditCode"));if(e==null || List.of("SUSPENDED","REJECTED").contains(e.getReviewStatus()))throw BusinessException.badRequest("单位状态已变化，请联系学校核验");
        accounts.approval(id,"PENDING");mapper.event(id,a.id(),"SUBMITTED");accounts.notifySchool(a.schoolId(),id);audit.audit(a.id(),"SELF_SUBMIT","internship_placement",id,"提交自主实习学校审批");return accounts.lockPlacement(id);
    }
}

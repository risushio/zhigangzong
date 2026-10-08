package com.zhigangzong.service;

import com.zhigangzong.common.*;
import com.zhigangzong.dto.JobFilter;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.vo.JobRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class JobMatchingService {
    private final PortalService portal;
    private final PortalMapper actors;
    private final StudentProfileMapper students;
    private final UserAccountMapper users;
    private final JobMatchMapper mapper;

    public PageResult<Map<String,Object>> jobs(JobFilter f,int page,int size) {
        portal.actor("STUDENT","RECRUITER","SCHOOL_ADMIN");var p=new PageQuery(page,size);
        return new PageResult<>(mapper.jobs(f,p.size(),p.offset()),mapper.count(f),page,size);
    }
    public List<JobRecommendation> recommendations(Long studentId) {
        var a=portal.actor("STUDENT","SCHOOL_ADMIN");
        StudentProfile s;
        if(studentId==null){if(!"STUDENT".equals(a.role()))throw BusinessException.badRequest("请选择学生");s=actors.student(a.id());}
        else {if(studentId<1)throw BusinessException.badRequest("studentId 必须大于 0");s=students.findById(studentId);}
        var owner=s==null?null:users.findById(s.getUserId());
        if(owner==null || owner.getSchoolId()!=a.schoolId() || ("STUDENT".equals(a.role()) && owner.getId()!=a.id()))throw BusinessException.notFound("学生档案");
        return mapper.candidates().stream().map(j->score(s,j))
            .sorted(Comparator.comparing((JobRecommendation r)->!r.conflicts().isEmpty())
                .thenComparing(JobRecommendation::score,Comparator.reverseOrder()).thenComparingLong(JobRecommendation::jobId))
            .limit(100).toList();
    }
    public static JobRecommendation score(StudentProfile s,JobPosition j) {
        var reasons=new ArrayList<String>();var missing=new ArrayList<String>();
        var conflicts=new ArrayList<String>();var info=new ArrayList<String>();int score=0;
        var majors=JobFilter.tokens(j.getRequiredMajor());
        if(majors.isEmpty() || majors.contains("不限")){score+=30;reasons.add("岗位不限专业");}
        else if(s.getMajor()==null || s.getMajor().isBlank())info.add("未填写学生专业");
        else if(majors.contains(s.getMajor().trim().toLowerCase(Locale.ROOT))){score+=30;reasons.add("专业符合岗位要求");}
        else conflicts.add("专业不符合岗位要求："+j.getRequiredMajor());
        var required=JobFilter.tokens(j.getRequiredSkills());var actual=JobFilter.tokens(s.getSkills());
        if(required.isEmpty()){score+=30;reasons.add("岗位未设技能条件");}
        else {for(var skill:required)if(!actual.contains(skill))missing.add(skill);
            score+=(int)Math.round(30.0*(required.size()-missing.size())/required.size());
            if(missing.size()<required.size())reasons.add("已具备技能："+String.join("、",required.stream().filter(actual::contains).toList()));
            if(actual.isEmpty())info.add("未填写学生技能");}
        if(s.getPreferredCity()==null || s.getPreferredCity().isBlank())info.add("未填写意向城市");
        else if(JobFilter.tokens(s.getPreferredCity()).contains(j.getCity().trim().toLowerCase(Locale.ROOT))){score+=15;reasons.add("地点符合意向城市");}
        else conflicts.add("岗位地点与意向城市不一致："+j.getCity());
        if(s.getAvailableFrom()==null || s.getAvailableTo()==null)info.add("学生可实习起止日期不完整");
        else if(j.getStartDate()==null || j.getEndDate()==null)info.add("岗位起止日期不完整，需向企业确认");
        else if(s.getAvailableFrom().isAfter(j.getStartDate()) || s.getAvailableTo().isBefore(j.getEndDate()))conflicts.add("学生可实习时间未覆盖岗位全程："+j.getStartDate()+" 至 "+j.getEndDate());
        else {score+=20;reasons.add("可实习时间覆盖岗位全程");}
        if(j.getDaysPerWeek()==null){score+=5;reasons.add("岗位未设每周到岗天数");}
        else if(s.getDaysPerWeek()==null)info.add("未填写每周可到岗天数");
        else if(s.getDaysPerWeek()<j.getDaysPerWeek())conflicts.add("每周到岗天数不足：岗位要求 "+j.getDaysPerWeek()+" 天");
        else {score+=5;reasons.add("每周到岗天数符合要求");}
        return new JobRecommendation(j.getId(),BigDecimal.valueOf(score),reasons,missing,conflicts,info,j.getTitle(),j.getCity());
    }
}

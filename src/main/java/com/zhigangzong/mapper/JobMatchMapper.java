package com.zhigangzong.mapper;

import com.zhigangzong.dto.JobFilter;
import com.zhigangzong.entity.JobPosition;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface JobMatchMapper {
    String OPEN=" FROM job_position j JOIN enterprise e ON e.id=j.enterprise_id WHERE j.review_status='APPROVED' AND j.publish_status='PUBLISHED' AND e.review_status='APPROVED' AND (j.application_deadline IS NULL OR j.application_deadline>=CURRENT_DATE)";
    String FILTER=" AND (INSTR(j.title,#{f.q})>0 OR INSTR(j.city,#{f.q})>0 OR INSTR(COALESCE(j.required_major,''),#{f.q})>0 OR INSTR(COALESCE(j.required_skills,''),#{f.q})>0)"
        +"<if test=\"f.major != ''\"> AND (j.required_major IS NULL OR TRIM(j.required_major)='' OR INSTR(j.required_major,#{f.major})>0)</if>"
        +"<if test=\"f.city != ''\"> AND INSTR(j.city,#{f.city})>0</if>"
        +"<foreach collection='f.skillTokens()' item='skill'> AND FIND_IN_SET(#{skill},REGEXP_REPLACE(TRIM(LOWER(COALESCE(j.required_skills,''))), '[[:space:]]*[,，;；、\\n\\r]+[[:space:]]*', ','))>0</foreach>"
        +"<if test='f.from != null'> AND j.start_date IS NOT NULL AND j.start_date &lt;= #{f.from}</if>"
        +"<if test='f.to != null'> AND j.end_date IS NOT NULL AND j.end_date &gt;= #{f.to}</if>";
    @Select("<script>SELECT j.id,j.title,j.description,j.city,j.required_major AS requiredMajor,j.required_skills AS requiredSkills,j.monthly_pay AS monthlyPay,j.start_date AS startDate,j.end_date AS endDate,j.days_per_week AS daysPerWeek,j.application_deadline AS applicationDeadline,e.name AS enterpriseName"+OPEN+FILTER+" ORDER BY j.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String,Object>> jobs(@Param("f")JobFilter f,@Param("size")int size,@Param("offset")int offset);
    @Select("<script>SELECT COUNT(*)"+OPEN+FILTER+"</script>") long count(@Param("f")JobFilter f);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT j.*"+OPEN+" ORDER BY j.id") List<JobPosition> candidates();
}

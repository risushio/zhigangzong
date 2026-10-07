package com.zhigangzong.mapper;

import com.zhigangzong.vo.StatisticsOverview;
import org.apache.ibatis.annotations.Select;

public interface StatisticsMapper {
    @Select("""
            SELECT (SELECT COUNT(*) FROM student_profile) AS students,
                   (SELECT COUNT(*) FROM enterprise) AS enterprises,
                   (SELECT COUNT(*) FROM job_position) AS jobs,
                   (SELECT COUNT(*) FROM internship_placement) AS placements,
                   (SELECT COUNT(*) FROM internship_placement WHERE school_approval_status = 'APPROVED') AS approved_placements,
                   (SELECT COUNT(*) FROM risk_alert WHERE status IN ('OPEN', 'IN_PROGRESS')) AS open_alerts
            """)
    StatisticsOverview overview();
}

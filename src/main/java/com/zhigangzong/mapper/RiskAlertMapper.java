package com.zhigangzong.mapper;

import com.zhigangzong.entity.RiskAlert;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface RiskAlertMapper {
    @Select("SELECT id, student_id, placement_id, alert_type, description, owner_id, status, resolution, resolved_at, created_at FROM risk_alert ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<RiskAlert> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM risk_alert")
    long count();

    @Select("SELECT id, student_id, placement_id, alert_type, description, owner_id, status, resolution, resolved_at, created_at FROM risk_alert WHERE id = #{id}")
    RiskAlert findById(Long id);
}

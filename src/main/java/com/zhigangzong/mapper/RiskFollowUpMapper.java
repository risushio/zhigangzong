package com.zhigangzong.mapper;

import com.zhigangzong.entity.RiskFollowUp;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface RiskFollowUpMapper {
    @Select("SELECT id, alert_id, actor_id, content, result, created_at FROM risk_follow_up ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<RiskFollowUp> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM risk_follow_up")
    long count();

    @Select("SELECT id, alert_id, actor_id, content, result, created_at FROM risk_follow_up WHERE id = #{id}")
    RiskFollowUp findById(Long id);
}

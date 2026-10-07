package com.zhigangzong.mapper;

import com.zhigangzong.entity.Notification;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface NotificationMapper {
    @Select("SELECT id, recipient_id, title, content, business_type, business_id, due_at, read_at, completed_at, created_at FROM notification ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Notification> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM notification")
    long count();

    @Select("SELECT id, recipient_id, title, content, business_type, business_id, due_at, read_at, completed_at, created_at FROM notification WHERE id = #{id}")
    Notification findById(Long id);
}

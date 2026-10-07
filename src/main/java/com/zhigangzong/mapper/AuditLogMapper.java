package com.zhigangzong.mapper;

import com.zhigangzong.entity.AuditLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface AuditLogMapper {
    @Select("SELECT id, actor_id, action, resource_type, resource_id, description, created_at FROM audit_log ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<AuditLog> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM audit_log")
    long count();

    @Select("SELECT id, actor_id, action, resource_type, resource_id, description, created_at FROM audit_log WHERE id = #{id}")
    AuditLog findById(Long id);

    default int recordCreation(String resourceType, Long resourceId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return insertCreation(resourceType, resourceId, authentication == null ? null : authentication.getName());
    }

    @Insert("INSERT INTO audit_log (actor_id, action, resource_type, resource_id, description) "
            + "VALUES ((SELECT user_id FROM auth_account WHERE username=#{username}), 'CREATE', #{resourceType}, #{resourceId}, '创建基础记录')")
    int insertCreation(@Param("resourceType") String resourceType, @Param("resourceId") Long resourceId,
                       @Param("username") String username);
}

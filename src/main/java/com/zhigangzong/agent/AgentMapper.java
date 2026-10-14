package com.zhigangzong.agent;

import com.zhigangzong.agent.AgentRecords.*;
import com.zhigangzong.mapper.PortalMapper.Actor;
import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface AgentMapper {
    @Select("SELECT u.id,u.school_id,u.department_id,u.role,NULL AS enterprise_id FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username} AND a.enabled=TRUE")
    Actor actor(String username);
    @Select("SELECT u.id,u.school_id,u.department_id,u.role,NULL AS enterprise_id FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username} AND a.enabled=TRUE FOR UPDATE")
    Actor lockActor(String username);
    @Select("SELECT id FROM school WHERE id=#{id} FOR UPDATE") Long lockSchool(long id);
    @Insert("INSERT INTO agent_session(id,school_id,owner_id,created_at,updated_at,expires_at) VALUES(#{id},#{schoolId},#{ownerId},#{createdAt},#{updatedAt},#{expiresAt})") int insertSession(Session s);
    @Select("SELECT * FROM agent_session WHERE id=#{id} AND school_id=#{school} AND owner_id=#{owner} FOR UPDATE")
    Session lockSession(@Param("id") String id,@Param("school") long school,@Param("owner") long owner);
    @Update("UPDATE agent_session SET active_draft_id=#{activeDraftId},processing_message_id=#{processingMessageId},processing_until=#{processingUntil},updated_at=#{updatedAt} WHERE id=#{id}") int updateSession(Session s);
    @Select("SELECT * FROM agent_message WHERE session_id=#{session} AND client_message_id=#{client} FOR UPDATE")
    Message message(@Param("session") String session,@Param("client") String client);
    @Select("SELECT * FROM agent_message WHERE id=#{id}") Message messageById(String id);
    @Select("SELECT * FROM agent_message WHERE session_id=#{session} ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Message> messages(@Param("session") String session,@Param("limit") int limit,@Param("offset") int offset);
    @Select("SELECT COUNT(*) FROM agent_message m JOIN agent_session s ON s.id=m.session_id WHERE s.owner_id=#{owner} AND s.school_id=#{school} AND m.created_at>DATE_SUB(NOW(),INTERVAL 1 MINUTE)")
    int recentMessages(@Param("owner") long owner,@Param("school") long school);
    @Insert("INSERT INTO agent_message(id,session_id,client_message_id,request_hash,content,processing_status,trace_id,created_at,updated_at) VALUES(#{id},#{sessionId},#{clientMessageId},#{requestHash},#{content},#{processingStatus},#{traceId},#{createdAt},#{updatedAt})") int insertMessage(Message m);
    @Update("UPDATE agent_message SET processing_status=#{processingStatus},response_payload=#{responsePayload},updated_at=#{updatedAt} WHERE id=#{id}") int updateMessage(Message m);
    @Select("SELECT * FROM agent_student_draft WHERE id=#{id} AND school_id=#{school} AND owner_id=#{owner}")
    Draft draft(@Param("id") String id,@Param("school") long school,@Param("owner") long owner);
    @Select("SELECT * FROM agent_student_draft WHERE id=#{id} AND school_id=#{school} AND owner_id=#{owner} FOR UPDATE")
    Draft lockDraft(@Param("id") String id,@Param("school") long school,@Param("owner") long owner);
    @Insert("INSERT INTO agent_student_draft(id,session_id,school_id,owner_id,status,version,fields_json,sources_json,created_at,updated_at,expires_at) VALUES(#{id},#{sessionId},#{schoolId},#{ownerId},#{status},#{version},#{fieldsJson},#{sourcesJson},#{createdAt},#{updatedAt},#{expiresAt})") int insertDraft(Draft d);
    @Update("UPDATE agent_student_draft SET status=#{status},version=#{version},fields_json=#{fieldsJson},sources_json=#{sourcesJson},student_id=#{studentId},confirmed_by=#{confirmedBy},confirmed_at=#{confirmedAt},confirmed_version=#{confirmedVersion},confirmed_payload_hash=#{confirmedPayloadHash},updated_at=#{updatedAt} WHERE id=#{id}") int updateDraft(Draft d);
    @Select("SELECT * FROM agent_action_request WHERE school_id=#{school} AND actor_id=#{actor} AND action_type='CREATE_STUDENT_PROFILE' AND idempotency_key=#{key} FOR UPDATE")
    Action action(@Param("school") long school,@Param("actor") long actor,@Param("key") String key);
    @Insert("INSERT INTO agent_action_request(id,school_id,actor_id,action_type,idempotency_key,draft_id,draft_version,request_hash,student_id,result_json,created_at) VALUES(#{id},#{schoolId},#{actorId},#{actionType},#{idempotencyKey},#{draftId},#{draftVersion},#{requestHash},#{studentId},#{resultJson},#{createdAt})") int insertAction(Action a);
    @Select("SELECT * FROM agent_tool_call WHERE message_id=#{message} AND call_id=#{call} FOR UPDATE")
    ToolCall toolCall(@Param("message") String message,@Param("call") String call);
    @Select("SELECT result_json FROM agent_tool_call WHERE message_id=#{id}") List<String> toolResults(String id);
    @Insert("INSERT INTO agent_tool_call(message_id,call_id,request_hash,result_json,created_at) VALUES(#{messageId},#{callId},#{requestHash},#{resultJson},#{createdAt})") int insertToolCall(ToolCall call);
    @Select("SELECT CAST(id AS CHAR) AS id,name,code FROM department WHERE school_id=#{school} AND name LIKE CONCAT('%',#{keyword},'%') ORDER BY name,id LIMIT 10")
    List<Map<String,Object>> departments(@Param("school") long school,@Param("keyword") String keyword);
    @Select("SELECT CAST(id AS CHAR) AS id,name,code FROM department WHERE school_id=#{school} AND name=#{name} ORDER BY id LIMIT 11")
    List<Map<String,Object>> exactDepartments(@Param("school") long school,@Param("name") String name);
    @Select("SELECT name FROM department WHERE id=#{id} AND school_id=#{school}")
    String departmentName(@Param("id") long id,@Param("school") long school);
    @Select("SELECT COUNT(*) FROM student_profile WHERE school_id=#{school} AND student_no=#{no}")
    int duplicates(@Param("school") long school,@Param("no") String no);
    @Select("SELECT COUNT(*) FROM student_profile WHERE id=#{id} AND school_id=#{school}")
    int studentAccessible(@Param("id") long id,@Param("school") long school);
    // 保留审计与确认记录，只清理超过保留期的资料文本；不删除学生。
    @Update("UPDATE agent_message SET content=NULL,response_payload=NULL WHERE updated_at<DATE_SUB(NOW(),INTERVAL #{days} DAY) AND processing_status<>'PROCESSING'") int redactMessages(int days);
    @Update("UPDATE agent_student_draft SET fields_json='{}',sources_json='{}',status=IF(status IN ('EDITING','COLLECTING','READY'),'EXPIRED',status) WHERE expires_at<DATE_SUB(NOW(),INTERVAL #{days} DAY)") int redactDrafts(int days);
    @Delete("DELETE c FROM agent_tool_call c JOIN agent_message m ON m.id=c.message_id WHERE m.updated_at<DATE_SUB(NOW(),INTERVAL #{days} DAY) AND m.processing_status<>'PROCESSING'") int cleanToolCalls(int days);
    @Update("UPDATE agent_message m JOIN agent_session s ON s.processing_message_id=m.id SET m.processing_status='FAILED',m.updated_at=NOW(),m.response_payload='{\"processingStatus\":\"FAILED\",\"code\":\"PROCESSING_EXPIRED\",\"message\":\"处理已超时，草稿保留，请发送新消息\"}' WHERE m.processing_status='PROCESSING' AND s.processing_until<NOW()") int expireProcessing();
}

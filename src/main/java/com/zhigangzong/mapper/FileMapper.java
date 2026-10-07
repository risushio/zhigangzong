package com.zhigangzong.mapper;
import com.zhigangzong.entity.StoredFile;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface FileMapper {
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM stored_file WHERE id=#{id}") StoredFile file(long id);
    @Insert("INSERT INTO stored_file(owner_user_id,school_id,placement_id,kind,original_name,content_type,storage_key,byte_size,sha256,review_status) VALUES(#{ownerUserId},#{schoolId},#{placementId},#{kind},#{originalName},#{contentType},#{storageKey},#{byteSize},#{sha256},#{reviewStatus})")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(StoredFile f);
    String FILTER=" FROM stored_file WHERE <choose><when test='placement != null'>placement_id=#{placement}</when><otherwise>owner_user_id=#{owner} AND kind='RESUME'</otherwise></choose>";
    @Select("<script>SELECT *"+FILTER+" ORDER BY id DESC LIMIT #{size} OFFSET #{offset}</script>") List<StoredFile> list(@Param("placement")Long placement,@Param("owner")long owner,@Param("size")int size,@Param("offset")int offset);
    @Select("<script>SELECT COUNT(*)"+FILTER+"</script>") long count(@Param("placement")Long placement,@Param("owner")long owner);
    @Insert("INSERT INTO application_resume(application_id,file_id) VALUES(#{application},#{file})") int share(@Param("application")long application,@Param("file")long file);
    @Select("SELECT f.* FROM stored_file f JOIN application_resume r ON r.file_id=f.id WHERE r.application_id=#{id}") StoredFile resume(long id);
    @Select("SELECT COUNT(*) FROM application_resume r JOIN job_application a ON a.id=r.application_id JOIN job_position j ON j.id=a.job_id JOIN student_profile s ON s.id=a.student_id JOIN user_account u ON u.id=s.user_id WHERE a.recruitment_status<>'WITHDRAWN' AND r.file_id=#{file} AND u.school_id=#{school} AND j.enterprise_id=#{enterprise}")
    long shared(@Param("file")long file,@Param("school")long school,@Param("enterprise")long enterprise);
    @Insert("INSERT INTO internship_material(placement_id,material_type,file_ref) VALUES(#{placement},#{kind},CONCAT('/api/portal/files/',#{file},'/download'))") int material(@Param("placement")long placement,@Param("kind")String kind,@Param("file")long file);
    @Update("UPDATE stored_file SET review_status=#{decision},review_comment=#{comment} WHERE id=#{id}") int review(@Param("id")long id,@Param("decision")String decision,@Param("comment")String comment);
    @Update("UPDATE internship_material SET review_status=#{decision},review_comment=#{comment} WHERE file_ref=CONCAT('/api/portal/files/',#{id},'/download')") int materialReview(@Param("id")long id,@Param("decision")String decision,@Param("comment")String comment);
    @Insert("INSERT INTO file_review_event(file_id,actor_id,decision,comment) VALUES(#{id},#{actor},#{decision},#{comment})") int reviewEvent(@Param("id")long id,@Param("actor")long actor,@Param("decision")String decision,@Param("comment")String comment);
    @Select("SELECT h.decision,h.comment,h.created_at AS createdAt,u.display_name AS actorName FROM file_review_event h JOIN user_account u ON u.id=h.actor_id WHERE file_id=#{id} ORDER BY h.id") List<Map<String,Object>> events(long id);
}

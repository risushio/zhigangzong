package com.zhigangzong;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class ReminderStatisticsIntegrationTest extends PortalTestFixture {
 long active()throws Exception{long a=apply();transition(a,recruiter,"OFFERED");transition(a,student,"ACCEPTED");long p=placement(a);send(post("/api/portal/placements/"+p+"/approval"),admin,Map.of("decision","APPROVED","comment","Test"),200);return p;}
 void warningPolicy()throws Exception{send(put("/api/portal/batches/"+batch+"/warnings"),admin,Map.of("unplaced",false,"unplacedGraceDays",0,"materials",true,"materialDays",0,"requiredKinds",List.of("AGREEMENT"),"reports",true,"frequencyDays",7,"reportGraceDays",0,"contact",false,"contactDays",14),200);}
 long material(long p){return insert("INSERT INTO stored_file(owner_user_id,school_id,placement_id,kind,original_name,content_type,storage_key,byte_size,sha256,review_status) VALUES(?,?,?,'AGREEMENT','test.txt','text/plain',?,1,?,'APPROVED')",userId(student),school,p,UUID.randomUUID()+".bin","0".repeat(64));}
 @Test void deadlinesDeduplicateCompleteOnResolutionAndProtectRecipient()throws Exception{
  long p=active();String from=LocalDate.now().minusDays(6).toString(),to=LocalDate.now().toString();db.update("UPDATE internship_placement SET start_date=?,end_date=?,arrival_status='ARRIVED' WHERE id=?",from,to,p);warningPolicy();
  assertEquals(2,send(post("/api/portal/reminders/scan"),admin,null,200).path("created").asInt());
  assertEquals(0,send(post("/api/portal/reminders/scan"),admin,null,200).path("created").asInt());
  long n=db.queryForObject("SELECT notification_id FROM deadline_reminder WHERE placement_id=? AND reminder_key LIKE '%MATERIAL%'",Long.class,p);
  send(post("/api/portal/notifications/"+n+"/complete"),other,null,404);
  send(post("/api/portal/notifications/"+n+"/read"),student,null,200);
  assertNull(db.queryForObject("SELECT completed_at FROM notification WHERE id=?",Object.class,n));
  material(p);send(post("/api/portal/reminders/scan"),admin,null,200);
  assertNotNull(db.queryForObject("SELECT completed_at FROM notification WHERE id=?",Object.class,n));
  long report=db.queryForObject("SELECT notification_id FROM deadline_reminder WHERE placement_id=? AND reminder_key LIKE '%REPORT%'",Long.class,p);
  send(post("/api/portal/notifications/"+report+"/complete"),student,null,200);assertNotNull(db.queryForObject("SELECT completed_at FROM notification WHERE id=?",Object.class,report));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM progress_report WHERE placement_id=?",Integer.class,p));
  send(post("/api/portal/reminders/scan"),student,null,403);
 }
 @Test void statisticsHaveExplicitDenominatorsMissingMaterialsAndCurrentArrangements()throws Exception{
  long p=active();warningPolicy();db.update("UPDATE job_position SET required_major='CS,Design' WHERE id=?",job);
  var d=send(get("/api/portal/batch-statistics").param("batchId",""+batch),admin,null,200);assertEquals(2,d.path("cohort").asInt());assertEquals(1,d.path("placed").asInt());assertEquals(50,d.path("placementRate").asInt());assertEquals(1,d.path("matched").asInt());assertEquals(1,d.path("materialMissing").asInt());
  long file=material(p);insert("INSERT INTO placement_archive(placement_id,result_file_id,summary,status,snapshot) VALUES(?,?,'Test','APPROVED','{}')",p,file);
  d=send(get("/api/portal/batch-statistics").param("batchId",""+batch),admin,null,200);assertEquals(1,d.path("completed").asInt());assertEquals(50,d.path("completionRate").asInt());assertEquals(0,d.path("materialMissing").asInt());
  db.update("UPDATE job_position SET required_major=NULL WHERE id=?",job);assertEquals(1,send(get("/api/portal/batch-statistics").param("batchId",""+batch),admin,null,200).path("professionalUnknown").asInt());
  long req=insert("INSERT INTO change_request(placement_id,change_type,status,reason,original_snapshot,requested_snapshot) VALUES(?,'TERMINATE','APPROVED','Test','{}','{}')",p);
  db.update("INSERT INTO placement_termination(placement_id,request_id,terminated_at) VALUES(?,?,CURRENT_TIMESTAMP)",p,req);
  d=send(get("/api/portal/batch-statistics").param("batchId",""+batch),admin,null,200);assertEquals(0,d.path("placed").asInt());assertEquals(0,d.path("completed").asInt());
  send(get("/api/portal/batch-statistics").param("batchId",""+batch),foreignAdmin,null,404);send(get("/api/portal/batch-statistics").param("batchId",""+batch),student,null,403);
 }
}

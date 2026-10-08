package com.zhigangzong;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import com.zhigangzong.service.StudentSheetService;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
class StudentSheetIntegrationTest extends PortalTestFixture {
 byte[] sheet(List<List<String>> rows,boolean formula)throws Exception{try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()){var s=book.createSheet("Students");var h=s.createRow(0);for(int i=0;i<StudentSheetService.HEADERS.size();i++)h.createCell(i).setCellValue(StudentSheetService.HEADERS.get(i));int n=1;for(var values:rows){var r=s.createRow(n++);for(int i=0;i<values.size();i++)r.createCell(i).setCellValue(values.get(i));}if(formula)s.getRow(1).getCell(2).setCellFormula("1+1");book.write(out);return out.toByteArray();}}
 com.fasterxml.jackson.databind.JsonNode upload(byte[] bytes,boolean apply,String who,int expected)throws Exception{return send(multipart("/api/portal/student-sheet/import").file(new MockMultipartFile("file","students.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes)).param("apply",""+apply),who,null,expected);}
 @Test void importValidatesAllRowsBeforeAtomicWriteAndExportsSafeText()throws Exception {
  long fresh=userId(account(school,department,"STUDENT"));long foreign=userId(account(db.queryForObject("SELECT school_id FROM user_account WHERE id=?",Long.class,userId(foreignAdmin)),null,"STUDENT"));
  var row=List.of(""+fresh,"000123","=CS","Java,SQL","Shanghai","2027-01-01","2027-12-31","5","Project","");var otherRow=List.of(""+foreign,"000124","CS");
  var invalid=upload(sheet(List.of(row,otherRow),false),true,admin,200);assertEquals(1,invalid.path("errors").size());assertEquals(0,invalid.path("imported").asInt());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM student_profile WHERE user_id=?",Integer.class,fresh));
  assertEquals(1,upload(sheet(List.of(row),true),true,admin,200).path("errors").size());
  assertEquals(1,upload(sheet(List.of(row,row),false),true,admin,200).path("errors").size());
  var valid=sheet(List.of(row),false);assertEquals(0,upload(valid,false,admin,200).path("imported").asInt());assertEquals(1,upload(valid,true,admin,200).path("imported").asInt());assertEquals("000123",db.queryForObject("SELECT student_no FROM student_profile WHERE user_id=?",String.class,fresh));
  assertEquals(1,upload(valid,true,admin,200).path("errors").size());upload(valid,false,student,403);
  var response=mvc.perform(as(get("/api/portal/student-sheet/export"),admin)).andReturn().getResponse();assertEquals(200,response.getStatus());
  try(var book=new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))){assertEquals(4,book.getSheetAt(0).getPhysicalNumberOfRows());var r=book.getSheetAt(0).getRow(3);assertEquals("000123",r.getCell(1).getStringCellValue());assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,r.getCell(2).getCellType());assertEquals("=CS",r.getCell(2).getStringCellValue());}
  upload(new byte[]{1,2,3},false,admin,400);
  send(get("/api/portal/student-sheet/export"),student,null,403);
 }
}

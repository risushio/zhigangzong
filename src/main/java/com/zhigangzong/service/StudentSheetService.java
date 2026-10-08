package com.zhigangzong.service;
import com.zhigangzong.dto.CreateStudentProfileRequest;
import com.zhigangzong.entity.StudentProfile;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional public class StudentSheetService {
 public static final List<String> HEADERS=List.of("userId","studentNo","major","skills","preferredCity","availableFrom","availableTo","daysPerWeek","projectExperience","resumeRef");
 private final PortalService portal;private final PortalMapper accounts;private final StudentSheetMapper mapper;private final StudentProfileMapper profiles;private final ManagementMapper audit;private final ReminderMapper locks;private final Validator validator;
 public byte[] export(boolean template)throws IOException{var a=portal.actor("SCHOOL_ADMIN");var records=template?List.<StudentProfile>of():mapper.students(a.schoolId());if(records.size()>10000)throw BusinessException.badRequest("一次最多导出 10000 个学生，请缩小范围");try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()){var sheet=book.createSheet("Students");var header=sheet.createRow(0);var style=book.createCellStyle();var font=book.createFont();font.setBold(true);style.setFont(font);for(int i=0;i<HEADERS.size();i++){var c=header.createCell(i);c.setCellValue(HEADERS.get(i));c.setCellStyle(style);sheet.setColumnWidth(i,(i>=8?40:22)*256);}sheet.createFreezePane(0,1);int row=1;for(var s:records){var r=sheet.createRow(row++);var values=Arrays.asList(s.getUserId(),s.getStudentNo(),s.getMajor(),s.getSkills(),s.getPreferredCity(),s.getAvailableFrom(),s.getAvailableTo(),s.getDaysPerWeek(),s.getProjectExperience(),s.getResumeRef());for(int i=0;i<values.size();i++)r.createCell(i,CellType.STRING).setCellValue(values.get(i)==null?"":values.get(i).toString());}book.write(out);audit.audit(a.id(),"STUDENT_EXPORT","student_profile",0,template?"导出导入模板":"导出本校学生档案");return out.toByteArray();}}
 public Map<String,Object> importSheet(MultipartFile file,boolean apply)throws IOException {
  var a=portal.actor("SCHOOL_ADMIN");locks.lockSchool(a.schoolId());String name=file.getOriginalFilename();if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")||file.isEmpty()||file.getSize()>2*1024*1024)throw BusinessException.badRequest("请选择不超过 2 MB 的非空 .xlsx 文件");
  var valid=new ArrayList<StudentProfile>();var errors=new ArrayList<Map<String,Object>>();var users=new HashSet<Long>();var numbers=new HashSet<String>();
  try(var book=new XSSFWorkbook(file.getInputStream())){
   if(book.getNumberOfSheets()!=1||book.isMacroEnabled()||!book.getExternalLinksTable().isEmpty())throw BusinessException.badRequest("导入只接受单工作表、无宏、无外部链接的模板");var sheet=book.getSheetAt(0);if(sheet.getLastRowNum()>1000)throw BusinessException.badRequest("一次最多导入 1000 行");
   if(sheet.getRow(0)==null||sheet.getRow(0).getLastCellNum()!=HEADERS.size())throw BusinessException.badRequest("表头与模板不一致");for(int c=0;c<HEADERS.size();c++)if(!HEADERS.get(c).equals(value(sheet.getRow(0),c)))throw BusinessException.badRequest("第 "+(c+1)+" 列须为 "+HEADERS.get(c));
   for(int n=1;n<=sheet.getLastRowNum();n++){var r=sheet.getRow(n);if(r==null)continue;try{if(r.getLastCellNum()>HEADERS.size())throw BusinessException.badRequest("存在模板以外的列");var v=new ArrayList<String>();for(int c=0;c<HEADERS.size();c++)v.add(value(r,c));if(v.stream().allMatch(String::isEmpty))continue;Long userId=Long.valueOf(v.get(0));if(userId<1)throw BusinessException.badRequest("userId 必须为正整数");Integer days=v.get(7).isEmpty()?null:Integer.valueOf(v.get(7));var request=new CreateStudentProfileRequest(userId,v.get(1),v.get(2),optional(v.get(3)),optional(v.get(8)),optional(v.get(9)),optional(v.get(4)),date(v.get(5)),date(v.get(6)),days);var violations=validator.validate(request);if(!violations.isEmpty())throw BusinessException.badRequest(violations.stream().map(x->x.getPropertyPath()+": "+x.getMessage()).sorted().findFirst().orElseThrow());if(request.availableFrom()!=null&&request.availableTo()!=null&&request.availableTo().isBefore(request.availableFrom()))throw BusinessException.badRequest("结束日期不能早于开始日期");var u=accounts.lockUser(userId);if(u==null||u.getSchoolId()!=a.schoolId()||!"STUDENT".equals(u.getRole()))throw BusinessException.badRequest("userId 须关联本校 STUDENT 用户");if(accounts.student(userId)!=null)throw BusinessException.badRequest("用户已有档案，导入不会覆盖");if(!users.add(userId))throw BusinessException.badRequest("文件内 userId 重复");if(!numbers.add(request.studentNo().toLowerCase(Locale.ROOT))||mapper.studentNo(a.schoolId(),request.studentNo())>0)throw BusinessException.badRequest("本校或文件内学号重复");var s=new StudentProfile();s.setUserId(userId);s.setStudentNo(request.studentNo());s.setMajor(request.major());s.setSkills(request.skills());s.setProjectExperience(request.projectExperience());s.setResumeRef(request.resumeRef());s.setPreferredCity(request.preferredCity());s.setAvailableFrom(request.availableFrom());s.setAvailableTo(request.availableTo());s.setDaysPerWeek(request.daysPerWeek());valid.add(s);
    }catch(RuntimeException ex){errors.add(Map.of("row",n+1,"message",ex instanceof com.zhigangzong.exception.BusinessException?ex.getMessage():"数字、日期或单元格格式无效；日期须为 YYYY-MM-DD"));}}
  }catch(org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException ex){throw BusinessException.badRequest("文件不是有效的 XLSX 工作簿");}catch(org.apache.poi.ooxml.POIXMLException|IllegalArgumentException ex){throw BusinessException.badRequest("工作簿损坏或结构不支持");}
  if(valid.isEmpty()&&errors.isEmpty())throw BusinessException.badRequest("模板没有学生数据");int imported=0;if(apply&&errors.isEmpty()){for(var s:valid){profiles.insert(s);audit.audit(a.id(),"STUDENT_IMPORT","student_profile",s.getId(),"Excel 导入本校学生档案");imported++;}}
  return Map.of("validRows",valid.size(),"errors",errors,"imported",imported,"applied",apply&&errors.isEmpty());
 }
 private String value(Row r,int i){var cell=r.getCell(i);if(cell==null||cell.getCellType()==CellType.BLANK)return "";if(cell.getCellType()==CellType.FORMULA||cell.getCellType()==CellType.ERROR)throw BusinessException.badRequest("不接受公式或错误单元格");if(cell.getCellType()!=CellType.STRING&&cell.getCellType()!=CellType.NUMERIC)throw BusinessException.badRequest("只接受文本或数字单元格");return new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();}
 private String optional(String v){return v.isEmpty()?null:v;}
 private LocalDate date(String v){return v.isEmpty()?null:LocalDate.parse(v);}
}

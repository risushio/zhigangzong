package com.zhigangzong.agent.extraction;

import com.zhigangzong.agent.llm.*;
import java.util.*;
import java.util.stream.Stream;

final class StudentExtractionFixtures {
    record Sample(String id,String input,Map<String,Object> fields,List<String> clear,boolean multiple) {
        String json(){return LlmJson.write(Map.of("multipleStudents",multiple,"fields",fields,"clearFields",clear));}
        @Override public String toString(){return id;}
    }
    static Sample sample(String id,String input,Map<String,Object> fields){return new Sample(id,input,fields,List.of(),false);}
    static StudentFields current(){return StudentFields.fromDraft(LlmJson.object("{\"name\":\"林验\",\"studentNo\":\"000999\",\"major\":\"计算机\",\"phone\":\"13800138000\",\"className\":\"原班级\"}"),null);}
    static Stream<Sample> samples(){return Stream.of(
        sample("01_complete","帮我录入张三，学号20260001，2026级，软件工程专业，软件工程1班。",Map.of("name","张三","studentNo","20260001","gradeYear",2026,"majorName","软件工程","className","软件工程1班")),
        sample("02_missing_major","姓名林验，学号000123",Map.of("name","林验","studentNo","000123")),
        sample("03_missing_number","姓名周验，专业计算机",Map.of("name","周验","majorName","计算机")),
        sample("04_name_only","姓名为陈验",Map.of("name","陈验")),
        sample("05_major_supplement","补充上一名学生的专业为软件工程",Map.of("majorName","软件工程")),
        sample("06_class_correction","班级改为软件二班",Map.of("className","软件二班")),
        sample("07_number_correction","学号改为000456",Map.of("studentNo","000456")),
        sample("08_name_correction","姓名更正为林新",Map.of("name","林新")),
        sample("09_phone","手机号是13800138000",Map.of("phone","13800138000")),
        new Sample("10_clear_phone","清空手机号",Map.of(),List.of("phone"),false),
        new Sample("11_clear_class","清空班级",Map.of(),List.of("className"),false),
        sample("12_grade","年级为2027",Map.of("gradeYear",2027)),
        sample("13_department","学院名称为信息工程学院",Map.of("departmentName","信息工程学院")),
        sample("14_city","意向城市为杭州",Map.of("preferredCity","杭州")),
        sample("15_skills","技能为Java",Map.of("skills","Java")),
        sample("16_project","项目经历为校园管理系统",Map.of("projectExperience","校园管理系统")),
        sample("17_from_date","开始日期为2026-11-01",Map.of("availableFrom","2026-11-01")),
        sample("18_to_date","结束日期为2027-03-01",Map.of("availableTo","2027-03-01")),
        sample("19_days","每周天数为5",Map.of("daysPerWeek",5)),
        sample("20_confirmation","确认创建",Map.of()),
        new Sample("21_batch","录入两名学生：张三学号001，李四学号002",Map.of(),List.of(),true),
        sample("22_invalid_phone","手机号为123",Map.of("phone","123")),
        sample("23_invalid_grade","年级为1800",Map.of("gradeYear",1800)),
        sample("24_no_guess","手机号暂未提供",Map.of()),
        new Sample("25_compound_change","修改上一名学生的班级名称为软件验收2班，并明确清空手机号。其余资料保持原样。",Map.of("className","软件验收2班"),List.of("phone"),false)
    );}
}

package com.zhigangzong.agent;

import java.util.*;

public final class AgentToolRegistry {
    private AgentToolRegistry() {}
    private static Map<String,Object> text(int max) {return Map.of("type","string","maxLength",max);}
    private static Map<String,Object> object(Map<String,Object> fields,List<String> required) {return Map.of("type","object","properties",fields,"required",required,"additionalProperties",false);}
    private static Map<String,Object> tool(String name,String description,Map<String,Object> parameters) {return Map.of("type","function","function",Map.of("name",name,"description",description,"parameters",parameters,"strict",false));}
    public static List<Map<String,Object>> schemas() {
        var fields=new LinkedHashMap<String,Object>();
        StudentDraftRules.TEXT_LIMITS.forEach((key,max)->fields.put(key,Map.of("type",List.of("string","null"),"maxLength",max)));
        fields.put("departmentId",Map.of("type",List.of("string","null"),"pattern","^[1-9][0-9]{0,18}$"));
        fields.put("daysPerWeek",Map.of("type",List.of("integer","null"),"minimum",1,"maximum",7));
        fields.put("gradeYear",Map.of("type",List.of("integer","null"),"minimum",1900,"maximum",2100));
        var upsert=new LinkedHashMap<String,Object>();upsert.put("draftId",Map.of("type",List.of("string","null"),"maxLength",36));upsert.put("expectedVersion",Map.of("type",List.of("integer","null"),"minimum",1));upsert.put("studentCount",Map.of("type","integer","minimum",1,"maximum",1));upsert.put("fields",object(fields,List.of()));
        return List.of(
            tool("search_departments","搜索操作者授权学校的学院；重名必须追问用户并选择真实 ID，项目没有班级字典。",object(Map.of("keyword",text(100)),List.of("keyword"))),
            tool("check_student_duplicate","仅检查本校学号是否重复，不返回其他学生资料。",object(Map.of("studentNo",text(40)),List.of("studentNo"))),
            tool("upsert_student_draft","仅保存一名学生草稿，绝不创建档案。未出现字段省略，明确清空用 null。更新必须使用当前 draftId 和 expectedVersion。",object(upsert,List.of("studentCount","fields"))),
            tool("get_student_draft","读取当前会话的草稿。",object(Map.of("draftId",text(36)),List.of("draftId"))));
    }
}

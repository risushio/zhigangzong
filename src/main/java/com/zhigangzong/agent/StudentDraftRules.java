package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhigangzong.dto.CreateStandaloneStudentProfileRequest;
import com.zhigangzong.exception.BusinessException;
import java.time.LocalDate;
import java.util.*;

public final class StudentDraftRules {
    private StudentDraftRules() {}
    static final Map<String,Integer> TEXT_LIMITS=Map.ofEntries(
        Map.entry("name",80),Map.entry("studentNo",40),Map.entry("major",100),Map.entry("phone",20),Map.entry("className",100),
        Map.entry("skills",1000),Map.entry("projectExperience",10000),Map.entry("preferredCity",100),
        Map.entry("availableFrom",10),Map.entry("availableTo",10));
    static final Set<String> FIELDS;
    static { var fields=new HashSet<>(TEXT_LIMITS.keySet()); fields.addAll(List.of("departmentId","daysPerWeek","gradeYear")); FIELDS=Set.copyOf(fields); }

    static ObjectNode normalize(JsonNode patch) {
        AgentJson.keys(patch,FIELDS);
        var result=AgentJson.JSON.createObjectNode();
        patch.fields().forEachRemaining(entry->{
            String key=entry.getKey();var value=entry.getValue();
            if(value.isNull()) { result.putNull(key); return; }
            if(TEXT_LIMITS.containsKey(key)) {
                if(!value.isTextual()||value.asText().length()>TEXT_LIMITS.get(key)) throw BusinessException.badRequest("字段类型或长度不合法："+key);
                String normalized=value.asText().trim();
                if(normalized.isEmpty()) result.putNull(key); else result.put(key,normalized);
            } else if("departmentId".equals(key)) {
                if(!value.isTextual()||!value.asText().matches("[1-9][0-9]{0,18}")) throw BusinessException.badRequest("学院 ID 必须为正整数字符串");
                try { Long.parseLong(value.asText()); } catch(NumberFormatException e) { throw BusinessException.badRequest("学院 ID 超出范围"); }
                result.put(key,value.asText());
            } else {
                if(!value.isIntegralNumber()||!value.canConvertToInt()) throw BusinessException.badRequest("gradeYear".equals(key)?"年级必须为整数":"每周天数必须为整数");
                result.put(key,value.asInt());
            }
        });
        return result;
    }

    record Validation(List<String> missingFields,List<Map<String,String>> fieldErrors,Map<String,String> display) {
        boolean ready() { return missingFields.isEmpty()&&fieldErrors.isEmpty(); }
    }
    static Validation validate(ObjectNode fields,long school,AgentMapper mapper) {
        var missing=new ArrayList<String>();var errors=new ArrayList<Map<String,String>>();var display=new LinkedHashMap<String,String>();
        for(String key:List.of("name","studentNo","major")) if(!fields.hasNonNull(key)) missing.add(key);
        if(fields.hasNonNull("phone")&&!fields.path("phone").asText().matches("1[3-9][0-9]{9}")) error(errors,"phone","手机号须为 11 位有效大陆手机号");
        if(fields.hasNonNull("departmentId")) {
            String name=mapper.departmentName(Long.parseLong(fields.path("departmentId").asText()),school);
            if(name==null) error(errors,"departmentId","请选择可访问的本校学院"); else display.put("departmentName",name);
        }
        if(fields.hasNonNull("studentNo")&&mapper.duplicates(school,fields.path("studentNo").asText())>0) error(errors,"studentNo","本校学号已存在");
        LocalDate from=date(fields,"availableFrom",errors),to=date(fields,"availableTo",errors);
        if(from!=null&&to!=null&&to.isBefore(from)) error(errors,"availableTo","结束日期不能早于开始日期");
        if(fields.hasNonNull("daysPerWeek")&&(fields.path("daysPerWeek").asInt()<1||fields.path("daysPerWeek").asInt()>7)) error(errors,"daysPerWeek","每周天数须为 1 至 7");
        if(fields.hasNonNull("gradeYear")&&(fields.path("gradeYear").asInt()<1900||fields.path("gradeYear").asInt()>2100)) error(errors,"gradeYear","年级须为 1900 至 2100 的年份");
        return new Validation(missing,errors,display);
    }
    private static void error(List<Map<String,String>> errors,String field,String message) { errors.add(Map.of("field",field,"message",message)); }
    private static LocalDate date(ObjectNode fields,String key,List<Map<String,String>> errors) {
        if(!fields.hasNonNull(key)) return null;
        try { return LocalDate.parse(fields.path(key).asText()); }
        catch(Exception e) { error(errors,key,"日期须为有效的 YYYY-MM-DD"); return null; }
    }
    static CreateStandaloneStudentProfileRequest creationRequest(ObjectNode fields) {
        return new CreateStandaloneStudentProfileRequest(
            string(fields,"name"),string(fields,"studentNo"),string(fields,"major"),
            fields.hasNonNull("departmentId")?Long.parseLong(fields.path("departmentId").asText()):null,
            string(fields,"phone"),string(fields,"skills"),string(fields,"projectExperience"),string(fields,"preferredCity"),
            fields.hasNonNull("availableFrom")?LocalDate.parse(fields.path("availableFrom").asText()):null,
            fields.hasNonNull("availableTo")?LocalDate.parse(fields.path("availableTo").asText()):null,
            fields.hasNonNull("daysPerWeek")?fields.path("daysPerWeek").asInt():null,
            fields.hasNonNull("gradeYear")?fields.path("gradeYear").asInt():null,string(fields,"className"));
    }
    static String string(JsonNode fields,String key) { return fields.hasNonNull(key)?fields.path(key).asText():null; }
}

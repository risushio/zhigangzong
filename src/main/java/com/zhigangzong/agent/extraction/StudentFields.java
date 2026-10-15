package com.zhigangzong.agent.extraction;

import com.fasterxml.jackson.databind.JsonNode;

/** Model context contains student fields only, never persistence or authorization identifiers. */
public record StudentFields(String name,String studentNo,String majorName,Integer gradeYear,String className,
                            String phone,String departmentName,String skills,String projectExperience,
                            String preferredCity,String availableFrom,String availableTo,Integer daysPerWeek) {
    private static String text(JsonNode n,String key){return n.path(key).isTextual()?n.path(key).asText():null;}
    private static Integer number(JsonNode n,String key){return n.path(key).isIntegralNumber()?n.path(key).asInt():null;}
    public static StudentFields fromDraft(JsonNode fields,String departmentName) {
        return new StudentFields(text(fields,"name"),text(fields,"studentNo"),text(fields,"major"),number(fields,"gradeYear"),
            text(fields,"className"),text(fields,"phone"),departmentName,text(fields,"skills"),text(fields,"projectExperience"),
            text(fields,"preferredCity"),text(fields,"availableFrom"),text(fields,"availableTo"),number(fields,"daysPerWeek"));
    }
    @Override public String toString(){return "StudentFields[redacted]";}
}

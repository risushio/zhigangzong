package com.zhigangzong.dto;

import com.zhigangzong.exception.BusinessException;
import java.time.LocalDate;
import java.util.*;

public record JobFilter(String q, String major, String skills, String city, LocalDate from, LocalDate to) {
    public JobFilter {
        q=clean(q,100); major=clean(major,100); skills=clean(skills,1000); city=clean(city,100);
        if(from!=null && to!=null && to.isBefore(from))throw BusinessException.badRequest("结束日期不能早于开始日期");
    }
    private static String clean(String value,int max) {
        String v=value==null?"":value.trim();
        if(v.length()>max)throw BusinessException.badRequest("筛选条件过长");
        return v;
    }
    public List<String> skillTokens(){return tokens(skills);}
    public static List<String> tokens(String s){
        if(s==null || s.isBlank())return List.of();
        return Arrays.stream(s.toLowerCase(Locale.ROOT).split("[,，;；、\\n\\r]+"))
            .map(String::trim).filter(x->!x.isEmpty()).distinct().toList();
    }
}

package com.zhigangzong.mapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface CatalogMapper {
    @SelectProvider(type=CatalogSql.class, method="page")
    List<Map<String,Object>> page(@Param("resource") String resource, @Param("q") String q,
            @Param("status") String status, @Param("city") String city, @Param("offset") int offset, @Param("size") int size);
    @SelectProvider(type=CatalogSql.class, method="count")
    long count(@Param("resource") String resource, @Param("q") String q, @Param("status") String status, @Param("city") String city);
}

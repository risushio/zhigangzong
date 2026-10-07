package com.zhigangzong.service;
import com.zhigangzong.common.PageResult;
import java.util.Map;
public interface CatalogService {
    PageResult<Map<String,Object>> search(String resource,String q,String status,String city,int page,int size);
}

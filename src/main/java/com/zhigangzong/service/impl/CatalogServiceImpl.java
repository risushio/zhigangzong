package com.zhigangzong.service.impl;
import com.zhigangzong.common.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.service.CatalogService;
import com.zhigangzong.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {
    private final CatalogMapper mapper;
    @Override @Transactional(readOnly=true)
    public PageResult<Map<String,Object>> search(String resource,String q,String status,String city,int page,int size) {
        CatalogSql.validate(resource);
        if(q.length()>100 || status.length()>32 || city.length()>100) throw BusinessException.badRequest("查询条件过长");
        PageQuery query=new PageQuery(page,size);
        return new PageResult<>(mapper.page(resource,q,status,city,query.offset(),size),mapper.count(resource,q,status,city),page,size);
    }
}

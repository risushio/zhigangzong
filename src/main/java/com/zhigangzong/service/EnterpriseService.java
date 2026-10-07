package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.dto.*;

public interface EnterpriseService {
    PageResult<Enterprise> listEnterprise(int page, int size);
    Enterprise getEnterprise(long id);
    Enterprise createEnterprise(CreateEnterpriseRequest request);
}

package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface RiskService {
    PageResult<RiskAlert> listRiskAlert(int page, int size);
    RiskAlert getRiskAlert(long id);

    PageResult<RiskFollowUp> listRiskFollowUp(int page, int size);
    RiskFollowUp getRiskFollowUp(long id);
}

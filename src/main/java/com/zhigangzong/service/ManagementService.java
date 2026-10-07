package com.zhigangzong.service;
import com.zhigangzong.dto.*;
import com.zhigangzong.entity.*;
public interface ManagementService {
    Enterprise updateEnterprise(long id,CreateEnterpriseRequest request);
    JobPosition updateJob(long id,CreateJobPositionRequest request);
    Enterprise reviewEnterprise(long id,ReviewRequest request);
    JobPosition reviewJob(long id,ReviewRequest request);
    JobPosition publication(long id,PublicationRequest request);
}

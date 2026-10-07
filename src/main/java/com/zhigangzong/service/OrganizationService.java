package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.dto.*;

public interface OrganizationService {
    PageResult<School> listSchool(int page, int size);
    School getSchool(long id);
    School createSchool(CreateSchoolRequest request);

    PageResult<Department> listDepartment(int page, int size);
    Department getDepartment(long id);
    Department createDepartment(CreateDepartmentRequest request);

    PageResult<UserAccount> listUserAccount(int page, int size);
    UserAccount getUserAccount(long id);
    UserAccount createUserAccount(CreateUserAccountRequest request);
}

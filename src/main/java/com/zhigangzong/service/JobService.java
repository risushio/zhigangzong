package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.dto.*;

public interface JobService {
    PageResult<JobPosition> listJobPosition(int page, int size);
    JobPosition getJobPosition(long id);
    JobPosition createJobPosition(CreateJobPositionRequest request);

    PageResult<JobFavorite> listJobFavorite(int page, int size);
    JobFavorite getJobFavorite(long id);
}

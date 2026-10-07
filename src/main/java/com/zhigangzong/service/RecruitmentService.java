package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface RecruitmentService {
    PageResult<JobApplication> listJobApplication(int page, int size);
    JobApplication getJobApplication(long id);

    PageResult<RecruitmentEvent> listRecruitmentEvent(int page, int size);
    RecruitmentEvent getRecruitmentEvent(long id);
}

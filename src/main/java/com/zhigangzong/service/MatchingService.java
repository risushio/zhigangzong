package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface MatchingService {
    java.util.List<com.zhigangzong.vo.JobRecommendation> recommendations(long studentId);

    PageResult<MatchFeedback> listMatchFeedback(int page, int size);
    MatchFeedback getMatchFeedback(long id);
}

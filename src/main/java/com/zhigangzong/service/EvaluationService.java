package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface EvaluationService {
    PageResult<InternshipEvaluation> listInternshipEvaluation(int page, int size);
    InternshipEvaluation getInternshipEvaluation(long id);

    PageResult<ArchiveRecord> listArchiveRecord(int page, int size);
    ArchiveRecord getArchiveRecord(long id);
}

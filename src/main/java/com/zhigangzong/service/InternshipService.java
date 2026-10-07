package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.dto.*;

public interface InternshipService {
    PageResult<InternshipBatch> listInternshipBatch(int page, int size);
    InternshipBatch getInternshipBatch(long id);
    InternshipBatch createInternshipBatch(CreateInternshipBatchRequest request);

    PageResult<InternshipPlacement> listInternshipPlacement(int page, int size);
    InternshipPlacement getInternshipPlacement(long id);

    PageResult<ApprovalRecord> listApprovalRecord(int page, int size);
    ApprovalRecord getApprovalRecord(long id);

    PageResult<InternshipMaterial> listInternshipMaterial(int page, int size);
    InternshipMaterial getInternshipMaterial(long id);
}

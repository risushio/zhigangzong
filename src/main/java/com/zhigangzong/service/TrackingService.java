package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;

public interface TrackingService {
    PageResult<ProgressReport> listProgressReport(int page, int size);
    ProgressReport getProgressReport(long id);

    PageResult<AttendanceRecord> listAttendanceRecord(int page, int size);
    AttendanceRecord getAttendanceRecord(long id);

    PageResult<ChangeRequest> listChangeRequest(int page, int size);
    ChangeRequest getChangeRequest(long id);

    PageResult<GuidanceRecord> listGuidanceRecord(int page, int size);
    GuidanceRecord getGuidanceRecord(long id);
}

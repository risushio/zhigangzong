package com.zhigangzong.service.impl;

import com.zhigangzong.common.PageQuery;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.mapper.*;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingServiceImpl implements TrackingService {
    private final ProgressReportMapper progressReportMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ChangeRequestMapper changeRequestMapper;
    private final GuidanceRecordMapper guidanceRecordMapper;

    @Override
    public PageResult<ProgressReport> listProgressReport(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(progressReportMapper.findPage(query.offset(), query.size()),
                progressReportMapper.count(), page, size);
    }

    @Override
    public ProgressReport getProgressReport(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        ProgressReport entity = progressReportMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("周报与阶段任务");
        }
        return entity;
    }

    @Override
    public PageResult<AttendanceRecord> listAttendanceRecord(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(attendanceRecordMapper.findPage(query.offset(), query.size()),
                attendanceRecordMapper.count(), page, size);
    }

    @Override
    public AttendanceRecord getAttendanceRecord(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        AttendanceRecord entity = attendanceRecordMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("出勤与补签");
        }
        return entity;
    }

    @Override
    public PageResult<ChangeRequest> listChangeRequest(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(changeRequestMapper.findPage(query.offset(), query.size()),
                changeRequestMapper.count(), page, size);
    }

    @Override
    public ChangeRequest getChangeRequest(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        ChangeRequest entity = changeRequestMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("实习变更历史");
        }
        return entity;
    }

    @Override
    public PageResult<GuidanceRecord> listGuidanceRecord(int page, int size) {
        PageQuery query = new PageQuery(page, size);
        return new PageResult<>(guidanceRecordMapper.findPage(query.offset(), query.size()),
                guidanceRecordMapper.count(), page, size);
    }

    @Override
    public GuidanceRecord getGuidanceRecord(long id) {
        if (id < 1) {
            throw BusinessException.badRequest("id 必须大于 0");
        }
        GuidanceRecord entity = guidanceRecordMapper.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("双导师指导与联系");
        }
        return entity;
    }
}

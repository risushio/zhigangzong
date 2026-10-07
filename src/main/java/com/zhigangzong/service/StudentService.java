package com.zhigangzong.service;

import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.dto.*;

public interface StudentService {
    PageResult<StudentProfile> listStudentProfile(int page, int size);
    StudentProfile getStudentProfile(long id);
    StudentProfile createStudentProfile(CreateStudentProfileRequest request);
}

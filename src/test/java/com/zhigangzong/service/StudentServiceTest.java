package com.zhigangzong.service;

import com.zhigangzong.dto.CreateStudentProfileRequest;
import com.zhigangzong.entity.UserAccount;
import com.zhigangzong.exception.BusinessException;
import com.zhigangzong.mapper.*;
import com.zhigangzong.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceTest {
    private final StudentProfileMapper students = mock(StudentProfileMapper.class);
    private final AuditLogMapper audit = mock(AuditLogMapper.class);
    private final UserAccountMapper users = mock(UserAccountMapper.class);
    private final StudentService service = new StudentServiceImpl(students, audit, users);

    @Test
    void rejectsNonStudentWithoutWritingData() {
        UserAccount teacher = new UserAccount();
        teacher.setRole("TEACHER");
        when(users.findById(1L)).thenReturn(teacher);
        assertThrows(BusinessException.class, () -> service.createStudentProfile(request(null, null)));
        verifyNoInteractions(students, audit);
    }

    @Test
    void rejectsInvertedDateRangeWithoutWritingData() {
        assertThrows(BusinessException.class, () -> service.createStudentProfile(
                request(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 5, 1))));
        verifyNoInteractions(students, audit, users);
    }

    private CreateStudentProfileRequest request(LocalDate from, LocalDate to) {
        return new CreateStudentProfileRequest(1L, "S001", "Computer Science",
                null, null, null, null, from, to, 5);
    }
}

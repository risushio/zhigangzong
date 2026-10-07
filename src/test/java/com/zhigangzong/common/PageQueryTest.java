package com.zhigangzong.common;

import com.zhigangzong.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageQueryTest {
    @Test
    void calculatesOffset() {
        assertEquals(40, new PageQuery(3, 20).offset());
    }

    @Test
    void rejectsInvalidPaginationAndOverflow() {
        assertThrows(BusinessException.class, () -> new PageQuery(0, 20));
        assertThrows(BusinessException.class, () -> new PageQuery(1, 101));
        assertThrows(BusinessException.class, () -> new PageQuery(Integer.MAX_VALUE, 100));
    }
}

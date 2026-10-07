package com.zhigangzong.common;

import com.zhigangzong.exception.BusinessException;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw BusinessException.badRequest("page 必须大于 0，size 必须在 1–100 之间，且页码不能超出范围");
        }
    }

    public int offset() {
        return (page - 1) * size;
    }
}

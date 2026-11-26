package com.shopmall.dto;

import java.util.List;

public record PageResult<T>(
        int page, int pageSize, long total, List<T> list
) {
}

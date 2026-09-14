package com.queue.backend.common;

import java.util.List;

/**
 * Jedinstven oblik svake greske iz API-ja - klijent uvijek parsira isto.
 */
public record ApiError(String message, List<String> details) {

    public static ApiError of(String message) {
        return new ApiError(message, List.of());
    }
}

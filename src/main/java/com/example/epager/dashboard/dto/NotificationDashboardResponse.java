package com.example.epager.dashboard.dto;

import java.util.List;

public record NotificationDashboardResponse(
        long total,
        long queued,
        long sent,
        long received,
        long seen,
        long failed,
        List<CountByStatusResponse> byStatus
) {
}

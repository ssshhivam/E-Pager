package com.example.epager.dashboard.dto;

import java.util.List;

public record IncidentDashboardResponse(
        long total,
        long triggered,
        long acknowledged,
        long resolved,
        long scheduledForEscalation,
        List<CountByStatusResponse> byStatus
) {
}

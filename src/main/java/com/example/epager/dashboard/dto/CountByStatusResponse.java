package com.example.epager.dashboard.dto;

public record CountByStatusResponse(
        String status,
        long count
) {
}

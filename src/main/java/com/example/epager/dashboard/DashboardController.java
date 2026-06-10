package com.example.epager.dashboard;

import com.example.epager.dashboard.dto.DashboardResponse;
import com.example.epager.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.buildDashboard(currentUser.require());
    }
}

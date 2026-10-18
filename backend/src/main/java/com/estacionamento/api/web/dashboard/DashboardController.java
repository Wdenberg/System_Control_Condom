package com.estacionamento.api.web.dashboard;

import com.estacionamento.api.application.dashboard.DashboardActivityResponse;
import com.estacionamento.api.application.dashboard.DashboardAlertResponse;
import com.estacionamento.api.application.dashboard.DashboardCameraResponse;
import com.estacionamento.api.application.dashboard.DashboardOccupancyResponse;
import com.estacionamento.api.application.dashboard.DashboardService;
import com.estacionamento.api.application.dashboard.DashboardSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAuthority('DASHBOARD_READ')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/activity")
    public List<DashboardActivityResponse> activity(@RequestParam(defaultValue = "20") int limit) {
        return dashboardService.recentActivity(limit);
    }

    @GetMapping("/occupancy")
    public List<DashboardOccupancyResponse> occupancy() {
        return dashboardService.occupancy();
    }

    @GetMapping("/cameras")
    public List<DashboardCameraResponse> cameras() {
        return dashboardService.cameras();
    }

    @GetMapping("/alerts")
    public List<DashboardAlertResponse> alerts(@RequestParam(defaultValue = "20") int limit) {
        return dashboardService.alerts(limit);
    }
}

package sg.edu.nus.features.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.security.UserPrincipal;
import sg.edu.nus.features.dashboard.dto.DashboardDailyResponse;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;

/*
*   AUTHOR: HuaYuan Xie / Khairulanwar
*   PURPOSE: Controller for the Dashboard feature, providing endpoints to fetch daily wellness summaries and activity records, as well as date range summaries for charting purposes.
*/

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    // Endpoint 1: The Main Dashboard View (Summary + Feed)
    @GetMapping("/daily")
    public ResponseEntity<DashboardDailyResponse> getDailyDashboard(
            @RequestParam 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Fetching daily dashboard for user: {} on date: {}", principal.getUsername(), date);
                
        User user = userRepository.findByEmailAddress(principal.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        DashboardDailyResponse response = dashboardService.getDailyDashboard(user, date);
        return ResponseEntity.ok(response);
    }

    // Endpoint 2: The Chart/Graph View (Date Range Summaries)
    @GetMapping("/range")
    public ResponseEntity<List<DailyWellnessSummary>> getDashboardRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("Fetching dashboard range for user: {} from date: {} to date: {}", principal.getUsername(), startDate, endDate);
        
        User user = userRepository.findByEmailAddress(principal.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<DailyWellnessSummary> response = dashboardService.getSummaryRange(user, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
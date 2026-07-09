package sg.edu.nus.features.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.wellness.model.ActivityRecord;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.repository.ActivityRecordRepository;
import sg.edu.nus.features.wellness.repository.DailyWellnessSummaryRepository;
import sg.edu.nus.features.dashboard.dto.DashboardDailyResponse;

/*
*   AUTHOR: HuaYuan Xie / Khairulanwar
*   PURPOSE: Service layer for the Dashboard feature, handling the business logic for fetching daily wellness summaries and activity records, as well as date range summaries for charting purposes.
*/

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DailyWellnessSummaryRepository summaryRepo;
    private final ActivityRecordRepository activityRepo;

    // 1. Fetch a Single Day's Wellness Summary and Activity Records for the Dashboard
    public DashboardDailyResponse getDailyDashboard(User user, LocalDate date) {
        
        // Fetch the summary, or return an empty/default one if the user hasn't logged anything today
        DailyWellnessSummary summary = summaryRepo.findByUserIdAndSummaryDate(user.getId(), date)
            .orElse(DailyWellnessSummary.builder()
                .user(user)
                .summaryDate(date)
                .totalWaterMl(0)
                .totalCaloriesIntake(0)
                .totalCaloriesBurned(0)
                .totalExerciseMinutes(0)
                .build());

        // Fetch the activity feed for this specific day (00:00:00 to 23:59:59)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusNanos(1);
        
        List<ActivityRecord> activities = activityRepo.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            user.getId(), startOfDay, endOfDay
        );

        return DashboardDailyResponse.builder()
            .dailyWellnessSummary(summary)
            .activityRecords(activities)
            .build();
    }

    // 2. Fetch Date Range (Just the Summaries for charts/graphs)
    public List<DailyWellnessSummary> getSummaryRange(User user, LocalDate startDate, LocalDate endDate) {
        return summaryRepo.findByUserIdAndSummaryDateBetweenOrderBySummaryDateAsc(
            user.getId(), startDate, endDate
        );
    }

}

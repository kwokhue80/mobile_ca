// Author: HuaYuan Xie
package sg.edu.nus.features.wellness.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// All-time / rolling aggregates used to evaluate the Badges grid's achievement
// conditions. Threshold comparisons (e.g. >= 1000km) live client-side in
// BadgesViewModel, alongside the badge descriptions.
@Getter
@Builder
@AllArgsConstructor
public class BadgeProgressResponse {
    private BigDecimal totalRunDistanceKm;
    private Integer totalCaloriesBurned;
    private Integer totalHydrationMl;
    private Double avgSleepHoursLast30Days;   // null if no sleep logged in the last 30 days
    private Double avgMoodLast30Days;          // null if no mood logged in the last 30 days
    private Integer distinctExerciseDays;
    private BigDecimal todayWeightKg;          // null if not logged today
}

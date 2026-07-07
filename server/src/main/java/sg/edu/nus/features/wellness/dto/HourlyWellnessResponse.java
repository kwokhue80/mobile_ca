package sg.edu.nus.features.wellness.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One of 24 fixed hourly buckets for a single date, used by the Distance/Calories/
// Hydration detail screens' Day view (precise-to-the-hour chart).
@Getter
@Builder
@AllArgsConstructor
public class HourlyWellnessResponse {
    private Integer hour;
    private BigDecimal distanceKm;
    private Integer caloriesBurnedKcal;
    private Integer waterMl;
}

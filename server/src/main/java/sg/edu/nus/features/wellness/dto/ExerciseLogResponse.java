// Author: HuaYuan Xie
package sg.edu.nus.features.wellness.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Structured exercise session used by the Home "Activity Tracked" list and
// History screen, as opposed to ActivityRecordDto's generic text description.
@Getter
@Builder
@AllArgsConstructor
public class ExerciseLogResponse {
    private Long id;
    private String exerciseType;
    private Integer durationMinutes;
    private BigDecimal distanceKm;
    private Integer caloriesBurnedKcal;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime loggedAt;
}

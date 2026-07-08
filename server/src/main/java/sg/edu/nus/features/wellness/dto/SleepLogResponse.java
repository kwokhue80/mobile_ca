package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One sleep session for the Sleep detail screen's charts. Attributed to the
// wake-up (endTime) date on the client, matching how the daily summary counts it.
@Getter
@Builder
@AllArgsConstructor
public class SleepLogResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer sleepQualityScore;
}

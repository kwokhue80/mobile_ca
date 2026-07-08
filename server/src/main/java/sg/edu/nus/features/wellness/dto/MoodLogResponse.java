package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One mood entry (1-10) for the Mental Health detail screen's charts.
@Getter
@Builder
@AllArgsConstructor
public class MoodLogResponse {
    private Long id;
    private Integer moodRating;
    private LocalDateTime loggedAt;
}

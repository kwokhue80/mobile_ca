package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One water-intake entry for the Hydration detail screen's charts.
@Getter
@Builder
@AllArgsConstructor
public class HydrationLogResponse {
    private Long id;
    private Integer volumeMl;
    private LocalDateTime loggedAt;
}

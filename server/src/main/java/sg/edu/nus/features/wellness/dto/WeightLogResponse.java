package sg.edu.nus.features.wellness.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One weight measurement for the Weight detail screen's line chart.
@Getter
@Builder
@AllArgsConstructor
public class WeightLogResponse {
    private Long id;
    private BigDecimal weightKg;
    private LocalDateTime loggedAt;
}

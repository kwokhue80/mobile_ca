package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Structured meal entry used by the Food Summary detail screen (Day meal list
// and the per-day breakdown under the Week/Month charts).
@Getter
@Builder
@AllArgsConstructor
public class FoodLogResponse {
    private Long id;
    private String mealType;
    private String foodName;
    private Integer caloriesKcal;
    private LocalDateTime loggedAt;
}

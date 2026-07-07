package sg.edu.nus.features.user.goal.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserGoalResponse {
    private final String goalType;
    private final BigDecimal targetValue;
    private final String unit;
}

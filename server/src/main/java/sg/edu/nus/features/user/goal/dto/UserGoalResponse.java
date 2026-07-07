package sg.edu.nus.features.user.goal.dto;

import java.math.BigDecimal;

public class UserGoalResponse {

    private final String goalType;
    private final BigDecimal targetValue;
    private final String unit;

    public UserGoalResponse(String goalType, BigDecimal targetValue, String unit) {
        this.goalType = goalType;
        this.targetValue = targetValue;
        this.unit = unit;
    }

    public String getGoalType() {
        return goalType;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public String getUnit() {
        return unit;
    }
}

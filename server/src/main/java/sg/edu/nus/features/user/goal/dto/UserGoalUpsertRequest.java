package sg.edu.nus.features.user.goal.dto;

import java.math.BigDecimal;

public class UserGoalUpsertRequest {

    private BigDecimal targetValue;

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }
}

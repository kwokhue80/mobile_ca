package sg.edu.nus.features.user.goal.dto;

import java.math.BigDecimal;

/*
*   AUTHOR: Amelia
*   PURPOSE: To be used by chatbot as repo projection interface; fetch raw columns without using full entity
*/
public interface UserGoalRawView {
    String getGoalType();
    BigDecimal getTargetValue();
}
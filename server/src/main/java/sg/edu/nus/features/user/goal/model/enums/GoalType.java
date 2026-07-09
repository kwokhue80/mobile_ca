package sg.edu.nus.features.user.goal.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Synced with frontend goals
@Getter
@RequiredArgsConstructor
public enum GoalType {
    DISTANCE("km per day"),
    STEPS("steps per day"),
    CALORIES("kcal per day"),
    FOOD_INTAKE("kcal per day"),
    SLEEP("hours per day"),
    WATER_ML("ml per day"),
    EXERCISE("days per week"),
    WEIGHT("kg");

    private final String unit;
}

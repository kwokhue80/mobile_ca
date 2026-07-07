package sg.edu.nus.features.user.goal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.user.goal.dto.UserGoalResponse;
import sg.edu.nus.features.user.goal.dto.UserGoalRawView;
import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.UserGoalId;
import sg.edu.nus.features.user.goal.model.enums.GoalType;

@Service
@RequiredArgsConstructor
public class UserGoalService {

    private final UserGoalRepository userGoalRepository;
    private final UserRepository userRepository;

    // Save user goal
    public UserGoal save(UserGoal userGoal) { return userGoalRepository.save(userGoal); }

    // Get user goal by Composite Key: User ID + Goal Type
    public UserGoal getById(UserGoalId id) {
        return userGoalRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User goal not found"));
    }

    // Get user goal by User ID only
    public List<UserGoal> getByUserId(UUID userId) { return userGoalRepository.findByUserId(userId); }
    
    // Get raw goal view (for chatbot) - limited fields
    public List<UserGoalResponse> getRawByUserId(UUID userId) {
        return userGoalRepository.findRawByUserId(userId)
            .stream()
            .map(this::toRawResponse)
            .toList();
    }

    // Update goal using Composite Key
    public UserGoal updateTargetValue(UUID userId, GoalType goalType, BigDecimal targetValue) {
        UserGoal goal = getById(new UserGoalId(userId, goalType));
        goal.setTargetValue(targetValue);
        return userGoalRepository.save(goal);
    }

    // Update user goal if it exists, insert if it doesn't
    // REASON for combining into one transaction: set goal as a single action
    @Transactional
    public UserGoal upsertGoal(UUID userId, GoalType goalType, BigDecimal targetValue) {
        // edge case: target value null / <= 0 
        if (targetValue == null || targetValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target value must be greater than zero");
        }
        
        return userGoalRepository

            // Find User Goal by Composite Key
            .findById(new UserGoalId(userId, goalType))

            // If exists, set new target value and save to DB
            .map(existing -> {
                existing.setTargetValue(targetValue);
                return userGoalRepository.save(existing);
            })

            // If not exists, create new user goal and save to DB
            .orElseGet(() -> {
                // Find user
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

                // Build UserGoal and map to user (JPA saves this automatically)
                UserGoalId id = new UserGoalId(userId, goalType);
                UserGoal goal = UserGoal.builder()
                    .id(id)
                    .user(user)
                    .targetValue(targetValue)
                    .build();

                // Save to DB
                return userGoalRepository.save(goal);
            });
    }

    public UserGoalResponse toResponse(UserGoal userGoal) {
		return new UserGoalResponse(
			userGoal.getId().getGoalType().name(),
			userGoal.getTargetValue(),
			userGoal.getId().getGoalType().getUnit());
	}

    public UserGoalResponse toRawResponse(UserGoalRawView rawGoal) {
        return new UserGoalResponse(
            rawGoal.getGoalType(),
            rawGoal.getTargetValue(),
            goalUnitFor(rawGoal.getGoalType())
        );
    }

    private String goalUnitFor(String goalType) {
        if (goalType == null) {
            return "";
        }

        String normalized = goalType.trim().toUpperCase();
        return switch (normalized) {
            case "WATER_ML", "HYDRATION" -> "ml per day";
            case "SLEEP_MINUTES" -> "minutes per day";
            case "SLEEP" -> "hours per day";
            case "CALORIES_BURNED", "CALORIES" -> "kcal per day";
            case "EXERCISE_DAYS" -> "days per week";
            case "WEIGHT" -> "kg";
            case "STEPS" -> "steps per day";
            default -> "";
        };
    }

}

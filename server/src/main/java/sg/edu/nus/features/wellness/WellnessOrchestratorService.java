package sg.edu.nus.features.wellness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;

import sg.edu.nus.features.wellness.model.ActivityRecord;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.model.ExerciseLog;
import sg.edu.nus.features.wellness.model.FoodLog;
import sg.edu.nus.features.wellness.model.HydrationLog;
import sg.edu.nus.features.wellness.model.MoodLog;
import sg.edu.nus.features.wellness.model.SleepLog;
import sg.edu.nus.features.wellness.model.WeightLog;
import sg.edu.nus.features.wellness.model.enums.ActivityType;
import sg.edu.nus.features.wellness.model.enums.ExerciseType;
import sg.edu.nus.features.wellness.model.enums.MealType;

import sg.edu.nus.features.wellness.repository.ActivityRecordRepository;
import sg.edu.nus.features.wellness.repository.DailyWellnessSummaryRepository;
import sg.edu.nus.features.wellness.repository.ExerciseLogRepository;
import sg.edu.nus.features.wellness.repository.FoodLogRepository;
import sg.edu.nus.features.wellness.repository.HydrationLogRepository;
import sg.edu.nus.features.wellness.repository.MoodLogRepository;
import sg.edu.nus.features.wellness.repository.SleepLogRepository;
import sg.edu.nus.features.wellness.repository.WeightLogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class WellnessOrchestratorService {
    
    private final DailyWellnessSummaryRepository summaryRepo;
    private final ActivityRecordRepository activityRepo;
    
    private final SleepLogRepository sleepRepo;
    private final FoodLogRepository foodRepo;
    private final WeightLogRepository weightRepo;
    private final HydrationLogRepository hydrationRepo;
    private final ExerciseLogRepository exerciseRepo;
    private final MoodLogRepository moodRepo;
    
    @Transactional
    public void processMonolithicRecord(User currentUser, WellnessRecordPayload payload) {
        LocalDateTime recordDate = payload.getRecordDate();
        LocalDate summaryDate = recordDate.toLocalDate();

        // 1. Fetch/Initialize the DailyWellnessSummary
        DailyWellnessSummary summary = summaryRepo.findByUserIdAndSummaryDate(currentUser.getId(), summaryDate)
            .orElse(DailyWellnessSummary.builder()
                .user(currentUser)
                .summaryDate(summaryDate)
                .totalWaterMl(0)
                .totalCaloriesIntake(0)
                .totalCaloriesBurned(0)
                .totalExerciseMinutes(0)
                .build());

        // 2. Process each Component of the Wellness Record
        // 2.1 Process Hydration
        if (payload.getWaterIntakeMl() != null && payload.getWaterIntakeMl() > 0) {
            HydrationLog hydration = HydrationLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .volumeMl(payload.getWaterIntakeMl())
                .build();
            hydration = hydrationRepo.save(hydration);

            saveActivityRecord(currentUser, hydration.getId(), "HYDRATION", "Water Logged", payload.getWaterIntakeMl() + " ml", recordDate);

            summary.setTotalWaterMl(summary.getTotalWaterMl() + payload.getWaterIntakeMl());
        }  

        // 2.2 Process Weight
        if (payload.getWeightKg() != null && payload.getWeightKg() > 0) {
            WeightLog weight = WeightLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .weightKg(BigDecimal.valueOf(payload.getWeightKg()))
                .build();
            weight = weightRepo.save(weight);

            saveActivityRecord(currentUser, weight.getId(), "WEIGHT", "Weight Logged", payload.getWeightKg() + " kg", recordDate);

            summary.setWeightKg(BigDecimal.valueOf(payload.getWeightKg()));
        }

        // 2.3 Process Mood
        if (payload.getMoodRating() != null) {
            MoodLog mood = MoodLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .moodRating(payload.getMoodRating())
                .build();
            mood = moodRepo.save(mood);

            saveActivityRecord(currentUser, mood.getId(), "MOOD", "Mood Logged", "Rating: " + payload.getMoodRating() + "/10", recordDate);

            summary.setMoodScore(payload.getMoodRating());
        }

        // 2.4 Process Food
        if (payload.getMealType() != null && !payload.getMealType().isBlank() && payload.getMealCaloriesKcal() != null) {
            FoodLog food = FoodLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .mealType(MealType.valueOf(payload.getMealType())) 
                .foodName(payload.getMealDescription() != null ? payload.getMealDescription() : "Logged Meal")
                .caloriesKcal(payload.getMealCaloriesKcal())
                .build();
            food = foodRepo.save(food);

            saveActivityRecord(currentUser, food.getId(), "FOOD", "Meal Logged (" + payload.getMealType() + ")", payload.getMealCaloriesKcal() + " kcal", recordDate);

            summary.setTotalCaloriesIntake(summary.getTotalCaloriesIntake() + payload.getMealCaloriesKcal());
        }

        // 2.5 Process Exercise
        if (payload.getExerciseType() != null && !payload.getExerciseType().isBlank() && payload.getExerciseDurationMinutes() != null) {
            ExerciseLog exercise = ExerciseLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .exerciseType(ExerciseType.valueOf(payload.getExerciseType()))
                .durationMinutes(payload.getExerciseDurationMinutes())
                .distanceKm(payload.getExerciseDistanceKm() != null ? BigDecimal.valueOf(payload.getExerciseDistanceKm()) : null)
                .caloriesBurnedKcal(payload.getExerciseCaloriesBurnedKcal() != null ? payload.getExerciseCaloriesBurnedKcal() : 0)
                .build();
            exercise = exerciseRepo.save(exercise);

            saveActivityRecord(currentUser, exercise.getId(), "EXERCISE", "Exercise: " + payload.getExerciseType(), payload.getExerciseDurationMinutes() + " mins", recordDate);

            summary.setTotalExerciseMinutes(summary.getTotalExerciseMinutes() + payload.getExerciseDurationMinutes());
            if (payload.getExerciseCaloriesBurnedKcal() != null) {
                summary.setTotalCaloriesBurned(summary.getTotalCaloriesBurned() + payload.getExerciseCaloriesBurnedKcal());
            }
        }

        // 2.6 Process Sleep
        if (payload.getSleepMinutes() != null && payload.getSleepMinutes() > 0) {
            LocalDateTime endTime = recordDate;
            LocalDateTime startTime = endTime.minusMinutes(payload.getSleepMinutes());

            SleepLog sleep = SleepLog.builder()
                .user(currentUser)
                .startTime(startTime)
                .endTime(endTime)
                .sleepQualityScore(payload.getSleepQualityRating())
                .build();
            sleep = sleepRepo.save(sleep);

            int hours = payload.getSleepMinutes() / 60;
            int mins = payload.getSleepMinutes() % 60;
            saveActivityRecord(currentUser, sleep.getId(), "SLEEP", "Sleep Logged", 
                               hours + "h " + mins + "m", recordDate);

            summary.setSleepMinutes(payload.getSleepMinutes()); // Overwrites to latest logged sleep for the day
            summary.setSleepQualityScore(payload.getSleepQualityRating());
        }

        // 3. Save the updated DailyWellnessSummary
        summaryRepo.save(summary);
    }

    private void saveActivityRecord(User user, Long sourceLogId, String activityType, String title, String description, LocalDateTime recordedAt) {
        ActivityRecord record = ActivityRecord.builder()
            .user(user)
            .sourceLogId(sourceLogId)
            .activityType(ActivityType.valueOf(activityType))
            .title(title)
            .description(description)
            .recordedAt(recordedAt)
            .build();
        activityRepo.save(record);
    }
}

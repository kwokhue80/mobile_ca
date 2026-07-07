package sg.edu.nus.features.wellness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import sg.edu.nus.features.wellness.dto.ActivityRecordDto;
import sg.edu.nus.features.wellness.dto.ExerciseLogResponse;
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

            saveActivityRecord(currentUser, food.getId(), "FOOD", "Meal Logged (" + payload.getMealType() + ")", 
            	    (payload.getMealDescription() != null ? payload.getMealDescription() : "Logged Meal") + " - " + payload.getMealCaloriesKcal() + " kcal", 
            	    recordDate);
            // saveActivityRecord(currentUser, food.getId(), "FOOD", "Meal Logged (" + payload.getMealType() + ")", payload.getMealCaloriesKcal() + " kcal", recordDate);

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
                .startTime(payload.getExerciseStartTime())
                .endTime(payload.getExerciseEndTime())
                .build();
            exercise = exerciseRepo.save(exercise);

            saveActivityRecord(currentUser, exercise.getId(), "EXERCISE", "Exercise: " + payload.getExerciseType(), payload.getExerciseDurationMinutes() + " mins", recordDate);

            summary.setTotalExerciseMinutes(summary.getTotalExerciseMinutes() + payload.getExerciseDurationMinutes());
            if (payload.getExerciseCaloriesBurnedKcal() != null) {
                summary.setTotalCaloriesBurned(summary.getTotalCaloriesBurned() + payload.getExerciseCaloriesBurnedKcal());
            }
            if (payload.getExerciseDistanceKm() != null) {
                summary.setTotalDistanceKm(summary.getTotalDistanceKm().add(BigDecimal.valueOf(payload.getExerciseDistanceKm())));
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
    
 // Retrieves a user's logged activity history over a given number of
 // past days. Uses the existing activity_records table, which already
 // stores a short readable title and description for every logged event.
    public List<ActivityRecordDto> getActivityHistory(User currentUser, int numberOfDays) {
    LocalDateTime endTime = LocalDateTime.now();
    LocalDateTime startTime = endTime.minusDays(numberOfDays);

    List<ActivityRecord> records = activityRepo.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
        currentUser.getId(), startTime, endTime
    );

    List<ActivityRecordDto> result = new ArrayList<>();
    for (ActivityRecord record : records) {
        ActivityRecordDto dto = new ActivityRecordDto(
            record.getActivityType().toString(),
            record.getTitle(),
            record.getDescription(),
            record.getRecordedAt()
        );
        result.add(dto);
    }
    return result;
}

    // Retrieves a user's logged exercise sessions (structured: duration, distance,
    // calories, start/end time) over a given number of past days. Backs the Home
    // "Activity Tracked" list and the History screen.
    public List<ExerciseLogResponse> getExerciseLogs(User currentUser, int numberOfDays) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(numberOfDays);

        return exerciseRepo.findByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
                currentUser.getId(), startTime, endTime
        ).stream()
            .map(log -> ExerciseLogResponse.builder()
                .id(log.getId())
                .exerciseType(log.getExerciseType().toString())
                .durationMinutes(log.getDurationMinutes())
                .distanceKm(log.getDistanceKm())
                .caloriesBurnedKcal(log.getCaloriesBurnedKcal())
                .startTime(log.getStartTime())
                .endTime(log.getEndTime())
                .loggedAt(log.getLoggedAt())
                .build())
            .toList();
    }

    // Wipes today's logged wellness data (sleep/hydration/weight/mood/exercise, the
    // generic activity feed, and the aggregated daily summary) for the given user,
    // leaving user_goals and user_profile untouched. Used to reset a test account
    // to a clean slate on every app login.
    @Transactional
    public void resetToday(User currentUser) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        UUID userId = currentUser.getId();

        sleepRepo.deleteByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);
        hydrationRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        weightRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        moodRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        exerciseRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        activityRepo.deleteByUserIdAndRecordedAtBetween(userId, startOfDay, endOfDay);
        summaryRepo.deleteByUserIdAndSummaryDate(userId, today);
    }
}

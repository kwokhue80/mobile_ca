package sg.edu.nus.features.wellness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import sg.edu.nus.features.wellness.dto.ActivityRecordDto;
import sg.edu.nus.features.wellness.dto.BadgeProgressResponse;
import sg.edu.nus.features.wellness.dto.ExerciseLogResponse;
import sg.edu.nus.features.wellness.dto.FoodLogResponse;
import sg.edu.nus.features.wellness.dto.HourlyWellnessResponse;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserService;

import sg.edu.nus.features.user.profile.UserProfile;
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

/*
*   AUTHOR: Maowei Yang / Khairulanwar / Amelia Wong / Huayuan Xie
*   PURPOSE: Service layer for orchestrating wellness-related operations, handling the business logic for processing various wellness records and updating the corresponding summaries.
*/

@Slf4j
@Service
@RequiredArgsConstructor
public class WellnessOrchestratorService {
    
    private final DailyWellnessSummaryRepository summaryRepo;
    private final ActivityRecordRepository activityRepo;
    private final UserService userService;
    
    private final SleepLogRepository sleepRepo;
    private final FoodLogRepository foodRepo;
    private final WeightLogRepository weightRepo;
    private final HydrationLogRepository hydrationRepo;
    private final ExerciseLogRepository exerciseRepo;
    private final MoodLogRepository moodRepo;
    

    // ---------------------- CRUD LOGIC ---------------------- //

    public DailyWellnessSummary getDailyWellnessSummary(UUID userId) {
        User currentUser = userService.getById(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Singapore"));

        return summaryRepo.findByUserIdAndSummaryDate(userId, today)
            .orElse(DailyWellnessSummary.builder()
                .user(currentUser)
                .summaryDate(today)
                .totalWaterMl(0)
                .totalCaloriesIntake(0)
                .totalCaloriesBurned(0)
                .totalExerciseMinutes(0)
                .build());
    }
    
    @Transactional
    public void processMonolithicRecord(UUID userId, WellnessRecordPayload payload) {
        User currentUser = userService.getById(userId);
        LocalDateTime recordDate = payload.getRecordDate();
        LocalDate summaryDate = recordDate.toLocalDate();

        // 1. Fetch/Initialize the DailyWellnessSummary
        DailyWellnessSummary summary = summaryRepo.findByUserIdAndSummaryDate(userId, summaryDate)
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
                (payload.getMealDescription() != null ? payload.getMealDescription() : "Logged Meal") 
                + " - " + payload.getMealCaloriesKcal() + " kcal", recordDate);
            // saveActivityRecord(currentUser, food.getId(), "FOOD", "Meal Logged (" + payload.getMealType() + ")", payload.getMealCaloriesKcal() + " kcal", recordDate);

            summary.setTotalCaloriesIntake(summary.getTotalCaloriesIntake() + payload.getMealCaloriesKcal());
        }

        // 2.5 Process Exercise
        if (payload.getExerciseType() != null && !payload.getExerciseType().isBlank() && payload.getExerciseDurationMinutes() != null) {
            int caloriesBurnedKcal = resolveExerciseCaloriesBurned(currentUser, payload);

            ExerciseLog exercise = ExerciseLog.builder()
                .user(currentUser)
                .loggedAt(recordDate)
                .exerciseType(ExerciseType.valueOf(payload.getExerciseType()))
                .durationMinutes(payload.getExerciseDurationMinutes())
                .distanceKm(payload.getExerciseDistanceKm() != null ? BigDecimal.valueOf(payload.getExerciseDistanceKm()) : null)
                .caloriesBurnedKcal(caloriesBurnedKcal)
                .startTime(payload.getExerciseStartTime())
                .endTime(payload.getExerciseEndTime())
                .build();
            exercise = exerciseRepo.save(exercise);

            saveActivityRecord(currentUser, exercise.getId(), "EXERCISE", "Exercise: " 
                + payload.getExerciseType(), payload.getExerciseDurationMinutes() + " mins", recordDate);

            summary.setTotalExerciseMinutes(summary.getTotalExerciseMinutes() + payload.getExerciseDurationMinutes());
            
            if (payload.getExerciseDistanceKm() != null) {
                BigDecimal currentDistance = summary.getTotalDistanceKm() != null
                    ? summary.getTotalDistanceKm()
                    : BigDecimal.ZERO;
                summary.setTotalDistanceKm(currentDistance.add(BigDecimal.valueOf(payload.getExerciseDistanceKm())));
            }

            if (caloriesBurnedKcal > 0) {
                summary.setTotalCaloriesBurned(summary.getTotalCaloriesBurned() + caloriesBurnedKcal);
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

    private int resolveExerciseCaloriesBurned(User currentUser, WellnessRecordPayload payload) {
        if (payload.getExerciseCaloriesBurnedKcal() != null && payload.getExerciseCaloriesBurnedKcal() > 0) {
            return payload.getExerciseCaloriesBurnedKcal();
        }

        if (payload.getExerciseDurationMinutes() == null || payload.getExerciseDurationMinutes() <= 0) {
            return 0;
        }

        BigDecimal weightKg = resolveWeightKgForExerciseEstimate(currentUser, payload);
        BigDecimal heightCm = resolveHeightCmForExerciseEstimate(currentUser);

        if (weightKg == null || heightCm == null) {
            return 0;
        }

        double met = estimateMetForExerciseType(payload.getExerciseType());
        double durationHours = payload.getExerciseDurationMinutes() / 60.0;
        double baseCalories = met * weightKg.doubleValue() * durationHours;

        double heightAdjustment = 1.0 + ((heightCm.doubleValue() - 170.0) / 500.0);
        heightAdjustment = Math.max(0.85, Math.min(1.15, heightAdjustment));

        int estimatedCalories = (int) Math.round(baseCalories * heightAdjustment);
        return Math.max(estimatedCalories, 0);
    }

    private BigDecimal resolveWeightKgForExerciseEstimate(User currentUser, WellnessRecordPayload payload) {
        if (payload.getWeightKg() != null && payload.getWeightKg() > 0) {
            return BigDecimal.valueOf(payload.getWeightKg());
        }

        List<WeightLog> logs = weightRepo.findAllByUserIdOrderByLoggedAtDesc(currentUser.getId());
        for (WeightLog log : logs) {
            if (log.getWeightKg() != null && log.getWeightKg().compareTo(BigDecimal.ZERO) > 0) {
                return log.getWeightKg();
            }
        }

        return null;
    }

    private BigDecimal resolveHeightCmForExerciseEstimate(User currentUser) {
        UserProfile profile = currentUser.getProfile();
        if (profile == null || profile.getHeightCm() == null || profile.getHeightCm().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return profile.getHeightCm();
    }

    private double estimateMetForExerciseType(String exerciseTypeRaw) {
        if (exerciseTypeRaw == null || exerciseTypeRaw.isBlank()) {
            return 5.0;
        }

        String exerciseType = exerciseTypeRaw.trim().toUpperCase();
        return switch (exerciseType) {
            case "RUNNING", "JOGGING" -> 8.3;
            case "WALKING" -> 3.8;
            case "CYCLING" -> 7.5;
            case "SWIMMING", "HIKING" -> 6.0;
            case "HIIT", "CROSSFIT" -> 8.0;
            case "STRENGTH_TRAINING", "WEIGHTLIFTING", "BODYWEIGHT_TRAINING" -> 5.0;
            case "YOGA", "PILATES", "STRETCHING" -> 2.8;
            case "ROWING" -> 7.0;
            case "JUMP_ROPE" -> 10.0;
            case "DANCING" -> 5.0;
            case "BASKETBALL", "FOOTBALL", "BADMINTON", "TENNIS", "VOLLEYBALL", "MARTIAL_ARTS", "CLIMBING" -> 6.5;
            default -> 5.0;
        };
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
    public List<ActivityRecordDto> getActivityHistory(UUID userId, int numberOfDays) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(numberOfDays);

        List<ActivityRecord> records = activityRepo.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            userId, startTime, endTime
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
    public List<ExerciseLogResponse> getExerciseLogs(UUID userId, int numberOfDays) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(numberOfDays);

        return exerciseRepo.findByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
                userId, startTime, endTime
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
    /**
     * Author(s): Yang Mao Wei
     * Contribution:
     * - Added service logic for returning structured food logs.
     * - Mapped FoodLog entity into FoodLogResponse for frontend display.
     */
    // Returns structured meal entries (meal type, food name, calories) over a given
    // number of past days. Backs the Food Summary detail screen's Day meal list and
    // the per-day breakdown shown under its Week/Month charts.
    public List<FoodLogResponse> getFoodLogs(UUID userId, int numberOfDays) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(numberOfDays);

        return foodRepo.findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
                userId, startTime, endTime
        ).stream()
            .map(log -> FoodLogResponse.builder()
                .id(log.getId())
                .mealType(log.getMealType() != null ? log.getMealType().toString() : "OTHER")
                .foodName(log.getFoodName())
                .caloriesKcal(log.getCaloriesKcal())
                .loggedAt(log.getLoggedAt())
                .build())
            .toList();
    }

    // Deletes a single exercise session (Home "Activity Tracked" list's delete button)
    // and reverses its contribution to that day's aggregated summary (distance,
    // calories burned, exercise minutes), plus the matching activity-feed entry.
    @Transactional
    public void deleteExerciseLog(UUID userId, Long logId) {
        ExerciseLog log = exerciseRepo.findById(logId)
            .filter(l -> l.getUser().getId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Exercise log not found"));

        LocalDate summaryDate = log.getLoggedAt().toLocalDate();
        summaryRepo.findByUserIdAndSummaryDate(userId, summaryDate).ifPresent(summary -> {
            summary.setTotalExerciseMinutes(
                Math.max(0, summary.getTotalExerciseMinutes() - log.getDurationMinutes()));

            if (log.getDistanceKm() != null && summary.getTotalDistanceKm() != null) {
                BigDecimal newDistance = summary.getTotalDistanceKm().subtract(log.getDistanceKm());
                summary.setTotalDistanceKm(newDistance.max(BigDecimal.ZERO));
            }

            if (log.getCaloriesBurnedKcal() != null) {
                summary.setTotalCaloriesBurned(
                    Math.max(0, summary.getTotalCaloriesBurned() - log.getCaloriesBurnedKcal()));
            }

            summaryRepo.save(summary);
        });

        activityRepo.deleteBySourceLogIdAndActivityType(logId, ActivityType.EXERCISE);
        exerciseRepo.delete(log);
    }

    // Buckets a single date's exercise (distance/calories) and hydration (water) logs
    // by the hour of their timestamp, returning a fixed 24-entry list (zero-filled for
    // empty hours). Backs the Distance/Calories/Hydration detail screens' Day view.
    public List<HourlyWellnessResponse> getHourlySummary(UUID userId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        BigDecimal[] distanceByHour = new BigDecimal[24];
        int[] caloriesByHour = new int[24];
        int[] waterByHour = new int[24];
        for (int i = 0; i < 24; i++) {
            distanceByHour[i] = BigDecimal.ZERO;
        }

        for (ExerciseLog log : exerciseRepo.findByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(userId, dayStart, dayEnd)) {
            LocalDateTime attributedAt = log.getStartTime() != null ? log.getStartTime() : log.getLoggedAt();
            int hour = attributedAt.getHour();
            if (log.getDistanceKm() != null) {
                distanceByHour[hour] = distanceByHour[hour].add(log.getDistanceKm());
            }
            caloriesByHour[hour] += log.getCaloriesBurnedKcal() != null ? log.getCaloriesBurnedKcal() : 0;
        }

        for (HydrationLog log : hydrationRepo.findByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(userId, dayStart, dayEnd)) {
            int hour = log.getLoggedAt().getHour();
            waterByHour[hour] += log.getVolumeMl() != null ? log.getVolumeMl() : 0;
        }

        List<HourlyWellnessResponse> result = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            result.add(HourlyWellnessResponse.builder()
                .hour(hour)
                .distanceKm(distanceByHour[hour])
                .caloriesBurnedKcal(caloriesByHour[hour])
                .waterMl(waterByHour[hour])
                .build());
        }
        return result;
    }

    // All-time / rolling aggregates backing the Badges grid's achievement conditions.
    // Threshold comparisons (e.g. >= 1000km) live client-side alongside the badge
    // descriptions; this just supplies the raw numbers.
    public BadgeProgressResponse getBadgeProgress(UUID userId) {
        List<ExerciseLog> exerciseLogs = exerciseRepo.findAllByUserIdOrderByLoggedAtDesc(userId);

        BigDecimal totalRunDistanceKm = exerciseLogs.stream()
            .filter(log -> log.getExerciseType() == ExerciseType.RUNNING && log.getDistanceKm() != null)
            .map(ExerciseLog::getDistanceKm)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalCaloriesBurned = exerciseLogs.stream()
            .mapToInt(log -> log.getCaloriesBurnedKcal() != null ? log.getCaloriesBurnedKcal() : 0)
            .sum();

        int distinctExerciseDays = (int) exerciseLogs.stream()
            .map(log -> log.getLoggedAt().toLocalDate())
            .distinct()
            .count();

        int totalHydrationMl = hydrationRepo.findAllByUserIdOrderByLoggedAtDesc(userId).stream()
            .mapToInt(log -> log.getVolumeMl() != null ? log.getVolumeMl() : 0)
            .sum();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Integer> recentSleepMinutes = sleepRepo.findAllByUserIdOrderByStartTimeDesc(userId).stream()
            .filter(log -> log.getStartTime().isAfter(thirtyDaysAgo))
            .map(SleepLog::getDurationMinutes)
            .filter(minutes -> minutes != null)
            .toList();
        Double avgSleepHoursLast30Days = recentSleepMinutes.isEmpty() ? null :
            recentSleepMinutes.stream().mapToInt(Integer::intValue).average().orElse(0.0) / 60.0;

        List<Integer> recentMoodRatings = moodRepo.findAllByUserIdOrderByLoggedAtDesc(userId).stream()
            .filter(log -> log.getLoggedAt().isAfter(thirtyDaysAgo))
            .map(MoodLog::getMoodRating)
            .toList();
        Double avgMoodLast30Days = recentMoodRatings.isEmpty() ? null :
            recentMoodRatings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        LocalDate today = LocalDate.now();
        BigDecimal todayWeightKg = weightRepo.findAllByUserIdOrderByLoggedAtDesc(userId).stream()
            .filter(log -> log.getLoggedAt().toLocalDate().equals(today))
            .findFirst()
            .map(WeightLog::getWeightKg)
            .orElse(null);

        return BadgeProgressResponse.builder()
            .totalRunDistanceKm(totalRunDistanceKm)
            .totalCaloriesBurned(totalCaloriesBurned)
            .totalHydrationMl(totalHydrationMl)
            .avgSleepHoursLast30Days(avgSleepHoursLast30Days)
            .avgMoodLast30Days(avgMoodLast30Days)
            .distinctExerciseDays(distinctExerciseDays)
            .todayWeightKg(todayWeightKg)
            .build();
    }

    // Wipes today's logged wellness data (sleep/hydration/weight/mood/exercise, the
    // generic activity feed, and the aggregated daily summary) for the given user,
    // leaving user_goals and user_profile untouched. Used to reset a test account
    // to a clean slate on every app login.
    @Transactional
    public void resetToday(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        sleepRepo.deleteByUserIdAndStartTimeBetween(userId, startOfDay, endOfDay);
        hydrationRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        weightRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        moodRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        exerciseRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        // Food logs must reset alongside the summary, or the Food Summary card
        // (summary.totalCaloriesIntake, wiped below) reads 0 while the detail
        // screen (raw food_logs) still lists today's meals.
        foodRepo.deleteByUserIdAndLoggedAtBetween(userId, startOfDay, endOfDay);
        activityRepo.deleteByUserIdAndRecordedAtBetween(userId, startOfDay, endOfDay);
        summaryRepo.deleteByUserIdAndSummaryDate(userId, today);
    }

     // Fetch Date Range (Just the Summaries for charts/graphs)
    public List<DailyWellnessSummary> getSummaryRange(User user, LocalDate startDate, LocalDate endDate) {
        return summaryRepo.findByUserIdAndSummaryDateBetweenOrderBySummaryDateAsc(
            user.getId(), startDate, endDate
        );
    }
}

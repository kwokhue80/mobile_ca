package sg.edu.nus.features.wellness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import sg.edu.nus.features.wellness.dto.WellnessLogRequest;
import sg.edu.nus.features.wellness.model.ExerciseLogDetail;
import sg.edu.nus.features.wellness.model.FoodLogDetail;
import sg.edu.nus.features.wellness.model.SleepLogDetail;
import sg.edu.nus.features.wellness.model.WellnessLog;
import sg.edu.nus.features.wellness.repository.ExerciseLogDetailRepository;
import sg.edu.nus.features.wellness.repository.FoodLogDetailRepository;
import sg.edu.nus.features.wellness.repository.SleepLogDetailRepository;
import sg.edu.nus.features.wellness.repository.WellnessLogRepository;

@Service
public class WellnessService {

    private final WellnessLogRepository wellnessLogRepository;
    private final FoodLogDetailRepository foodLogDetailRepository;
    private final ExerciseLogDetailRepository exerciseLogDetailRepository;
    private final SleepLogDetailRepository sleepLogDetailRepository;

    public WellnessService(
            WellnessLogRepository wellnessLogRepository,
            FoodLogDetailRepository foodLogDetailRepository,
            ExerciseLogDetailRepository exerciseLogDetailRepository,
            SleepLogDetailRepository sleepLogDetailRepository
    ) {
        this.wellnessLogRepository = wellnessLogRepository;
        this.foodLogDetailRepository = foodLogDetailRepository;
        this.exerciseLogDetailRepository = exerciseLogDetailRepository;
        this.sleepLogDetailRepository = sleepLogDetailRepository;
    }

    public WellnessLog saveWellnessLog(WellnessLogRequest request) {
        WellnessLog wellnessLog = new WellnessLog();

        wellnessLog.setUserId("demo-user");
        wellnessLog.setMetricType(request.getMetric_type());
        wellnessLog.setNotes(request.getNotes());
        wellnessLog.setRecordedAt(
                request.getRecorded_at() != null
                        ? request.getRecorded_at()
                        : LocalDateTime.now()
        );

        WellnessLog savedLog = wellnessLogRepository.save(wellnessLog);

        String metricType = request.getMetric_type();

        if ("FOOD".equalsIgnoreCase(metricType)) {
            saveFoodDetails(savedLog.getId(), request.getDetails());
        } else if ("ACTIVITY".equalsIgnoreCase(metricType)) {
            saveExerciseDetails(savedLog.getId(), request.getDetails());
        } else if ("SLEEP".equalsIgnoreCase(metricType)) {
            saveSleepDetails(savedLog.getId(), request.getDetails());
        }

        return savedLog;
    }

    private void saveFoodDetails(Long metricId, Map<String, Object> details) {
        if (details == null) {
            return;
        }

        FoodLogDetail foodLogDetail = new FoodLogDetail();
        foodLogDetail.setMetricId(metricId);
        foodLogDetail.setMealType(readString(details, "meal_type"));
        foodLogDetail.setFoodName(readString(details, "food_name"));
        foodLogDetail.setCalories(readBigDecimal(details, "calories"));

        foodLogDetailRepository.save(foodLogDetail);
    }

    private void saveExerciseDetails(Long metricId, Map<String, Object> details) {
        if (details == null) {
            return;
        }

        ExerciseLogDetail exerciseLogDetail = new ExerciseLogDetail();
        exerciseLogDetail.setMetricId(metricId);
        exerciseLogDetail.setActivityType(readString(details, "activity_type"));
        exerciseLogDetail.setDistanceKm(readBigDecimal(details, "distance_km"));
        exerciseLogDetail.setCaloriesBurned(readBigDecimal(details, "calories_burned"));

        exerciseLogDetailRepository.save(exerciseLogDetail);
    }

    private void saveSleepDetails(Long metricId, Map<String, Object> details) {
        if (details == null) {
            return;
        }

        SleepLogDetail sleepLogDetail = new SleepLogDetail();
        sleepLogDetail.setMetricId(metricId);
        sleepLogDetail.setSleepStart(readLocalDateTime(details, "sleep_start"));
        sleepLogDetail.setSleepEnd(readLocalDateTime(details, "sleep_end"));
        sleepLogDetail.setQualityScore(readInteger(details, "quality_score"));

        sleepLogDetailRepository.save(sleepLogDetail);
    }

    private String readString(Map<String, Object> details, String key) {
        Object value = details.get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private BigDecimal readBigDecimal(Map<String, Object> details, String key) {
        Object value = details.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer readInteger(Map<String, Object> details, String key) {
        Object value = details.get(key);

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime readLocalDateTime(Map<String, Object> details, String key) {
        Object value = details.get(key);

        if (value == null) {
            return null;
        }

        return LocalDateTime.parse(value.toString());
    }
}
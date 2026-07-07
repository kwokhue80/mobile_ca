package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class WellnessRecordPayload {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordDate;
    private Integer sleepMinutes;
    private Integer sleepQualityRating;
    private String mealType;
    private String mealDescription;
    private Integer mealCaloriesKcal;
    private Double weightKg;
    private Integer waterIntakeMl;
    private String exerciseType;
    private Integer exerciseDurationMinutes;
    private Double exerciseDistanceKm;
    private Integer exerciseCaloriesBurnedKcal;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exerciseStartTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exerciseEndTime;

    private Integer moodRating;
}
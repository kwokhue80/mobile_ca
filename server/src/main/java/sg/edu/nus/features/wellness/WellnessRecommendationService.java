package sg.edu.nus.features.wellness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.goal.UserGoalService;
import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.enums.GoalType;
import sg.edu.nus.features.wellness.dto.RecommendationResponse;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.repository.DailyWellnessSummaryRepository;

@Service
@RequiredArgsConstructor
public class WellnessRecommendationService {

    private final DailyWellnessSummaryRepository summaryRepository;
    private final UserGoalService userGoalService;

    public RecommendationResponse getLatestRecommendation(User currentUser) {
        Map<GoalType, BigDecimal> goalTargets = new HashMap<>();
        for (UserGoal goal : userGoalService.getByUserId(currentUser.getId())) {
            goalTargets.put(goal.getId().getGoalType(), goal.getTargetValue());
        }

        DailyWellnessSummary latestSummary = summaryRepository
            .findAllByUserIdOrderBySummaryDateDesc(currentUser.getId())
            .stream()
            .findFirst()
            .orElse(null);

        String recommendation;
        if (latestSummary == null) {
            recommendation = "Start by logging a meal, hydration, exercise, or sleep entry today so we can personalize your wellness tips.";
        } else {
            BigDecimal hydrationTarget = goalTargets.getOrDefault(GoalType.HYDRATION, BigDecimal.valueOf(2000));
            BigDecimal sleepTargetMinutes = goalTargets
                .getOrDefault(GoalType.SLEEP, BigDecimal.valueOf(7))
                .multiply(BigDecimal.valueOf(60));
            BigDecimal caloriesTarget = goalTargets.getOrDefault(GoalType.CALORIES, BigDecimal.valueOf(300));

            BigDecimal waterActual = BigDecimal.valueOf(latestSummary.getTotalWaterMl() != null ? latestSummary.getTotalWaterMl() : 0);
            BigDecimal sleepActual = BigDecimal.valueOf(latestSummary.getSleepMinutes() != null ? latestSummary.getSleepMinutes() : 0);
            BigDecimal caloriesActual = BigDecimal.valueOf(latestSummary.getTotalCaloriesBurned() != null ? latestSummary.getTotalCaloriesBurned() : 0);
            BigDecimal exerciseActual = BigDecimal.valueOf(latestSummary.getTotalExerciseMinutes() != null ? latestSummary.getTotalExerciseMinutes() : 0);
            BigDecimal exerciseTarget = BigDecimal.valueOf(30);

            List<NeedScore> needs = List.of(
                NeedScore.of("hydration", hydrationTarget, waterActual,
                    String.format(
                        "Hydration is below your goal (%d/%d ml). Add 2 to 3 glasses of water in the next few hours.",
                        waterActual.intValue(),
                        hydrationTarget.intValue()
                    )),
                NeedScore.of("sleep", sleepTargetMinutes, sleepActual,
                    String.format(
                        "Sleep is below target today (%d/%d minutes). Try winding down earlier for better recovery.",
                        sleepActual.intValue(),
                        sleepTargetMinutes.intValue()
                    )),
                NeedScore.of("calories", caloriesTarget, caloriesActual,
                    String.format(
                        "Active calories are below goal (%d/%d kcal). Add a short walk or light session today.",
                        caloriesActual.intValue(),
                        caloriesTarget.intValue()
                    )),
                NeedScore.of("exercise", exerciseTarget, exerciseActual,
                    String.format(
                        "Movement is low today (%d/%d minutes). A 10 to 15 minute activity break can help.",
                        exerciseActual.intValue(),
                        exerciseTarget.intValue()
                    ))
            );

            recommendation = needs.stream()
                .filter(need -> need.deficitRatio.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(need -> need.deficitRatio))
                .map(need -> need.message)
                .orElse("Great consistency today. Keep your hydration, sleep, and activity patterns steady.");
        }

        // Generate recommendation timestamp in SGT and round to 3-hour buckets for poller cadence.
        ZoneId sgtZone = ZoneId.of("Asia/Singapore");
        ZonedDateTime nowSgt = ZonedDateTime.now(sgtZone);
        int bucketHour = (nowSgt.getHour() / 3) * 3;
        OffsetDateTime generatedAt = nowSgt
            .withHour(bucketHour)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toOffsetDateTime();

        return new RecommendationResponse(recommendation, generatedAt);
    }

    private record NeedScore(String name, BigDecimal deficitRatio, String message) {
        static NeedScore of(String name, BigDecimal target, BigDecimal actual, String message) {
            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
                return new NeedScore(name, BigDecimal.ZERO, message);
            }

            BigDecimal deficit = target.subtract(actual == null ? BigDecimal.ZERO : actual);
            BigDecimal ratio = deficit.compareTo(BigDecimal.ZERO) > 0
                ? deficit.divide(target, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            return new NeedScore(name, ratio, message);
        }
    }
}

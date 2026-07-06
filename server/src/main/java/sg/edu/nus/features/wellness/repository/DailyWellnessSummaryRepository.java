package sg.edu.nus.features.wellness.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sg.edu.nus.features.wellness.model.DailyWellnessSummary;

public interface DailyWellnessSummaryRepository extends JpaRepository<DailyWellnessSummary, Long> {

	List<DailyWellnessSummary> findAllByUserIdOrderBySummaryDateDesc(UUID userId);

	Optional<DailyWellnessSummary> findByUserIdAndSummaryDate(UUID userId, LocalDate summaryDate);

	List<DailyWellnessSummary> findByUserIdAndSummaryDateBetweenOrderBySummaryDateAsc(
		UUID userId, 
		LocalDate startDate, 
		LocalDate endDate
	);

    // UPDATE quantifiable metrics; only fields non null
	@Modifying
	@Query("""
		UPDATE DailyWellnessSummary d
		SET d.totalWaterMl = d.totalWaterMl + COALESCE(:waterMlDelta, 0),
			d.totalCaloriesIntake = d.totalCaloriesIntake + COALESCE(:caloriesIntakeDelta, 0),
			d.totalCaloriesBurned = d.totalCaloriesBurned + COALESCE(:caloriesBurnedDelta, 0),
			d.totalExerciseMinutes = d.totalExerciseMinutes + COALESCE(:exerciseMinutesDelta, 0)
		WHERE d.user.id = :userId
        AND d.summaryDate = :summaryDate
	""")
	int incrementTotals(
		@Param("userId") UUID userId,
		@Param("summaryDate") LocalDate summaryDate,
		@Param("waterMlDelta") Integer waterMlDelta,
		@Param("caloriesIntakeDelta") Integer caloriesIntakeDelta,
		@Param("caloriesBurnedDelta") Integer caloriesBurnedDelta,
		@Param("exerciseMinutesDelta") Integer exerciseMinutesDelta
	);

    // Only update non-null fields
	@Modifying
	@Query("""
		UPDATE DailyWellnessSummary d
		SET d.sleepMinutes = COALESCE(:sleepMinutes, d.sleepMinutes),
			d.sleepQualityScore = COALESCE(:sleepQualityScore, d.sleepQualityScore),
			d.moodScore = COALESCE(:moodScore, d.moodScore),
			d.weightKg = COALESCE(:weightKg, d.weightKg)
		WHERE d.user.id = :userId
        AND d.summaryDate = :summaryDate
	""")
	int updateLatestMetrics(
		@Param("userId") UUID userId,
		@Param("summaryDate") LocalDate summaryDate,
		@Param("sleepMinutes") Integer sleepMinutes,
		@Param("sleepQualityScore") Integer sleepQualityScore,
		@Param("moodScore") Integer moodScore,
		@Param("weightKg") BigDecimal weightKg
	);

}

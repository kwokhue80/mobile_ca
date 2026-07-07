package sg.edu.nus.features.wellness.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.common.Updatable;
import sg.edu.nus.features.user.account.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
	name = "daily_wellness_summary",
	uniqueConstraints = @UniqueConstraint(name = "uk_summary_user_date", columnNames = { "user_id", "summary_date" })
)
public class DailyWellnessSummary extends Updatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Builder.Default
	@Column(name = "total_water_ml", nullable = false)
	private Integer totalWaterMl = 0;

	@Builder.Default
	@Column(name = "total_calories_intake", nullable = false)
	private Integer totalCaloriesIntake = 0;

	@Builder.Default
	@Column(name = "total_calories_burned", nullable = false)
	private Integer totalCaloriesBurned = 0;

	@Builder.Default
	@Column(name = "total_exercise_minutes", nullable = false)
	private Integer totalExerciseMinutes = 0;

	@Builder.Default
	@Column(name = "total_distance_km", nullable = false, precision = 6, scale = 2)
	private BigDecimal totalDistanceKm = BigDecimal.ZERO;

	@Column(name = "sleep_minutes")
	private Integer sleepMinutes;

	@Column(name = "sleep_quality_score")
	private Integer sleepQualityScore;

	@Column(name = "mood_score")
	private Integer moodScore;

	@Column(name = "weight_kg", precision = 5, scale = 2)
	private BigDecimal weightKg;

    // ASSOCIATIONS
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User user;

}

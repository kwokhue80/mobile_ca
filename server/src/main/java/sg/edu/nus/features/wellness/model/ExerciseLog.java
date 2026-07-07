package sg.edu.nus.features.wellness.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.common.Creatable;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.wellness.model.enums.ExerciseType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exercise_logs")
public class ExerciseLog extends Creatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Enumerated(EnumType.STRING)
	@Column(name = "exercise_type", length = 100, nullable = false)
	private ExerciseType exerciseType;

	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;

	@Column(name = "distance_km", precision = 6, scale = 2)
	private BigDecimal distanceKm;

	@Column(name = "calories_burned_kcal", nullable = false)
	private Integer caloriesBurnedKcal;

	@Column(name = "logged_at", nullable = false)
	private LocalDateTime loggedAt;

    // ASSOCIATIONS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	@JsonIgnore
	private User user;

}

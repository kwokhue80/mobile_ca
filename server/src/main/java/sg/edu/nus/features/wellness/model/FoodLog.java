package sg.edu.nus.features.wellness.model;

import java.time.LocalDateTime;

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
import sg.edu.nus.features.wellness.model.enums.MealType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "food_logs")
public class FoodLog extends Creatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Enumerated(EnumType.STRING)
	@Column(name = "meal_type", length = 20)
	private MealType mealType;

	@Column(name = "food_name", length = 255, nullable = false)
	private String foodName;

	@Column(name = "calories_kcal", nullable = false)
	private Integer caloriesKcal;

	@Column(name = "logged_at", nullable = false)
	private LocalDateTime loggedAt;

    // ASSOCIATIONS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User user;

}

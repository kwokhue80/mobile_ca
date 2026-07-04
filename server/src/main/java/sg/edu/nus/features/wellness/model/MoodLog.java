package sg.edu.nus.features.wellness.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.common.Creatable;
import sg.edu.nus.features.user.account.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mood_logs")
public class MoodLog extends Creatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "logged_at", nullable = false)
	private LocalDateTime loggedAt;

    @Min(0)
    @Max(10)
	@Column(name = "mood_rating", nullable = false)
	private Integer moodRating;

	@Column(name = "notes", length = 255)
	private String notes;

    // ASSOCIATIONS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User user;

}

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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.common.Creatable;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.wellness.model.enums.ActivityType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activity_records")
public class ActivityRecord extends Creatable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "source_log_id", nullable = false)
	private Long sourceLogId;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_type", length = 50, nullable = false)
	private ActivityType activityType;

	@Column(name = "title", length = 100, nullable = false)
	private String title;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;
    
    // ASSOCIATIONS
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User user;

}

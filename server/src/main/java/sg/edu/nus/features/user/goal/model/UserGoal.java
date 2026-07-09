// AUTHOR: Amelia
package sg.edu.nus.features.user.goal.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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
@Table(name = "user_goals")
public class UserGoal extends Updatable {

    @EmbeddedId // Composite key: User ID + Goal Type
    private UserGoalId id;

    @Column(name = "target_value", precision = 8, scale = 2, nullable = false)
    private BigDecimal targetValue;

    // ASSOCIATIONS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

}

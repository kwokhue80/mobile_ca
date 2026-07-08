package sg.edu.nus.features.user.goal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sg.edu.nus.features.user.goal.dto.UserGoalRawView;
import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.UserGoalId;

public interface UserGoalRepository extends JpaRepository<UserGoal, UserGoalId> {
    List<UserGoal> findByUserId(UUID userId);

    @Query("""
        SELECT
            ug.id.goalType AS goalType,
            ug.targetValue AS targetValue
        FROM UserGoal ug
        WHERE ug.user.id = ?1
    """)
    List<UserGoalRawView> findRawByUserId(UUID userId);
}

package sg.edu.nus.features.user.goal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.UserGoalId;

public interface UserGoalRepository extends JpaRepository<UserGoal, UserGoalId> {

    // READ
    List<UserGoal> findByIdUserId(UUID userId);

}

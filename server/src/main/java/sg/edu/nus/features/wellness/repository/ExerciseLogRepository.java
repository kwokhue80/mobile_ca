package sg.edu.nus.features.wellness.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.ExerciseLog;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

	List<ExerciseLog> findAllByUserIdOrderByLoggedAtDesc(UUID userId);

}

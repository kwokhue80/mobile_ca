package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.WeightLog;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

	List<WeightLog> findAllByUserIdOrderByLoggedAtDesc(UUID userId);

	List<WeightLog> findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
			UUID userId, LocalDateTime start, LocalDateTime end);

	void deleteByUserIdAndLoggedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

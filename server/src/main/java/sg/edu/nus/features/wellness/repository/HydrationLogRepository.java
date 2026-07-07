package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.HydrationLog;

public interface HydrationLogRepository extends JpaRepository<HydrationLog, Long> {

	List<HydrationLog> findAllByUserIdOrderByLoggedAtDesc(UUID userId);

	List<HydrationLog> findByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
			UUID userId, LocalDateTime start, LocalDateTime end);

	void deleteByUserIdAndLoggedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

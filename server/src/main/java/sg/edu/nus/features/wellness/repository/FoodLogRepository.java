package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.FoodLog;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {
	/**
	 * Author(s): Yang Mao Wei
	 * Contribution:
	 * - Added repository query for fetching food logs within a date range.
	 * - Supported Food Summary history and chart data retrieval.
	 */
	List<FoodLog> findAllByUserIdOrderByLoggedAtDesc(UUID userId);

	List<FoodLog> findAllByUserIdAndLoggedAtBetweenOrderByLoggedAtDesc(
			UUID userId, LocalDateTime start, LocalDateTime end);

	void deleteByUserIdAndLoggedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

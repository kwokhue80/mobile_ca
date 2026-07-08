package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.SleepLog;

public interface SleepLogRepository extends JpaRepository<SleepLog, Long> {

	List<SleepLog> findAllByUserIdOrderByStartTimeDesc(UUID userId);

	// Sleep is attributed to the wake-up date, so range queries go by endTime.
	List<SleepLog> findAllByUserIdAndEndTimeBetweenOrderByEndTimeDesc(
			UUID userId, LocalDateTime start, LocalDateTime end);

	void deleteByUserIdAndStartTimeBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

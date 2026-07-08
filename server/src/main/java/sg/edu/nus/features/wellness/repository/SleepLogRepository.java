package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.SleepLog;

public interface SleepLogRepository extends JpaRepository<SleepLog, Long> {

	List<SleepLog> findAllByUserIdOrderByStartTimeDesc(UUID userId);

	void deleteByUserIdAndStartTimeBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

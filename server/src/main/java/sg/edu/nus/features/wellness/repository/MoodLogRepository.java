// AUTHOR: Amelia Wong, Huayuan Xie
package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.MoodLog;

public interface MoodLogRepository extends JpaRepository<MoodLog, Long> {

	List<MoodLog> findAllByUserIdOrderByLoggedAtDesc(UUID userId);

	void deleteByUserIdAndLoggedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

}

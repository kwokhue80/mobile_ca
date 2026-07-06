package sg.edu.nus.features.wellness.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.ActivityRecord;
import sg.edu.nus.features.wellness.model.enums.ActivityType;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

	List<ActivityRecord> findAllByUserIdOrderByRecordedAtDesc(UUID userId);

    Optional<ActivityRecord> findByUserIdAndActivityTypeAndRecordedAt(UUID userId, ActivityType activityType, LocalDateTime recordedAt);

    List<ActivityRecord> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
        UUID userId, 
        LocalDateTime start, 
        LocalDateTime end
    );
}

package sg.edu.nus.features.wellness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.WellnessLog;

public interface WellnessLogRepository extends JpaRepository<WellnessLog, Long> {
}
package sg.edu.nus.features.wellness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.SleepLogDetail;

public interface SleepLogDetailRepository extends JpaRepository<SleepLogDetail, Long> {
}
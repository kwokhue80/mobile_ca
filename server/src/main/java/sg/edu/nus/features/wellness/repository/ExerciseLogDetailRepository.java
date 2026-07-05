package sg.edu.nus.features.wellness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.ExerciseLogDetail;

public interface ExerciseLogDetailRepository extends JpaRepository<ExerciseLogDetail, Long> {
}
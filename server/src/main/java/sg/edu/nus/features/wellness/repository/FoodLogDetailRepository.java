package sg.edu.nus.features.wellness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.nus.features.wellness.model.FoodLogDetail;

public interface FoodLogDetailRepository extends JpaRepository<FoodLogDetail, Long> {
}
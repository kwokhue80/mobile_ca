package sg.edu.nus.features.wellness;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WellnessRepository extends JpaRepository<WellnessRecord, UUID> {
    
}

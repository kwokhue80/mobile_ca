package sg.edu.nus.features.user.profile;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
}

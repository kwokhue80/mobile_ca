// AUTHOR: Amelia Wong
package sg.edu.nus.features.user.profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

	// UPDATE (only update non-null fields)
	@Modifying
	@Query("""
		UPDATE UserProfile p
		SET p.fullName = COALESCE(:fullName, p.fullName),
			p.dateOfBirth = COALESCE(:dateOfBirth, p.dateOfBirth),
			p.gender = COALESCE(:gender, p.gender),
			p.heightCm = COALESCE(:heightCm, p.heightCm)
		WHERE p.userId = :userId
	""")
	int updateProfileByUserId(
		@Param("userId") UUID userId,
		@Param("fullName") String fullName,
		@Param("dateOfBirth") LocalDate dateOfBirth,
		@Param("gender") String gender,
		@Param("heightCm") BigDecimal heightCm
	);

    // DELETE not allowed
    
}

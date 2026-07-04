package sg.edu.nus.features.user.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/*
*   AUTHOR: Amelia
*   PURPOSE: User repository connecting to MySQL via Jpa
*/
public interface UserRepository extends JpaRepository<User, UUID> {

    // READ
    Optional<User> findByEmailAddress(String emailAddress);

    boolean existsByEmailAddress(String emailAddress);

}

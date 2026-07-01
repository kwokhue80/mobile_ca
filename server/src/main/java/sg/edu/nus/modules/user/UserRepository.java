package sg.edu.nus.modules.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/*
*   AUTHOR: Amelia
*   PURPOSE: User repository connecting to MySQL via Jpa
*/
public interface UserRepository extends JpaRepository<User, UUID> {

    public Optional<User> findByEmailAddress(String emailAddress);

    public boolean existsByEmailAddress(String emailAddress);

}

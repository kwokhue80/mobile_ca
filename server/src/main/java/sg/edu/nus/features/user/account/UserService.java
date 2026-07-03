package sg.edu.nus.features.user.account;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/*
*   AUTHOR: Amelia
*   PURPOSE: User service to interact with MySQL database for non-auth user functions
*/
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ---------- CRUD ---------- //
    public User save(User user) {
        return userRepository.save(user);
    }

    public User getByEmail(String emailAddress) {
        return userRepository.findByEmailAddress(emailAddress)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void disableUser(UUID userId) {
        User user = getById(userId);
        user.setEnabled(false); // Disable account
        userRepository.save(user);
    }

    // Transactional - involves deletion of all child tables
    @Transactional
    public void deleteUser(UUID userId) {
        User user = getById(userId);
        userRepository.delete(user);
    }

    public boolean existsByEmailAddress(String emailAddress) {
        return userRepository.existsByEmailAddress(emailAddress);
    }

}

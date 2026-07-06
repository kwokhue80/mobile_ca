package sg.edu.nus.features.user.profile;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.user.profile.dto.UserProfileUpdateRequest;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfile getProfile(UUID userId) {
        return userProfileRepository.findById(userId).orElse(null);
    }

    // Creates the profile row on first save, updates it on every save after that.
    @Transactional
    public UserProfile upsertProfile(User user, UserProfileUpdateRequest request) {
        validate(request);

        // `user` may be detached (fetched in a prior transaction). getReferenceById()
        // gives a proxy bound to this method's own session instead. Only the `user`
        // association is set (not `userId` directly) so @MapsId derives the id itself.
        UserProfile profile = userProfileRepository.findById(user.getId())
            .orElseGet(() -> {
                UserProfile p = new UserProfile();
                p.setUser(userRepository.getReferenceById(user.getId()));
                return p;
            });

        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setHeightCm(request.getHeightCm());

        return userProfileRepository.save(profile);
    }

    private void validate(UserProfileUpdateRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new RuntimeException("Full name is required");
        }
        if (request.getDateOfBirth() == null) {
            throw new RuntimeException("Date of birth is required");
        }
        if (request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new RuntimeException("Date of birth cannot be in the future");
        }
        if (request.getGender() == null || request.getGender().isBlank()) {
            throw new RuntimeException("Gender is required");
        }
        if (request.getHeightCm() == null || request.getHeightCm().signum() <= 0) {
            throw new RuntimeException("Height must be greater than 0");
        }
    }

}
package sg.edu.nus.features.user.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserService;
import sg.edu.nus.features.user.profile.dto.UserProfileResponse;
import sg.edu.nus.features.user.profile.dto.UserProfileUpdateRequest;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        UserProfile profile = userProfileService.getProfile(user.getId());

        UserProfileResponse response = UserProfileResponse.builder()
            .userId(user.getId())
            .emailAddress(user.getEmailAddress())
            .fullName(profile != null ? profile.getFullName() : null)
            .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
            .gender(profile != null ? profile.getGender() : null)
            .heightCm(profile != null ? profile.getHeightCm() : null)
            .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        UserProfile saved = userProfileService.upsertProfile(user, request);

        UserProfileResponse response = UserProfileResponse.builder()
            .userId(user.getId())
            .emailAddress(user.getEmailAddress())
            .fullName(saved.getFullName())
            .dateOfBirth(saved.getDateOfBirth())
            .gender(saved.getGender())
            .heightCm(saved.getHeightCm())
            .build();

        return ResponseEntity.ok(response);
    }

}
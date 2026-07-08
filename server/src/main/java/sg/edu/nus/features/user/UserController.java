package sg.edu.nus.features.user;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserService;
import sg.edu.nus.features.user.goal.UserGoalService;
import sg.edu.nus.features.user.goal.dto.UserGoalResponse;
import sg.edu.nus.features.user.goal.dto.UserGoalUpsertRequest;
import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.enums.GoalType;
import sg.edu.nus.features.user.profile.UserProfile;
import sg.edu.nus.features.user.profile.UserProfileService;
import sg.edu.nus.features.user.profile.dto.UserProfileResponse;
import sg.edu.nus.features.user.profile.dto.UserProfileUpdateRequest;
import sg.edu.nus.security.UserPrincipal;

/*
*   AUTHOR: Amelia
*   PURPOSE: User controller for client to access API on user profile changes/logout
*/
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserGoalService userGoalService;
	private final UserProfileService userProfileService;
	private final UserService userService;

	@GetMapping("/goals")
	public ResponseEntity<List<UserGoalResponse>> getCurrentUserGoals(
			@AuthenticationPrincipal UserPrincipal userPrincipal) {
		List<UserGoalResponse> goals = userGoalService.getByUserId(userPrincipal.getId())
				.stream()
				.sorted(Comparator.comparing(goal -> goal.getId().getGoalType().name()))
				.map(userGoalService::toResponse)
				.toList();
		return ResponseEntity.ok(goals);
	}

    @GetMapping("/goals/raw")
    public ResponseEntity<List<UserGoalResponse>> getCurrentUserGoalsRaw(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<UserGoalResponse> goals = userGoalService.getRawByUserId(userPrincipal.getId());
        return ResponseEntity.ok(goals);
    }

	@PutMapping("/goals/{goalType}")
	public ResponseEntity<UserGoalResponse> upsertCurrentUserGoal(
			@PathVariable GoalType goalType,
			@RequestBody UserGoalUpsertRequest request,
			@AuthenticationPrincipal UserPrincipal userPrincipal) {
		UserGoal savedGoal = userGoalService.upsertGoal(userPrincipal.getId(), goalType, request.getTargetValue());
		return ResponseEntity.ok(userGoalService.toResponse(savedGoal));
	}

	@GetMapping("/profile")
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

    @PutMapping("/profile")
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

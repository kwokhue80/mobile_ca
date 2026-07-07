package sg.edu.nus.features.user.goal;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.user.goal.dto.UserGoalResponse;
import sg.edu.nus.features.user.goal.dto.UserGoalUpsertRequest;
import sg.edu.nus.features.user.goal.model.UserGoal;
import sg.edu.nus.features.user.goal.model.enums.GoalType;

@RestController
@RequestMapping("/api/user-goals")
@RequiredArgsConstructor
public class UserGoalController {

	// Old placeholder body retained for traceability.
	// public class UserGoalController {
	// }

	private final UserGoalService userGoalService;
	private final UserRepository userRepository;

	@GetMapping
	public ResponseEntity<List<UserGoalResponse>> getCurrentUserGoals(Authentication authentication) {
		User currentUser = findAuthenticatedUser(authentication);

		List<UserGoalResponse> goals = userGoalService.getByUserId(currentUser.getId())
				.stream()
				.sorted(Comparator.comparing(goal -> goal.getId().getGoalType().name()))
				.map(this::toResponse)
				.toList();

		return ResponseEntity.ok(goals);
	}

	@PutMapping("/{goalType}")
	public ResponseEntity<UserGoalResponse> upsertCurrentUserGoal(
			@PathVariable GoalType goalType,
			@RequestBody UserGoalUpsertRequest request,
			Authentication authentication) {

		User currentUser = findAuthenticatedUser(authentication);
		UserGoal savedGoal = userGoalService.upsertGoal(currentUser.getId(), goalType, request.getTargetValue());
		return ResponseEntity.ok(toResponse(savedGoal));
	}

	private UserGoalResponse toResponse(UserGoal userGoal) {
		return new UserGoalResponse(
				userGoal.getId().getGoalType().name(),
				userGoal.getTargetValue(),
				userGoal.getId().getGoalType().getUnit());
	}

	private User findAuthenticatedUser(Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			throw new RuntimeException("User is not authenticated");
		}

		String emailAddress = authentication.getName();
		return userRepository.findByEmailAddress(emailAddress)
				.orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
	}
}

package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserService;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;
import sg.edu.nus.features.wellness.dto.ActivityRecordDto;
import sg.edu.nus.features.wellness.dto.ExerciseLogResponse;
import sg.edu.nus.features.wellness.dto.RecommendationResponse;

@Slf4j
@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessOrchestratorService orchestratorService;
    private final WellnessRecommendationService wellnessRecommendationService;
    private final UserService userService;

    @PostMapping("/records")
    public ResponseEntity<Void> saveRecord(
            @RequestBody WellnessRecordPayload payload,
            Authentication authentication) {
        User currentUser = findAuthenticatedUser(authentication);

        log.info("Received wellness record payload from user email: {}", currentUser.getEmailAddress());
        log.info("Payload recordDate: {}", payload.getRecordDate());

        orchestratorService.processMonolithicRecord(currentUser, payload);

        log.info("Successfully processed wellness record for user ID: {}", currentUser.getId());

        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityRecordDto>> getActivityHistory(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        User currentUser = findAuthenticatedUser(authentication);
        List<ActivityRecordDto> history = orchestratorService.getActivityHistory(currentUser, days);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/exercise-logs")
    public ResponseEntity<List<ExerciseLogResponse>> getExerciseLogs(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        User currentUser = findAuthenticatedUser(authentication);
        List<ExerciseLogResponse> logs = orchestratorService.getExerciseLogs(currentUser, days);
        return ResponseEntity.ok(logs);
    }

    // Testing convenience: wipes today's logged wellness data for the current user
    // (sleep/hydration/weight/mood/exercise + the aggregated daily summary), leaving
    // goals and profile untouched. Called by the client on every login.
    @PostMapping("/reset-today")
    public ResponseEntity<Void> resetToday(Authentication authentication) {
        User currentUser = findAuthenticatedUser(authentication);
        orchestratorService.resetToday(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recommendations/latest")
    public ResponseEntity<RecommendationResponse> getLatestRecommendation(Authentication authentication) {
        User currentUser = findAuthenticatedUser(authentication);

        RecommendationResponse recommendation = wellnessRecommendationService.getLatestRecommendation(currentUser);
        return ResponseEntity.ok(recommendation);
    }

    private User findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User is not authenticated");
        }

        return userService.getByEmail(authentication.getName());
    }
}
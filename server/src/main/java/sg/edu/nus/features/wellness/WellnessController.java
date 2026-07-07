package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.security.UserPrincipal;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;
import sg.edu.nus.features.wellness.dto.ActivityRecordDto;
import sg.edu.nus.features.wellness.dto.ExerciseLogResponse;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.model.ExerciseLog;
import sg.edu.nus.features.wellness.dto.RecommendationResponse;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessOrchestratorService orchestratorService;

    @PostMapping("/records")
    public ResponseEntity<Void> saveRecord(
        @RequestBody WellnessRecordPayload payload,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Received wellness record payload from user email: {}", userPrincipal.getUsername());
        log.info("Payload recordDate: {}", payload.getRecordDate());

        orchestratorService.processMonolithicRecord(userPrincipal.getId(), payload);
        log.info("Successfully processed wellness record for user ID: {}", userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<DailyWellnessSummary> getDailyWellnessSummary(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DailyWellnessSummary summary = orchestratorService.getDailyWellnessSummary(userPrincipal.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/weekly-exercise")
    public ResponseEntity<List<ExerciseLog>> getWeeklyExercise(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ExerciseLog> history = orchestratorService.getWeeklyExercise(userPrincipal.getId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/exercise-logs")
    public ResponseEntity<List<ExerciseLogResponse>> getExerciseLogs(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ExerciseLogResponse> logs = orchestratorService.getExerciseLogs(userPrincipal.getId(), days);
        return ResponseEntity.ok(logs);
    }

    // Testing convenience: wipes today's logged wellness data for the current user
    // (sleep/hydration/weight/mood/exercise + the aggregated daily summary), leaving
    // goals and profile untouched. Called by the client on every login.
    @PostMapping("/reset-today")
    public ResponseEntity<Void> resetToday(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        orchestratorService.resetToday(userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getLatestRecommendation(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecommendationResponse recommendation = orchestratorService.getLatestRecommendation(userPrincipal.getId());
        return ResponseEntity.ok(recommendation);
    }

    // @GetMapping("/activity")
    // public ResponseEntity<List<ActivityRecordDto>> getActivityHistory(
    //     @RequestParam(defaultValue = "7") int days,
    //     @AuthenticationPrincipal UserPrincipal userPrincipal) {
    //     List<ActivityRecordDto> history = orchestratorService.getActivityHistory(userPrincipal.getId(), days);
    //     return ResponseEntity.ok(history);
    // }

}
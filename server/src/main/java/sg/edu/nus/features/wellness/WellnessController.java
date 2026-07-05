package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.wellness.dto.WellnessLogRequest;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;
import sg.edu.nus.features.wellness.model.WellnessLog;


@RestController
@RequestMapping("/api/wellness")
public class WellnessController {

    private final WellnessService wellnessService;
    private final WellnessOrchestratorService orchestratorService;
    private final UserRepository userRepository;

    public WellnessController(
            WellnessService wellnessService,
            WellnessOrchestratorService orchestratorService,
            UserRepository userRepository
    ) {
        this.wellnessService = wellnessService;
        this.orchestratorService = orchestratorService;
        this.userRepository = userRepository;
    }

    @PostMapping("/logs")
    public ResponseEntity<WellnessLog> saveWellnessLog(
            @RequestBody WellnessLogRequest request
    ) {
        WellnessLog savedLog = wellnessService.saveWellnessLog(request);
        return ResponseEntity.ok(savedLog);
    }

    @PostMapping("/records")
    public ResponseEntity<Void> saveRecord(
            @RequestBody WellnessRecordPayload payload,
            @AuthenticationPrincipal String email
    ) {
        User currentUser = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        orchestratorService.processMonolithicRecord(currentUser, payload);

        return ResponseEntity.ok().build();
    }
}
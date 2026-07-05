package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;
import sg.edu.nus.security.UserPrincipal;


@Slf4j
@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {
    
    private final WellnessOrchestratorService orchestratorService;
    private final UserRepository userRepository;

    @PostMapping("/records")
    public ResponseEntity<Void> saveRecord(
            @RequestBody WellnessRecordPayload payload,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Received wellness record payload from user ID: {}", principal.getUsername());

        // Fetch the fully managed User entity from the database using the ID from the JWT
        User currentUser = userRepository.findByEmailAddress(principal.getUsername())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        // Pass to the orchestrator for transaction-safe processing
        orchestratorService.processMonolithicRecord(currentUser, payload);
        
        log.info("Successfully processed wellness record for user ID: {}", principal.getId());
        
        return ResponseEntity.ok().build();
    }
}

package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sg.edu.nus.features.user.account.User;
import sg.edu.nus.features.user.account.UserRepository;
import sg.edu.nus.features.wellness.dto.WellnessRecordPayload;

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
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User is not authenticated");
        }

        // JwtAuthenticationFilter stores the email as authentication name.
        String emailAddress = authentication.getName();

        log.info("Received wellness record payload from user email: {}", emailAddress);
        log.info("Payload recordDate: {}", payload.getRecordDate());

        User currentUser = userRepository.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

        orchestratorService.processMonolithicRecord(currentUser, payload);

        log.info("Successfully processed wellness record for user ID: {}", currentUser.getId());

        return ResponseEntity.ok().build();
    }
}
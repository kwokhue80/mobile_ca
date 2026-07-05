package sg.edu.nus.features.wellness;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.features.wellness.dto.WellnessLogRequest;
import sg.edu.nus.features.wellness.model.WellnessLog;

@RestController
@RequestMapping("/api/wellness")
public class WellnessController {

    private final WellnessService wellnessService;

    public WellnessController(WellnessService wellnessService) {
        this.wellnessService = wellnessService;
    }

    @PostMapping("/logs")
    public ResponseEntity<WellnessLog> saveWellnessLog(
            @RequestBody WellnessLogRequest request
    ) {
        WellnessLog savedLog = wellnessService.saveWellnessLog(request);
        return ResponseEntity.ok(savedLog);
    }
}
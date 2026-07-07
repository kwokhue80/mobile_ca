package sg.edu.nus.features.wellness.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RecommendationResponse {
    private final String recommendation;
    private final OffsetDateTime generatedAt;
}

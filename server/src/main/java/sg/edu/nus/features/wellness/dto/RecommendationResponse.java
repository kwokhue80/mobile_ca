package sg.edu.nus.features.wellness.dto;

import java.time.OffsetDateTime;

public class RecommendationResponse {

    private final String recommendation;
    private final OffsetDateTime generatedAt;

    public RecommendationResponse(String recommendation, OffsetDateTime generatedAt) {
        this.recommendation = recommendation;
        this.generatedAt = generatedAt;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }
}

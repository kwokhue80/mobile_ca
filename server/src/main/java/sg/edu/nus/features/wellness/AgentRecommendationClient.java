package sg.edu.nus.features.wellness;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AgentRecommendationClient {

    private static final int MAX_RECOMMENDATION_LENGTH = 280;
    private static final Pattern LEADING_BULLET_PREFIX = Pattern.compile("^(?:[-*\\u2022]|\\d+[.)])\\s*");

    private final RestClient restClient;

    public AgentRecommendationClient(
        RestClient.Builder restClientBuilder,
        @Value("${wellness.agent.base-url:http://localhost:8001}") String agentBaseUrl,
        @Value("${wellness.agent.connect-timeout-ms:1200}") int connectTimeoutMs,
        @Value("${wellness.agent.read-timeout-ms:2500}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = restClientBuilder
            .baseUrl(agentBaseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public String fetchRecommendation(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("Agent recommendation skipped because Authorization header is missing");
            return null;
        }

        try {
            ChatResponse response = restClient.post()
                .uri("/api/chat") // Send post request to FastAPI endpoint
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(
                    "Provide one concise, actionable wellness recommendation for today based on user data. "
                        + "Return only the recommendation text.",
                    List.of(), List.of()
                ))
                .retrieve()
                .body(ChatResponse.class);

            if (response == null || response.answer() == null) {
                return null;
            }

            String sanitized = sanitizeRecommendation(response.answer());
            if (sanitized == null) {
                return null;
            }

            if (looksStructuredRecommendationPayload(sanitized)) {
                String condensed = condenseStructuredRecommendation(sanitized);
                if (condensed == null || condensed.isBlank()) {
                    log.warn("Agent recommendation structured payload could not be condensed; falling back to deterministic single recommendation");
                    return null;
                }
                sanitized = condensed;
            }

            if (looksGenericBoilerplate(sanitized)) {
                log.warn("Agent recommendation rejected as generic boilerplate; falling back to deterministic recommendation");
                return null;
            }

            return sanitized;
        } catch (Exception ex) {
            log.warn("Agent recommendation call failed; falling back to deterministic recommendation", ex);
            return null;
        }
    }

    private String sanitizeRecommendation(String rawAnswer) {
        if (rawAnswer == null) {
            return null;
        }

        String cleaned = rawAnswer
            .replace("\r", "\n")
            .replaceAll("`", "")
            .replaceAll("\\*\\*", "")
            .replaceAll("\\s+", " ")
            .trim();

        cleaned = LEADING_BULLET_PREFIX.matcher(cleaned).replaceFirst("").trim();

        if (cleaned.isBlank()) {
            return null;
        }

        if (cleaned.length() > MAX_RECOMMENDATION_LENGTH) {
            return cleaned.substring(0, MAX_RECOMMENDATION_LENGTH).trim() + "...";
        }

        return cleaned;
    }

    private boolean looksGenericBoilerplate(String recommendation) {
        if (recommendation == null || recommendation.isBlank()) {
            return true;
        }

        String normalized = recommendation.toLowerCase();

        // Reject known generic templates that do not reflect user-specific data.
        if (normalized.contains("great wellness day")
            && normalized.contains("hydrate")
            && normalized.contains("balanced meal")
            && normalized.contains("good night")) {
            return true;
        }

        if (normalized.contains("general wellness tips")
            && normalized.contains("10-15 minute walk")
            && normalized.contains("protein-rich meal")) {
            return true;
        }

        if (normalized.contains("drink a glass of water")
            && normalized.contains("consistent sleep")) {
            return true;
        }

        return false;
    }

    private boolean looksStructuredRecommendationPayload(String recommendation) {
        String normalized = recommendation.toLowerCase();

        // Reject multi-section report-like responses and keep API output to one recommendation line.
        if (normalized.contains("recommendation summary")
            || normalized.contains("daily summary")
            || normalized.contains("profile basis")
            || normalized.contains("search details")
            || normalized.contains("web recommendations")) {
            return true;
        }

        if (normalized.contains("i fetched your latest recommendation")) {
            return true;
        }

        return false;
    }

    private String condenseStructuredRecommendation(String recommendation) {
        if (recommendation == null || recommendation.isBlank()) {
            return null;
        }

        String normalized = recommendation.replaceAll("\\s+", " ").trim();
        String lowered = normalized.toLowerCase();

        int recommendationsIndex = lowered.indexOf("recommendations:");
        if (recommendationsIndex >= 0) {
            String tail = normalized.substring(recommendationsIndex + "recommendations:".length()).trim();
            String candidate = pickActionableSegment(tail);
            if (candidate != null) {
                return truncateRecommendation(candidate);
            }
        }

        int webRecommendationsIndex = lowered.indexOf("web recommendations");
        if (webRecommendationsIndex >= 0) {
            return "Focus on hydration, a balanced meal, light movement, and consistent sleep today.";
        }

        String fallbackCandidate = pickActionableSegment(normalized);
        return truncateRecommendation(fallbackCandidate != null ? fallbackCandidate : normalized);
    }

    private String pickActionableSegment(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] segments = text.split("\\s+-\\s+|\\s+\\d+[.)]\\s+");
        for (String segment : segments) {
            String candidate = segment.trim();
            if (candidate.isBlank()) {
                continue;
            }

            String lowered = candidate.toLowerCase();
            if (lowered.contains("you still need")
                || lowered.contains("try ")
                || lowered.contains("aim ")
                || lowered.contains("add ")
                || lowered.contains("take ")
                || lowered.contains("keep ")
                || lowered.contains("short by")) {
                return candidate;
            }
        }

        for (String segment : segments) {
            String candidate = segment.trim();
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String truncateRecommendation(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String cleaned = LEADING_BULLET_PREFIX.matcher(text.trim()).replaceFirst("").trim();
        if (cleaned.length() <= MAX_RECOMMENDATION_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_RECOMMENDATION_LENGTH).trim() + "...";
    }

    private record ChatRequest(String query, List<ChatMessage> recentMessages, List<ChatMessage> relevantPastMessages) {}
    private record ChatMessage(String text, boolean isUser) {}
    private record ChatResponse(String answer) {}
}
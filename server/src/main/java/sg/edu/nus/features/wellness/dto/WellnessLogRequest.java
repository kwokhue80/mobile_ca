package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class WellnessLogRequest {

    private String metric_type;
    private String notes;
    private LocalDateTime recorded_at;
    private Map<String, Object> details;

    public String getMetric_type() {
        return metric_type;
    }

    public void setMetric_type(String metric_type) {
        this.metric_type = metric_type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRecorded_at() {
        return recorded_at;
    }

    public void setRecorded_at(LocalDateTime recorded_at) {
        this.recorded_at = recorded_at;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
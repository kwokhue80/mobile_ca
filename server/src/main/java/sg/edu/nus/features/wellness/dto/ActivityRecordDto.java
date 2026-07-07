package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

// Represents one logged activity record in a simplified form.
// Used when sending activity history data back to a caller,
// keeping only the fields that are actually needed.
public class ActivityRecordDto {
    private String activityType;
    private String title;
    private String description;
    private LocalDateTime recordedAt;

    public ActivityRecordDto(String activityType, String title, String description, LocalDateTime recordedAt) {
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.recordedAt = recordedAt;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
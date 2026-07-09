// AUTHOR: Amelia Wong
package sg.edu.nus.features.wellness.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Represents one logged activity record in a simplified form.
// Used when sending activity history data back to a caller,
// keeping only the fields that are actually needed.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecordDto {
    private String activityType;
    private String title;
    private String description;
    private LocalDateTime recordedAt;
}
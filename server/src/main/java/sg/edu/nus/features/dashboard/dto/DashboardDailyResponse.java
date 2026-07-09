// Author: HuaYuan Xie
package sg.edu.nus.features.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sg.edu.nus.features.wellness.model.DailyWellnessSummary;
import sg.edu.nus.features.wellness.model.ActivityRecord;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDailyResponse {

    // The Aggreated Wellness Summary for the Day
    private DailyWellnessSummary dailyWellnessSummary;

    // The List of Activity Records for the Day
    private List<ActivityRecord> activityRecords;

}

// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.history.model

import sg.edu.nus.iss.client.dashboard.model.ActivityRecord

sealed class HistoryListItem {
    data class Header(val label: String) : HistoryListItem()
    data class Record(val record: ActivityRecord) : HistoryListItem()
}

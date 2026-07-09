package sg.edu.nus.iss.client.dashboard.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
/**
* Author(s): Yang Mao Wei
* Contribution:
* - Added Food Intake detail display logic.
* - Used default Food Intake goal when Set Goals does not include Food Intake.
* - Displayed Food Intake day progress and percentage.
*/
class MetricDetailViewModelFactory(
    private val application: Application,
    private val metricType: MetricType
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricDetailViewModel(application, metricType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

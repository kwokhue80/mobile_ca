package sg.edu.nus.iss.client.dashboard.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType

class MetricDetailViewModelFactory(private val metricType: MetricType) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricDetailViewModel(metricType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

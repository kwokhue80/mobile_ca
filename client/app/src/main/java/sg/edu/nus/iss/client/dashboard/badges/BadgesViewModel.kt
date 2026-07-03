package sg.edu.nus.iss.client.dashboard.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import sg.edu.nus.iss.client.dashboard.badges.model.BadgeType

data class BadgeUiItem(
    val type: BadgeType,
    val achieved: Boolean,
    val collected: Boolean
)

/** Activity-scoped so collected state survives navigating from the Badges grid
 *  back to the Dashboard. Exactly one badge meets its condition ([achieved]) by
 *  default so the "Collect" flow can be tested without a backend, but the
 *  Dashboard's badge count only increases once the user explicitly collects
 *  it — being achievable alone isn't enough. */
class BadgesViewModel : ViewModel() {

    private val achieved = MutableStateFlow(setOf(BadgeType.DISTANCE_MASTER))
    private val collected = MutableStateFlow<Set<BadgeType>>(emptySet())

    val badgeItems: StateFlow<List<BadgeUiItem>> = combine(achieved, collected) { achievedSet, collectedSet ->
        BadgeType.entries.map { type ->
            BadgeUiItem(type = type, achieved = achievedSet.contains(type), collected = collectedSet.contains(type))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun collect(type: BadgeType) {
        if (!achieved.value.contains(type)) return
        collected.value = collected.value + type
    }
}

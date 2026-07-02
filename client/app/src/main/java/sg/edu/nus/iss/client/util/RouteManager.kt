package sg.edu.nus.iss.client.util

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.auth.LoginFragment
import sg.edu.nus.iss.client.chat.ChatFragment
import sg.edu.nus.iss.client.dashboard.goals.ActivitiesFragment
import sg.edu.nus.iss.client.dashboard.AddItemBottomSheetFragment
import sg.edu.nus.iss.client.dashboard.AddManuallyBottomSheetFragment
import sg.edu.nus.iss.client.dashboard.DashboardFragment
import sg.edu.nus.iss.client.dashboard.HomeFragment
import sg.edu.nus.iss.client.dashboard.activity.ActivityDurationFragment
import sg.edu.nus.iss.client.dashboard.activity.ChooseExerciseFragment
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.detail.MetricDetailFragment
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.goals.GoalSettingFragment
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.dashboard.history.HistoryFragment

object RouteManager {

    // Variables
    enum class HomeTab { MAIN, CHAT }

    private const val TAG_ADD_MANUALLY = "add_manually"
    private const val TAG_ADD_ITEM = "add_item"

    @IdRes
    private val ROOT_CONTAINER = R.id.fragment_container

    @IdRes
    private val HOME_CONTENT_CONTAINER = R.id.homeContentContainer

    // Routing
    // Initial login route from Activity
    fun startWithLogin(activity: AppCompatActivity) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(ROOT_CONTAINER, LoginFragment())
            .commit()
    }

    // To login fragment
    fun toLogin(host: Fragment) {
        replaceRoot(host, LoginFragment(), addToBackStack = false)
    }

    // To home fragment
    fun toHome(host: Fragment) {
        replaceRoot(host, HomeFragment(), addToBackStack = false)
    }

    // Home navigation to dashboard / chatbot
    fun switchHomeTab(host: Fragment, tab: HomeTab) {
        val target = when (tab) {
            HomeTab.MAIN -> DashboardFragment()
            HomeTab.CHAT -> ChatFragment()
        }
        host.childFragmentManager
            .beginTransaction()
            .replace(HOME_CONTENT_CONTAINER, target)
            .commit()
    }

    // Show add manually
    fun showAddManually(host: Fragment) {
        AddManuallyBottomSheetFragment()
            .show(host.childFragmentManager, TAG_ADD_MANUALLY)
    }

    // Show add item
    fun showAddItem(host: Fragment, itemName: String) {
        AddItemBottomSheetFragment
            .newInstance(itemName)
            .show(host.childFragmentManager, TAG_ADD_ITEM)
    }

    // To choose exercise
    fun toChooseExercise(host: Fragment) {
        replaceRoot(
            host,
            ChooseExerciseFragment(),
            addToBackStack = true,
            backStackName = ChooseExerciseFragment.BACK_STACK_NAME
        )
    }

    // To activity duration
    fun toActivityDuration(host: Fragment, exerciseType: ExerciseType) {
        replaceRoot(
            host,
            ActivityDurationFragment.newInstance(exerciseType),
            addToBackStack = true
        )
    }

    // To goals
    fun toGoals(host: Fragment) {
        replaceRoot(
            host,
            ActivitiesFragment(),
            addToBackStack = true
        )
    }

    // To goal setting
    fun toGoalSetting(host: Fragment, goalType: ActivityGoalType) {
        replaceRoot(
            host,
            GoalSettingFragment.newInstance(goalType),
            addToBackStack = true
        )
    }

    // To history
    fun toHistory(host: Fragment) {
        replaceRoot(
            host,
            HistoryFragment(),
            addToBackStack = true
        )
    }

    // To metric detail
    fun toMetricDetail(host: Fragment, metricType: MetricType) {
        replaceRoot(
            host,
            MetricDetailFragment.newInstance(metricType),
            addToBackStack = true
        )
    }

    // Back to last fragment
    fun back(host: Fragment) {
        host.requireActivity().supportFragmentManager.popBackStack()
    }

    // Back to specific fragment
    fun backTo(host: Fragment, backStackName: String, inclusive: Boolean) {
        host.requireActivity()
            .supportFragmentManager
            .popBackStack(
                backStackName,
                if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
            )
    }

    // Helpers
    // Start transaction to replace root fragment
    private fun replaceRoot(
        host: Fragment,
        target: Fragment,
        addToBackStack: Boolean,
        backStackName: String? = null
    ) {
        val tx = host.requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(ROOT_CONTAINER, target)

        if (addToBackStack) tx.addToBackStack(backStackName)

        tx.commit()
    }

}
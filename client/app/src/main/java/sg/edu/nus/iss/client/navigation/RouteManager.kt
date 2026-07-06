package sg.edu.nus.iss.client.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.chatbot.ChatFragment
import sg.edu.nus.iss.client.dashboard.DashboardFragment
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

object RouteManager {

    // Variables
    enum class HomeTab { MAIN, CHAT }

    // Routing - start destination: Login

    // To login fragment (on logout)
    fun toLogin(host: Fragment) {
        val navController = host.findNavController()
        navController.popBackStack(R.id.homeFragment, true)
        navController.navigate(R.id.loginFragment)
    }

    // To home fragment
    fun toHome(host: Fragment) {
        host.findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    // Home navigation to dashboard / chatbot
    // Note: not using Nav Component; keeping it simple for tab switching
    fun switchHomeTab(host: Fragment, tab: HomeTab) {
        // Implementation remains same as it manages a private sub-container
        val target = when (tab) {
            HomeTab.MAIN -> DashboardFragment()
            HomeTab.CHAT -> ChatFragment()
        }
        host.childFragmentManager
            .beginTransaction()
            .replace(R.id.homeContentContainer, target)
            .commit()
    }

    // Show add manually
    fun showAddManually(host: Fragment) {
        host.findNavController().navigate(R.id.addManuallyBottomSheetFragment)
    }

    // Show add item
    fun showAddItem(host: Fragment, itemName: String) {
        val bundle = Bundle().apply {
            putString("item_name", itemName)
        }
        host.findNavController().navigate(R.id.addItemBottomSheetFragment, bundle)
    }

    // To choose exercise
    fun toChooseExercise(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_chooseExerciseFragment)
    }

    // To activity duration
    fun toActivityDuration(host: Fragment, exerciseType: ExerciseType) {
        val bundle = Bundle().apply {
            putString("arg_exercise_type", exerciseType.name)
        }
        host.findNavController().navigate(R.id.action_chooseExerciseFragment_to_activityDurationFragment, bundle)
    }

    // To goals
    fun toGoals(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_activitiesFragment)
    }

    // To goal setting
    fun toGoalSetting(host: Fragment, goalType: ActivityGoalType) {
        val bundle = Bundle().apply {
            putString("arg_activity_goal_type", goalType.name)
        }
        host.findNavController().navigate(R.id.action_activitiesFragment_to_goalSettingFragment, bundle)
    }

    // To history
    fun toHistory(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_historyFragment)
    }

    // To metric detail
    fun toMetricDetail(host: Fragment, metricType: MetricType) {
        val bundle = Bundle().apply {
            putString("arg_metric_type", metricType.name)
        }
        host.findNavController().navigate(R.id.action_homeFragment_to_metricDetailFragment, bundle)
    }

    // To exercise days detail
    fun toExerciseDaysDetail(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_exerciseDaysDetailFragment)
    }

    // To mental health detail
    fun toMentalHealthDetail(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_mentalHealthDetailFragment)
    }

    // To badges
    fun toBadges(host: Fragment) {
        host.findNavController().navigate(R.id.action_homeFragment_to_badgesFragment)
    }

    // To activity detail
    fun toActivityDetail(host: Fragment, recordId: String) {
        val bundle = Bundle().apply {
            putString("arg_record_id", recordId)
        }
        host.findNavController().navigate(R.id.action_global_activityDetailFragment, bundle)
    }

    // Back to last fragment
    fun back(host: Fragment) {
        host.findNavController().popBackStack()
    }

    // Back to specific fragment
    fun backTo(host: Fragment, destinationId: Int, inclusive: Boolean) {
        host.findNavController().popBackStack(destinationId, inclusive)
    }
}
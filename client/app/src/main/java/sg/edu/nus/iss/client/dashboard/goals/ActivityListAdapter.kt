// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemActivityGoalBinding
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

class ActivityListAdapter(
    private val activityTypes: List<ActivityGoalType>,
    private val onActivityClick: (ActivityGoalType) -> Unit
) : RecyclerView.Adapter<ActivityListAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activityTypes[position])
    }

    override fun getItemCount(): Int = activityTypes.size

    inner class ActivityViewHolder(private val binding: ItemActivityGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(activityGoalType: ActivityGoalType) {
            binding.iconActivityGoal.setImageResource(activityGoalType.iconRes)
            binding.tvActivityGoalName.text = activityGoalType.displayName
            binding.root.setOnClickListener { onActivityClick(activityGoalType) }
        }
    }
}

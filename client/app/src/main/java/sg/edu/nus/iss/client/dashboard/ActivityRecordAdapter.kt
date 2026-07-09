// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.ItemActivityRecordBinding
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.dashboard.util.ActivityDateFormatter

class ActivityRecordAdapter(
    private val onDeleteClick: (ActivityRecord) -> Unit,
    private val onItemClick: (ActivityRecord) -> Unit = {}
) : ListAdapter<ActivityRecord, ActivityRecordAdapter.ActivityViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(private val binding: ItemActivityRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: ActivityRecord) {
            binding.iconActivityType.setImageResource(ExerciseType.iconResFor(record.type))
            
            binding.tvActivityType.text = record.type
            binding.tvActivityMeta.text =
                "${ActivityDateFormatter.formatCompact(record.timestamp)} · ${record.durationMinutes} min"
            binding.btnDeleteActivity.setOnClickListener { onDeleteClick(record) }
            binding.root.setOnClickListener { onItemClick(record) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ActivityRecord>() {
        override fun areItemsTheSame(oldItem: ActivityRecord, newItem: ActivityRecord) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityRecord, newItem: ActivityRecord) = oldItem == newItem
    }
}

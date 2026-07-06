package sg.edu.nus.iss.client.dashboard.history

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemHistoryHeaderBinding
import sg.edu.nus.iss.client.databinding.ItemHistoryRecordBinding
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.history.model.HistoryListItem
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.dashboard.util.ActivityDateFormatter

class HistoryListAdapter(
    private val onItemClick: (ActivityRecord) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_RECORD = 1
        private val DEFAULT_ACCENT_COLOR = Color.parseColor("#1F1F1F")
        private val DEFAULT_ACCENT_BACKGROUND = Color.parseColor("#EEF1F5")
    }

    private var items: List<HistoryListItem> = emptyList()

    fun submitList(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryListItem.Header -> VIEW_TYPE_HEADER
        is HistoryListItem.Record -> VIEW_TYPE_RECORD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemHistoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            RecordViewHolder(ItemHistoryRecordBinding.inflate(inflater, parent, false), onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is HistoryListItem.Record -> (holder as RecordViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(private val binding: ItemHistoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: HistoryListItem.Header) {
            binding.root.text = header.label
        }
    }

    class RecordViewHolder(
        private val binding: ItemHistoryRecordBinding,
        private val onItemClick: (ActivityRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.Record) {
            val record = item.record
            val exerciseType = ExerciseType.fromDisplayName(record.type)

            binding.iconActivityType.setImageResource(exerciseType?.iconRes ?: ExerciseType.iconResFor(record.type))
            binding.iconActivityType.setColorFilter(
                exerciseType?.accentColor ?: DEFAULT_ACCENT_COLOR,
                PorterDuff.Mode.SRC_IN
            )
            (binding.iconContainer.background.mutate() as GradientDrawable).setColor(
                exerciseType?.accentBackground ?: DEFAULT_ACCENT_BACKGROUND
            )

            binding.tvActivityType.text = record.type
            binding.tvActivityMeta.text =
                "${ActivityDateFormatter.formatTimeOnly(record.timestamp)} · ${record.durationMinutes} min"
            binding.root.setOnClickListener { onItemClick(record) }
        }
    }
}

package sg.edu.nus.iss.client.dashboard.history

import android.graphics.Color
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
    }

    private var items: List<HistoryListItem> = emptyList()
    private var highlightedIds: Set<String> = emptySet()

    fun submitList(newItems: List<HistoryListItem>, highlightedIds: Set<String> = emptySet()) {
        items = newItems
        this.highlightedIds = highlightedIds
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
            is HistoryListItem.Record ->
                (holder as RecordViewHolder).bind(item, highlightedIds.contains(item.record.id))
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

        companion object {
            // Light green highlight for the day jumped to via History's calendar picker.
            private val HIGHLIGHT_COLOR = Color.parseColor("#DCEDC8")
        }

        fun bind(item: HistoryListItem.Record, isHighlighted: Boolean) {
            val record = item.record

            // Matches ActivityRecordAdapter's bind() (Home's "Activity Tracked" list):
            // no color filter, no background tint override - just the drawable's own
            // baked-in tint, so both lists render identically.
            binding.iconActivityType.setImageResource(ExerciseType.iconResFor(record.type))

            binding.tvActivityType.text = record.type
            binding.tvActivityMeta.text =
                "${ActivityDateFormatter.formatCompact(record.timestamp)} · ${record.durationMinutes} min"
            binding.root.setBackgroundColor(if (isHighlighted) HIGHLIGHT_COLOR else Color.TRANSPARENT)
            binding.root.setOnClickListener { onItemClick(record) }
        }
    }
}

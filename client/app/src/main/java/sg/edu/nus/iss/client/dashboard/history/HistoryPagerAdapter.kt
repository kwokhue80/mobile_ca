package sg.edu.nus.iss.client.dashboard.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemActivityRecordPageBinding
import sg.edu.nus.iss.client.dashboard.ActivityPageFooterAdapter
import sg.edu.nus.iss.client.dashboard.history.model.HistoryListItem
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.dashboard.util.ActivityDateFormatter

class HistoryPagerAdapter(
    private val onPageNumberClick: (Int) -> Unit
) : RecyclerView.Adapter<HistoryPagerAdapter.PageViewHolder>() {

    companion object {
        const val PAGE_SIZE = 20
    }

    private var records: List<ActivityRecord> = emptyList()
    private var currentPage: Int = 0

    fun submitList(newRecords: List<ActivityRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    fun setCurrentPage(page: Int) {
        if (currentPage == page) return
        currentPage = page
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        if (records.isEmpty()) 1 else (records.size + PAGE_SIZE - 1) / PAGE_SIZE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemActivityRecordPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding, onPageNumberClick)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val start = position * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, records.size)
        val pageRecords = if (start < end) records.subList(start, end) else emptyList()
        holder.bind(buildPageItems(pageRecords), itemCount, currentPage)
    }

    private fun buildPageItems(pageRecords: List<ActivityRecord>): List<HistoryListItem> {
        val items = mutableListOf<HistoryListItem>()
        var lastGroup: String? = null
        for (record in pageRecords) {
            val group = ActivityDateFormatter.groupLabel(record.timestamp)
            if (group != lastGroup) {
                items.add(HistoryListItem.Header(group))
                lastGroup = group
            }
            items.add(HistoryListItem.Record(record))
        }
        return items
    }

    class PageViewHolder(
        private val binding: ItemActivityRecordPageBinding,
        onPageNumberClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val listAdapter = HistoryListAdapter()
        private val footerAdapter = ActivityPageFooterAdapter(onPageNumberClick)

        init {
            binding.rvPageRecords.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvPageRecords.adapter = ConcatAdapter(listAdapter, footerAdapter)
        }

        fun bind(items: List<HistoryListItem>, totalPages: Int, currentPage: Int) {
            listAdapter.submitList(items)
            footerAdapter.update(totalPages, currentPage)
        }
    }
}

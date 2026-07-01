package sg.edu.nus.iss.client.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemActivityRecordPageBinding
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord

class ActivityRecordPagerAdapter(
    private val onDeleteClick: (ActivityRecord) -> Unit,
    private val onPageNumberClick: (Int) -> Unit
) : RecyclerView.Adapter<ActivityRecordPagerAdapter.PageViewHolder>() {

    companion object {
        const val PAGE_SIZE = 8
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
        return PageViewHolder(binding, onDeleteClick, onPageNumberClick)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val start = position * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, records.size)
        val pageItems = if (start < end) records.subList(start, end) else emptyList()
        holder.bind(pageItems, itemCount, currentPage)
    }

    class PageViewHolder(
        private val binding: ItemActivityRecordPageBinding,
        onDeleteClick: (ActivityRecord) -> Unit,
        onPageNumberClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val recordsAdapter = ActivityRecordAdapter(onDeleteClick)
        private val footerAdapter = ActivityPageFooterAdapter(onPageNumberClick)

        init {
            binding.rvPageRecords.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvPageRecords.adapter = ConcatAdapter(recordsAdapter, footerAdapter)
        }

        fun bind(pageItems: List<ActivityRecord>, totalPages: Int, currentPage: Int) {
            recordsAdapter.submitList(pageItems)
            footerAdapter.update(totalPages, currentPage)
        }
    }
}

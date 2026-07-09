// Author: HuaYuan Xie
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
    private val onPageNumberClick: (Int) -> Unit,
    private val onItemClick: (ActivityRecord) -> Unit = {}
) : RecyclerView.Adapter<ActivityRecordPagerAdapter.PageViewHolder>() {

    companion object {
        const val PAGE_SIZE = 4
    }

    private var records: List<ActivityRecord> = emptyList()
    private var currentPage: Int = 0

    fun submitList(newRecords: List<ActivityRecord>) {
        val oldItemCount = itemCount
        records = newRecords
        val newItemCount = itemCount

        if (oldItemCount != newItemCount) {
            notifyDataSetChanged()
        } else {
            // ViewPager2 doesn't rebind the currently-displayed page on notifyDataSetChanged()
            // when itemCount is unchanged (a known RecyclerView/ViewPager2 limitation), so the
            // "Activity Tracked" list could go stale after an add/delete without a real page
            // navigation. notifyItemRangeChanged() forces the visible page to rebind.
            notifyItemRangeChanged(0, newItemCount)
        }
    }

    fun setCurrentPage(page: Int) {
        if (currentPage == page) return
        currentPage = page
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemCount(): Int =
        if (records.isEmpty()) 1 else (records.size + PAGE_SIZE - 1) / PAGE_SIZE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemActivityRecordPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding, onDeleteClick, onPageNumberClick, onItemClick)
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
        onPageNumberClick: (Int) -> Unit,
        onItemClick: (ActivityRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val recordsAdapter = ActivityRecordAdapter(onDeleteClick, onItemClick)
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

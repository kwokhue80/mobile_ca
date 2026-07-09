// Author: HuaYuan Xie
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
import java.time.LocalDate

class HistoryPagerAdapter(
    private val onPageNumberClick: (Int) -> Unit,
    private val onItemClick: (ActivityRecord) -> Unit = {}
) : RecyclerView.Adapter<HistoryPagerAdapter.PageViewHolder>() {

    companion object {
        const val PAGE_SIZE = 20
    }

    private var records: List<ActivityRecord> = emptyList()
    private var currentPage: Int = 0

    // Every record on the same day as the jump target, so the whole day is highlighted
    // (not just whichever single record happened to resolve the date search) - set by
    // [jumpToDate] and consumed by PageViewHolder to tint rows and auto-scroll to them.
    private var highlightedIds: Set<String> = emptySet()

    // Bumped on every jumpToDate() call so a PageViewHolder that's already showing the
    // target page (no ViewPager2 page-change, so no fresh bind would otherwise happen)
    // still knows to (re)scroll - e.g. picking the same date twice, or a date on the
    // page that's already current.
    private var jumpSeq: Int = 0

    fun submitList(newRecords: List<ActivityRecord>) {
        records = newRecords
        // ViewPager2 doesn't rebind the currently-displayed page on notifyDataSetChanged()
        // when itemCount is unchanged (a known RecyclerView/ViewPager2 limitation) - see
        // ActivityRecordPagerAdapter's identical fix - so use a targeted notify instead.
        notifyItemRangeChanged(0, itemCount)
    }

    fun setCurrentPage(page: Int) {
        if (currentPage == page) return
        currentPage = page
        notifyItemRangeChanged(0, itemCount)
    }

    // `records` is sorted newest-first (backend returns loggedAt DESC), so the first
    // record on or before the picked date is where that day's activities begin.
    // Falls back to the oldest page if every record postdates the picked date. Marks
    // every record sharing that same day as highlighted, so a date with no activity of
    // its own (e.g. picking 6/10 when only 6/9 has one) surfaces and highlights 6/9's
    // activities, and picking a date that does have activities highlights those.
    fun jumpToDate(date: LocalDate): Int {
        if (records.isEmpty()) return 0
        val index = records.indexOfFirst { !it.timestamp.toLocalDate().isAfter(date) }
        val targetIndex = if (index >= 0) index else records.size - 1
        val targetDate = records[targetIndex].timestamp.toLocalDate()
        highlightedIds = records.filter { it.timestamp.toLocalDate() == targetDate }.map { it.id }.toSet()
        jumpSeq++
        notifyItemRangeChanged(0, itemCount)
        return targetIndex / PAGE_SIZE
    }

    override fun getItemCount(): Int =
        if (records.isEmpty()) 1 else (records.size + PAGE_SIZE - 1) / PAGE_SIZE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemActivityRecordPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding, onPageNumberClick, onItemClick)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val start = position * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, records.size)
        val pageRecords = if (start < end) records.subList(start, end) else emptyList()
        holder.bind(buildPageItems(pageRecords), itemCount, currentPage, highlightedIds, jumpSeq)
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
        onPageNumberClick: (Int) -> Unit,
        onItemClick: (ActivityRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val listAdapter = HistoryListAdapter(onItemClick)
        private val footerAdapter = ActivityPageFooterAdapter(onPageNumberClick)
        private val layoutManager = LinearLayoutManager(binding.root.context)

        // Only auto-scroll once per jump (tracked by jumpSeq), so re-binding this same
        // page for an unrelated reason (e.g. a data refresh) doesn't yank the user's
        // scroll position back to the highlighted row every time.
        private var scrolledForSeq: Int = -1

        init {
            binding.rvPageRecords.layoutManager = layoutManager
            binding.rvPageRecords.adapter = ConcatAdapter(listAdapter, footerAdapter)
        }

        fun bind(
            items: List<HistoryListItem>,
            totalPages: Int,
            currentPage: Int,
            highlightedIds: Set<String>,
            jumpSeq: Int
        ) {
            listAdapter.submitList(items, highlightedIds)
            footerAdapter.update(totalPages, currentPage)

            if (jumpSeq != scrolledForSeq) {
                val targetPosition = items.indexOfFirst {
                    it is HistoryListItem.Record && highlightedIds.contains(it.record.id)
                }
                if (targetPosition >= 0) {
                    scrolledForSeq = jumpSeq
                    // Post so this runs after the RecyclerView has laid out the freshly
                    // submitted items - scrolling immediately would act on stale/empty
                    // layout state. A small top offset keeps the row clear of the very
                    // edge instead of flush against it.
                    val topOffsetPx = (16 * binding.root.resources.displayMetrics.density).toInt()
                    binding.rvPageRecords.post {
                        layoutManager.scrollToPositionWithOffset(targetPosition, topOffsetPx)
                    }
                }
            }
        }
    }
}

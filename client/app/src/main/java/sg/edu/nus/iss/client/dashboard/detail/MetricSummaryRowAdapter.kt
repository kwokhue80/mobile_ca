// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemMetricSummaryRowBinding
import sg.edu.nus.iss.client.dashboard.detail.model.MetricSummaryRow
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType

class MetricSummaryRowAdapter(private val metricType: MetricType) :
    RecyclerView.Adapter<MetricSummaryRowAdapter.SummaryRowViewHolder>() {

    private var rows: List<MetricSummaryRow> = emptyList()

    fun submitList(newRows: List<MetricSummaryRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryRowViewHolder {
        val binding = ItemMetricSummaryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SummaryRowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryRowViewHolder, position: Int) {
        holder.bind(rows[position], metricType)
    }

    override fun getItemCount(): Int = rows.size

    class SummaryRowViewHolder(private val binding: ItemMetricSummaryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: MetricSummaryRow, metricType: MetricType) {
            binding.tvSummaryLabel.text = row.label
            binding.tvSummaryValue.text = "${metricType.formatValue(row.value)} ${metricType.unit}"
            val labelStyle = if (row.isCurrentPeriod) Typeface.BOLD else Typeface.NORMAL
            binding.tvSummaryLabel.setTypeface(null, labelStyle)
        }
    }
}

// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemExerciseWeekSummaryRowBinding

class ExerciseWeekSummaryAdapter(
    private val onRowClick: (ExerciseWeekSummaryRow) -> Unit
) : RecyclerView.Adapter<ExerciseWeekSummaryAdapter.RowViewHolder>() {

    private var rows: List<ExerciseWeekSummaryRow> = emptyList()

    fun submitList(newRows: List<ExerciseWeekSummaryRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemExerciseWeekSummaryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(rows[position], onRowClick)
    }

    override fun getItemCount(): Int = rows.size

    class RowViewHolder(private val binding: ItemExerciseWeekSummaryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ExerciseWeekSummaryRow, onRowClick: (ExerciseWeekSummaryRow) -> Unit) {
            binding.tvWeekLabel.text = row.label
            binding.tvWeekValue.text = "${row.daysExercised} of ${row.goalDays} days"
            binding.tvWeekLabel.setTypeface(null, if (row.isCurrentWeek) Typeface.BOLD else Typeface.NORMAL)
            binding.root.setOnClickListener { onRowClick(row) }
        }
    }
}

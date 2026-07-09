// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemCalendarDayBinding

class CalendarDayAdapter : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(private val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: CalendarDay) {
            binding.tvDayNumber.text = day.date?.dayOfMonth?.toString().orEmpty()
            binding.circleExercised.visibility = if (day.hasExercise) View.VISIBLE else View.GONE
            binding.circleToday.visibility = if (day.isToday) View.VISIBLE else View.GONE
        }
    }
}

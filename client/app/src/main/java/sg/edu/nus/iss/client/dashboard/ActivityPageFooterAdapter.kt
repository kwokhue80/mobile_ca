package sg.edu.nus.iss.client.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemActivityPageFooterBinding

class ActivityPageFooterAdapter(
    private val onPageNumberClick: (Int) -> Unit
) : RecyclerView.Adapter<ActivityPageFooterAdapter.FooterViewHolder>() {

    companion object {
        private const val ACTIVE_COLOR = "#0B57D0"
        private const val INACTIVE_COLOR = "#7A7A7A"
    }

    private var totalPages: Int = 1
    private var currentPage: Int = 0

    fun update(totalPages: Int, currentPage: Int) {
        this.totalPages = totalPages
        this.currentPage = currentPage
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = if (totalPages > 1) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = ItemActivityPageFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.bind(totalPages, currentPage)
    }

    inner class FooterViewHolder(private val binding: ItemActivityPageFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(totalPages: Int, currentPage: Int) {
            binding.pageNumberContainer.removeAllViews()
            val context = binding.root.context
            val density = context.resources.displayMetrics.density
            for (i in 0 until totalPages) {
                val label = TextView(context).apply {
                    text = (i + 1).toString()
                    textSize = 14f
                    setTextColor(Color.parseColor(if (i == currentPage) ACTIVE_COLOR else INACTIVE_COLOR))
                    setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
                    setOnClickListener { onPageNumberClick(i) }
                }
                binding.pageNumberContainer.addView(label)
            }
        }
    }
}

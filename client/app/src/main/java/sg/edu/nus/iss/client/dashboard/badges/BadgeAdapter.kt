package sg.edu.nus.iss.client.dashboard.badges

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemBadgeBinding

class BadgeAdapter(private val onCollectClick: (BadgeUiItem) -> Unit) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private var items: List<BadgeUiItem> = emptyList()

    fun submitList(newItems: List<BadgeUiItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding, onCollectClick)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BadgeViewHolder(
        private val binding: ItemBadgeBinding,
        private val onCollectClick: (BadgeUiItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            private const val DIM_BUTTON_COLOR = "#D9D9D9"
            private const val ACTIVE_BUTTON_COLOR = "#0B57D0"
        }

        fun bind(item: BadgeUiItem) {
            binding.tvBadgeTitle.text = item.type.title
            binding.tvBadgeDescription.text = item.type.description
            binding.ivBadgeIcon.setImageResource(item.type.iconRes)
            binding.ivBadgeIcon.colorFilter = null
            binding.ivBadgeIcon.alpha = 1f

            binding.btnShowBadge.isEnabled = item.achieved && !item.collected
            binding.btnShowBadge.text = if (item.collected) "✓" else "Collect"
            binding.btnShowBadge.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(if (item.achieved) ACTIVE_BUTTON_COLOR else DIM_BUTTON_COLOR)
            )
            binding.btnShowBadge.setOnClickListener { onCollectClick(item) }
        }
    }
}

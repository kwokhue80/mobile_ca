package sg.edu.nus.iss.client.dashboard.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemFoodLogBinding

/** Renders one logged meal (meal type, food name, calories) in the Food Summary
 *  screen - used for both the Day view's meal list and the meals shown under the
 *  Week/Month chart when a bar is tapped. */
class FoodLogAdapter : ListAdapter<FoodEntry, FoodLogAdapter.FoodViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FoodViewHolder(private val binding: ItemFoodLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: FoodEntry) {
            binding.tvMealType.text = entry.mealType
            binding.tvFoodName.text = entry.foodName
            binding.tvMealCalories.text = "${entry.caloriesKcal} kcal"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FoodEntry>() {
        // Entries have no client-side id; loggedAt + name uniquely identify a meal row.
        override fun areItemsTheSame(oldItem: FoodEntry, newItem: FoodEntry) =
            oldItem.loggedAt == newItem.loggedAt && oldItem.foodName == newItem.foodName

        override fun areContentsTheSame(oldItem: FoodEntry, newItem: FoodEntry) = oldItem == newItem
    }
}

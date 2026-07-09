// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.ItemActivityGoalBinding
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType

class ExerciseTypeAdapter(
    private val exerciseTypes: List<ExerciseType>,
    private val onExerciseClick: (ExerciseType) -> Unit
) : RecyclerView.Adapter<ExerciseTypeAdapter.ExerciseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemActivityGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(exerciseTypes[position])
    }

    override fun getItemCount(): Int = exerciseTypes.size

    inner class ExerciseViewHolder(private val binding: ItemActivityGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(exerciseType: ExerciseType) {
            binding.iconActivityGoal.setImageResource(exerciseType.iconRes)
            binding.tvActivityGoalName.text = exerciseType.displayName
            binding.root.setOnClickListener { onExerciseClick(exerciseType) }
        }
    }
}

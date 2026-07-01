package sg.edu.nus.iss.client.dashboard

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import sg.edu.nus.iss.client.databinding.BottomSheetAddItemBinding

class AddItemBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ITEM_NAME = "item_name"

        fun newInstance(itemName: String): AddItemBottomSheetFragment {
            return AddItemBottomSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_ITEM_NAME, itemName) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.isFitToContents = false
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemName = arguments?.getString(ARG_ITEM_NAME).orEmpty()
        binding.tvAddItemTitle.text = "Add $itemName"

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        setupFormByItemName(itemName)

        val basePaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + navBarInset)
            insets
        }
    }

    private fun setupFormByItemName(itemName: String) {
        binding.contentContainer.removeAllViews()

        when (itemName) {
            "Sleep" -> setupSleepForm()
            "Calories" -> setupFoodForm()
            "Distance" -> setupDistanceForm()
            "Hydration" -> setupHydrationForm()
            "Weight" -> setupWeightForm()
            "Steps" -> setupStepsForm()
            "Exercise Days" -> setupExerciseDaysForm()
            "Mental Health" -> setupMentalHealthForm()
            "Badges" -> setupBadgesForm()
            else -> setupGenericMetricForm(itemName)
        }
    }

    /**
     * Food Details form.
     *
     * Database direction:
     * - meal_type
     * - food_name
     * - calories
     *
     * Not included here:
     * - protein_g
     * - carbs_g
     * - fat_g
     *
     * Reason: those fields were marked as not required in the UI/database discussion.
     */
    private fun setupFoodForm() {
        addSectionDescription("Enter food name and meal type. Calories can be auto-filled later from the local JSON food library.")

        val mealTypeInput = addInput(
            label = "Meal Type",
            hint = "Breakfast / Lunch / Dinner / Snack",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val foodNameInput = addInput(
            label = "Food Name",
            hint = "e.g. chicken rice",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val caloriesInput = addInput(
            label = "Calories (kcal)",
            hint = "Auto-filled from JSON or enter manually",
            inputType = decimalInputType()
        )

        val lookupButton = addButton("Lookup Calories")
        lookupButton.setOnClickListener {
            val foodName = foodNameInput.text.toString().trim()

            if (foodName.isEmpty()) {
                foodNameInput.error = "Food name is required"
                return@setOnClickListener
            }

            val calories = lookupCaloriesFromLocalFoodLibrary(foodName)

            if (calories != null) {
                caloriesInput.setText(calories.toString())
                Toast.makeText(requireContext(), "Calories found: $calories kcal", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Food not found in local JSON yet. Please enter calories manually.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val saveButton = addButton("Save Food Details")
        saveButton.setOnClickListener {
            val mealType = mealTypeInput.text.toString().trim()
            val foodName = foodNameInput.text.toString().trim()
            val calories = caloriesInput.text.toString().trim()

            if (mealType.isEmpty()) {
                mealTypeInput.error = "Meal type is required"
                return@setOnClickListener
            }

            if (foodName.isEmpty()) {
                foodNameInput.error = "Food name is required"
                return@setOnClickListener
            }

            if (calories.isEmpty()) {
                caloriesInput.error = "Calories is required"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("meal_type", mealType)
                put("food_name", foodName)
                put("calories", calories.toDouble())
            }

            savePayloadLocallyForNow("Food Details", payload)
        }
    }

    /**
     * Sleep Details form.
     *
     * CA-friendly version:
     * - sleep duration in hours
     * - quality score 1 to 5
     *
     * Backend can later convert duration into sleep_start_datetime and sleep_end_datetime
     * if the database requires exact datetime fields.
     */
    private fun setupSleepForm() {
        addSectionDescription("Record sleep duration and sleep quality score.")

        val durationInput = addInput(
            label = "Sleep Duration (hours)",
            hint = "e.g. 7.5",
            inputType = decimalInputType()
        )

        val qualityInput = addInput(
            label = "Sleep Quality Score",
            hint = "1 = poor, 5 = excellent",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Sleep Details")
        saveButton.setOnClickListener {
            val durationText = durationInput.text.toString().trim()
            val qualityText = qualityInput.text.toString().trim()

            if (durationText.isEmpty()) {
                durationInput.error = "Sleep duration is required"
                return@setOnClickListener
            }

            if (qualityText.isEmpty()) {
                qualityInput.error = "Quality score is required"
                return@setOnClickListener
            }

            val qualityScore = qualityText.toIntOrNull()
            if (qualityScore == null || qualityScore !in 1..5) {
                qualityInput.error = "Please enter a score from 1 to 5"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("sleep_duration_hours", durationText.toDouble())
                put("quality_score", qualityScore)
            }

            savePayloadLocallyForNow("Sleep Details", payload)
        }
    }

    /**
     * Exercise Details form for Distance.
     *
     * Database direction:
     * - activity_type
     * - distance_km
     * - calories_burned
     */
    private fun setupDistanceForm() {
        addSectionDescription("Record exercise activity, distance, and optional calories burned.")

        val activityTypeInput = addInput(
            label = "Activity Type",
            hint = "Walking / Running / Cycling",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val distanceInput = addInput(
            label = "Distance (km)",
            hint = "e.g. 3.2",
            inputType = decimalInputType()
        )

        val caloriesBurnedInput = addInput(
            label = "Calories Burned (optional)",
            hint = "e.g. 180",
            inputType = decimalInputType()
        )

        val saveButton = addButton("Save Exercise Details")
        saveButton.setOnClickListener {
            val activityType = activityTypeInput.text.toString().trim()
            val distance = distanceInput.text.toString().trim()
            val caloriesBurned = caloriesBurnedInput.text.toString().trim()

            if (activityType.isEmpty()) {
                activityTypeInput.error = "Activity type is required"
                return@setOnClickListener
            }

            if (distance.isEmpty()) {
                distanceInput.error = "Distance is required"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("activity_type", activityType)
                put("distance_km", distance.toDouble())

                if (caloriesBurned.isNotEmpty()) {
                    put("calories_burned", caloriesBurned.toDouble())
                }
            }

            savePayloadLocallyForNow("Exercise Details", payload)
        }
    }

    private fun setupHydrationForm() {
        addSectionDescription("Record water intake.")

        val amountInput = addInput(
            label = "Amount",
            hint = "e.g. 500",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "ml / glass / bottle",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Hydration")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "hydration",
                valueInput = amountInput,
                unitInput = unitInput,
                displayName = "Hydration"
            )
        }
    }

    private fun setupWeightForm() {
        addSectionDescription("Record current body weight.")

        val weightInput = addInput(
            label = "Weight",
            hint = "e.g. 82.5",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "kg",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("kg")

        val saveButton = addButton("Save Weight")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "weight",
                valueInput = weightInput,
                unitInput = unitInput,
                displayName = "Weight"
            )
        }
    }

    private fun setupStepsForm() {
        addSectionDescription("Record daily step count.")

        val stepsInput = addInput(
            label = "Steps",
            hint = "e.g. 8000",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "steps",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("steps")

        val saveButton = addButton("Save Steps")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "steps",
                valueInput = stepsInput,
                unitInput = unitInput,
                displayName = "Steps"
            )
        }
    }

    private fun setupExerciseDaysForm() {
        addSectionDescription("Record how many days you exercised in the selected period.")

        val daysInput = addInput(
            label = "Exercise Days",
            hint = "e.g. 3",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "days",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("days")

        val saveButton = addButton("Save Exercise Days")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "exercise_days",
                valueInput = daysInput,
                unitInput = unitInput,
                displayName = "Exercise Days"
            )
        }
    }

    private fun setupMentalHealthForm() {
        addSectionDescription("Record mental wellbeing score and optional notes.")

        val scoreInput = addInput(
            label = "Mental Health Score",
            hint = "1 = low, 10 = very good",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "How do you feel today?",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Mental Health")
        saveButton.setOnClickListener {
            val scoreText = scoreInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (scoreText.isEmpty()) {
                scoreInput.error = "Score is required"
                return@setOnClickListener
            }

            val score = scoreText.toIntOrNull()
            if (score == null || score !in 1..10) {
                scoreInput.error = "Please enter a score from 1 to 10"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("metric_type", "mental_health")
                put("metric_value", score)
                put("unit", "score")
                put("notes", notes)
            }

            savePayloadLocallyForNow("Mental Health", payload)
        }
    }

    /**
     * Badges are usually generated by the app, not manually entered by users.
     * This temporary form is kept only because the current Add Manually screen has a Badges button.
     */
    private fun setupBadgesForm() {
        addSectionDescription("Badges are usually generated automatically. This temporary form can be adjusted later.")

        val badgeNameInput = addInput(
            label = "Badge Name",
            hint = "e.g. 7-day streak",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Reason or description",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Badge")
        saveButton.setOnClickListener {
            val badgeName = badgeNameInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (badgeName.isEmpty()) {
                badgeNameInput.error = "Badge name is required"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("metric_type", "badge")
                put("metric_value", badgeName)
                put("unit", "text")
                put("notes", notes)
            }

            savePayloadLocallyForNow("Badge", payload)
        }
    }

    private fun setupGenericMetricForm(itemName: String) {
        addSectionDescription("Record a wellness metric.")

        val valueInput = addInput(
            label = "$itemName Value",
            hint = "Enter value",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "Enter unit",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save $itemName")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = itemName.lowercase().replace(" ", "_"),
                valueInput = valueInput,
                unitInput = unitInput,
                displayName = itemName
            )
        }
    }

    private fun saveGenericMetric(
        metricType: String,
        valueInput: EditText,
        unitInput: EditText,
        displayName: String
    ) {
        val value = valueInput.text.toString().trim()
        val unit = unitInput.text.toString().trim()

        if (value.isEmpty()) {
            valueInput.error = "Value is required"
            return
        }

        if (unit.isEmpty()) {
            unitInput.error = "Unit is required"
            return
        }

        val payload = JSONObject().apply {
            put("metric_type", metricType)
            put("metric_value", value.toDoubleOrNull() ?: value)
            put("unit", unit)
        }

        savePayloadLocallyForNow(displayName, payload)
    }

    /**
     * Temporary local lookup interface.
     *
     * Important:
     * Do not add invented calorie values here.
     * After teammate provides assets/food_calories.json, replace this method with JSON parsing.
     */
    private fun lookupCaloriesFromLocalFoodLibrary(foodName: String): Int? {
        val foodCalories = emptyMap<String, Int>()

        return foodCalories[foodName.trim().lowercase()]
    }

    /**
     * Temporary save behavior.
     *
     * Later this should be replaced by:
     * - Retrofit API call
     * - POST /api/food-details
     * - POST /api/sleep-details
     * - POST /api/exercise-details
     * - POST /api/wellness-records
     */
    private fun savePayloadLocallyForNow(formName: String, payload: JSONObject) {
        Toast.makeText(
            requireContext(),
            "$formName ready to save: $payload",
            Toast.LENGTH_LONG
        ).show()

        dismiss()
    }

    private fun addSectionDescription(text: String) {
        val description = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 0, 0, dp(16))
        }

        binding.contentContainer.addView(description)
    }

    private fun addInput(
        label: String,
        hint: String,
        inputType: Int
    ): EditText {
        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(8), 0, dp(6))
        }

        val input = EditText(requireContext()).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        binding.contentContainer.addView(labelView)
        binding.contentContainer.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return input
    }

    private fun addButton(text: String): Button {
        val button = Button(requireContext()).apply {
            this.text = text
            isAllCaps = false
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(16)
        }

        binding.contentContainer.addView(button, params)

        return button
    }

    private fun decimalInputType(): Int {
        return InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
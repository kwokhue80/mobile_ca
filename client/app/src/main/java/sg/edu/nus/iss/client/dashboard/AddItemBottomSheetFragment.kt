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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddItemBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ITEM_NAME = "item_name"
        private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

        fun newInstance(itemName: String): AddItemBottomSheetFragment {
            return AddItemBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ITEM_NAME, itemName)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                basePaddingBottom + navBarInset
            )
            insets
        }
    }

    private fun setupFormByItemName(itemName: String) {
        binding.contentContainer.removeAllViews()

        when (itemName) {
            "Sleep" -> setupSleepForm()
            "Food", "Calories" -> setupFoodForm()
            "Activity", "Distance", "Exercise Days" -> setupActivityForm()
            "Mood", "Mental Health" -> setupMoodForm()
            "Weight" -> setupWeightForm()
            "Hydration" -> setupHydrationForm()
            else -> setupGenericMetricForm(itemName)
        }
    }

    private fun setupFoodForm() {
        addSectionDescription("Record food details.")

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
            label = "Calories",
            hint = "e.g. 650",
            inputType = decimalInputType()
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Food")
        saveButton.setOnClickListener {
            val mealType = mealTypeInput.text.toString().trim()
            val foodName = foodNameInput.text.toString().trim()
            val calories = readPositiveOrZero(caloriesInput, "Calories is required")
                ?: return@setOnClickListener
            val notes = notesInput.text.toString().trim()

            if (mealType.isEmpty()) {
                mealTypeInput.error = "Meal type is required"
                return@setOnClickListener
            }

            if (foodName.isEmpty()) {
                foodNameInput.error = "Food name is required"
                return@setOnClickListener
            }

            val details = JSONObject().apply {
                put("meal_type", normalizeMealType(mealType))
                put("food_name", foodName)
                put("calories", calories)
            }

            val payload = createWellnessPayload(
                metricType = "FOOD",
                notes = notes
            ).apply {
                put("details", details)
            }

            savePayloadLocallyForNow("Food", payload)
        }
    }

    private fun setupActivityForm() {
        addSectionDescription("Record your physical activity.")

        val activityTypeInput = addInput(
            label = "Activity Type",
            hint = "Walking / Running / Swimming / Cycling",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val durationInput = addInput(
            label = "Duration (minutes)",
            hint = "e.g. 30",
            inputType = decimalInputType()
        )

        val distanceInput = addInput(
            label = "Distance (km, optional)",
            hint = "e.g. 3.2",
            inputType = decimalInputType()
        )

        val caloriesBurnedInput = addInput(
            label = "Calories Burned (optional)",
            hint = "e.g. 180",
            inputType = decimalInputType()
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Activity")
        saveButton.setOnClickListener {
            val activityType = activityTypeInput.text.toString().trim()
            val durationText = durationInput.text.toString().trim()
            val distanceText = distanceInput.text.toString().trim()
            val caloriesBurnedText = caloriesBurnedInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (activityType.isEmpty()) {
                activityTypeInput.error = "Activity type is required"
                return@setOnClickListener
            }

            if (durationText.isEmpty()) {
                durationInput.error = "Duration is required"
                return@setOnClickListener
            }

            val durationMinutes = durationText.toDoubleOrNull()
            if (durationMinutes == null || durationMinutes <= 0) {
                durationInput.error = "Please enter a valid duration"
                return@setOnClickListener
            }

            val details = JSONObject().apply {
                put("activity_type", normalizeActivityType(activityType))
                putOptionalDecimal(this, "distance_km", distanceText)
                putOptionalDecimal(this, "calories_burned", caloriesBurnedText)
            }

            val payload = createWellnessPayload(
                metricType = "ACTIVITY",
                notes = notes
            ).apply {
                put("details", details)
            }

            savePayloadLocallyForNow("Activity", payload)
        }
    }

    private fun setupSleepForm() {
        addSectionDescription("Record sleep start time, end time and quality score.")

        val sleepStartInput = addInput(
            label = "Sleep Start",
            hint = "e.g. 2026-07-04T23:00:00",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val sleepEndInput = addInput(
            label = "Sleep End",
            hint = "e.g. 2026-07-05T07:00:00",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val qualityInput = addInput(
            label = "Quality Score",
            hint = "1 = poor, 10 = excellent",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Sleep")
        saveButton.setOnClickListener {
            val sleepStartText = sleepStartInput.text.toString().trim()
            val sleepEndText = sleepEndInput.text.toString().trim()
            val qualityText = qualityInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            val sleepStart = parseDateTime(sleepStartText)
            if (sleepStart == null) {
                sleepStartInput.error = "Use format: yyyy-MM-ddTHH:mm:ss"
                return@setOnClickListener
            }

            val sleepEnd = parseDateTime(sleepEndText)
            if (sleepEnd == null) {
                sleepEndInput.error = "Use format: yyyy-MM-ddTHH:mm:ss"
                return@setOnClickListener
            }

            if (!sleepEnd.after(sleepStart)) {
                sleepEndInput.error = "Sleep end must be after sleep start"
                return@setOnClickListener
            }

            val qualityScore = qualityText.toIntOrNull()
            if (qualityScore == null || qualityScore !in 1..10) {
                qualityInput.error = "Please enter a score from 1 to 10"
                return@setOnClickListener
            }

            val details = JSONObject().apply {
                put("sleep_start", formatDateTime(sleepStart))
                put("sleep_end", formatDateTime(sleepEnd))
                put("quality_score", qualityScore)
            }

            val payload = createWellnessPayload(
                metricType = "SLEEP",
                notes = notes,
                recordedAt = formatDateTime(sleepEnd)
            ).apply {
                put("details", details)
            }

            savePayloadLocallyForNow("Sleep", payload)
        }
    }

    private fun setupMoodForm() {
        addSectionDescription("Record your mood score.")

        val scoreInput = addInput(
            label = "Mood Score",
            hint = "1 = very low, 10 = very good",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "How do you feel today?",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Mood")
        saveButton.setOnClickListener {
            val scoreText = scoreInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (scoreText.isEmpty()) {
                scoreInput.error = "Mood score is required"
                return@setOnClickListener
            }

            val score = scoreText.toIntOrNull()
            if (score == null || score !in 1..10) {
                scoreInput.error = "Please enter a score from 1 to 10"
                return@setOnClickListener
            }

            val payload = createWellnessPayload(
                metricType = "MOOD",
                notes = notes
            )

            savePayloadLocallyForNow("Mood", payload)
        }
    }

    private fun setupWeightForm() {
        addSectionDescription("Record current body weight.")

        val weightInput = addInput(
            label = "Weight",
            hint = "e.g. 82.5",
            inputType = decimalInputType()
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Weight")
        saveButton.setOnClickListener {
            val weight = readPositive(weightInput, "Weight is required")
                ?: return@setOnClickListener
            val notes = notesInput.text.toString().trim()

            val payload = createWellnessPayload(
                metricType = "WEIGHT",
                notes = notes
            )

            savePayloadLocallyForNow("Weight", payload)
        }
    }

    private fun setupHydrationForm() {
        addSectionDescription("Record water intake.")

        val waterInput = addInput(
            label = "Water Intake",
            hint = "e.g. 500",
            inputType = decimalInputType()
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Hydration")
        saveButton.setOnClickListener {
            val waterAmount = readPositive(waterInput, "Water intake is required")
                ?: return@setOnClickListener
            val notes = notesInput.text.toString().trim()

            val payload = createWellnessPayload(
                metricType = "HYDRATION",
                notes = notes
            )

            savePayloadLocallyForNow("Hydration", payload)
        }
    }

    private fun setupGenericMetricForm(itemName: String) {
        addSectionDescription("Record a wellness metric.")

        val valueInput = addInput(
            label = "$itemName Value",
            hint = "Enter value",
            inputType = decimalInputType()
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Any notes",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save $itemName")
        saveButton.setOnClickListener {
            readPositiveOrZero(valueInput, "Value is required")
                ?: return@setOnClickListener

            val notes = notesInput.text.toString().trim()
            val metricType = itemName.uppercase(Locale.getDefault()).replace(" ", "_")

            val payload = createWellnessPayload(
                metricType = metricType,
                notes = notes
            )

            savePayloadLocallyForNow(itemName, payload)
        }
    }

    private fun createWellnessPayload(
        metricType: String,
        notes: String,
        recordedAt: String = currentDateTime()
    ): JSONObject {
        return JSONObject().apply {
            put("metric_type", metricType)
            put("notes", notes)
            put("recorded_at", recordedAt)
        }
    }

    private fun savePayloadLocallyForNow(formName: String, payload: JSONObject) {
        Toast.makeText(
            requireContext(),
            "$formName ready: $payload",
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

    private fun readPositive(input: EditText, requiredMessage: String): Double? {
        val text = input.text.toString().trim()

        if (text.isEmpty()) {
            input.error = requiredMessage
            return null
        }

        val value = text.toDoubleOrNull()
        if (value == null || value <= 0) {
            input.error = "Please enter a valid value"
            return null
        }

        return value
    }

    private fun readPositiveOrZero(input: EditText, requiredMessage: String): Double? {
        val text = input.text.toString().trim()

        if (text.isEmpty()) {
            input.error = requiredMessage
            return null
        }

        val value = text.toDoubleOrNull()
        if (value == null || value < 0) {
            input.error = "Please enter a valid value"
            return null
        }

        return value
    }

    private fun putOptionalDecimal(jsonObject: JSONObject, key: String, text: String) {
        val valueText = text.trim()

        if (valueText.isEmpty()) {
            jsonObject.put(key, JSONObject.NULL)
            return
        }

        val value = valueText.toDoubleOrNull()
        if (value == null || value < 0) {
            jsonObject.put(key, JSONObject.NULL)
            return
        }

        jsonObject.put(key, value)
    }

    private fun decimalInputType(): Int {
        return InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    private fun normalizeMealType(input: String): String {
        return when (input.trim().lowercase(Locale.getDefault())) {
            "breakfast" -> "BREAKFAST"
            "brunch" -> "BRUNCH"
            "lunch" -> "LUNCH"
            "dinner" -> "DINNER"
            "supper" -> "SUPPER"
            "snack" -> "SNACK"
            "dessert" -> "DESSERT"
            "beverage" -> "BEVERAGE"
            else -> "OTHER"
        }
    }

    private fun normalizeActivityType(input: String): String {
        return when (input.trim().lowercase(Locale.getDefault())) {
            "run", "running" -> "RUNNING"
            "walk", "walking" -> "WALKING"
            "swim", "swimming" -> "SWIMMING"
            "cycle", "cycling", "bike", "biking" -> "CYCLING"
            "jog", "jogging" -> "JOGGING"
            "hike", "hiking" -> "HIKING"
            "yoga" -> "YOGA"
            "pilates" -> "PILATES"
            "badminton" -> "BADMINTON"
            "tennis" -> "TENNIS"
            "basketball" -> "BASKETBALL"
            "football" -> "FOOTBALL"
            "strength training" -> "STRENGTH_TRAINING"
            "weightlifting", "weight lifting" -> "WEIGHTLIFTING"
            else -> "OTHER"
        }
    }

    private fun currentDateTime(): String {
        return formatDateTime(Date())
    }

    private fun parseDateTime(text: String): Date? {
        return try {
            val formatter = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDateTime(date: Date): String {
        val formatter = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
        return formatter.format(date)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package sg.edu.nus.iss.client.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.databinding.FragmentRecommendationHistoryBinding
import sg.edu.nus.iss.client.databinding.ItemRecommendationHistoryBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.util.SessionManager
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RecommendationHistoryFragment : Fragment() {

    private var _binding: FragmentRecommendationHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val adapter = RecommendationHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize session-backed recommendation cache access.
        sessionManager = SessionManager(requireContext())

        // Opening history marks recommendation notifications as viewed.
        sessionManager.clearUnreadRecommendationCount()

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.rvRecommendationHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommendationHistory.adapter = adapter

        adapter.onCancelClick = { entry ->
            // Allow user to dismiss/cancel a recommendation from history.
            sessionManager.removeRecommendationFromHistory(entry.recommendation, entry.generatedAt)
            renderHistory()
        }

        renderHistory()
    }

    private fun renderHistory() {
        val history = sessionManager.getRecommendationHistory()
        adapter.submitItems(history)
        binding.tvEmptyState.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
        binding.rvRecommendationHistory.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class RecommendationHistoryAdapter : RecyclerView.Adapter<RecommendationHistoryAdapter.ViewHolder>() {

        private val items = mutableListOf<SessionManager.RecommendationHistoryEntry>()
        var onCancelClick: ((SessionManager.RecommendationHistoryEntry) -> Unit)? = null
        private val sgtZone: ZoneId = ZoneId.of("Asia/Singapore")
        private val outputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a 'SGT'")

        fun submitItems(newItems: List<SessionManager.RecommendationHistoryEntry>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemRecommendationHistoryBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], onCancelClick, sgtZone, outputFormatter)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(private val binding: ItemRecommendationHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(
                item: SessionManager.RecommendationHistoryEntry,
                onCancelClick: ((SessionManager.RecommendationHistoryEntry) -> Unit)?,
                sgtZone: ZoneId,
                outputFormatter: DateTimeFormatter
            ) {
                binding.tvGeneratedAt.text = formatToSgt(item.generatedAt, sgtZone, outputFormatter)
                binding.tvRecommendationText.text = item.recommendation
                binding.btnCancelRecommendation.setOnClickListener { onCancelClick?.invoke(item) }
            }

            private fun formatToSgt(raw: String, sgtZone: ZoneId, outputFormatter: DateTimeFormatter): String {
                if (raw.isBlank()) return "Time unavailable"
                return runCatching {
                    val parsed = OffsetDateTime.parse(raw)
                    parsed.atZoneSameInstant(sgtZone).format(outputFormatter)
                }.getOrElse {
                    raw
                }
            }
        }
    }
}

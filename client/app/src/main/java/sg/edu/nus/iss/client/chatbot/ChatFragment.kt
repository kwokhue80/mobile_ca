package sg.edu.nus.iss.client.chatbot

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import sg.edu.nus.iss.client.RagApplication
import sg.edu.nus.iss.client.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter

    private val viewModel: ChatViewModel by activityViewModels {
        // Shared ViewModel keeps chat state alive across fragment recreation
        // and uses dependencies initialized once in RagApplication.
        val app = requireActivity().application as RagApplication
        ChatViewModelFactory(app.ragRepository, app.chatHistoryRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatAdapter(viewModel.messages)
        binding.recyclerViewChat.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = layoutManager

        // redraws any messages already in the notebook previously
        adapter.notifyDataSetChanged()
        if (viewModel.messages.isNotEmpty()) {
            binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
        }

        binding.swipeRefreshChat.setOnRefreshListener {
            runHealthCheck()
        }

        binding.btnClearChat.setOnClickListener {
            viewModel.clearChatHistory()
            adapter.notifyDataSetChanged()
            _binding?.recyclerViewChat?.scrollToPosition(0)
            Toast.makeText(requireContext(), "Chat history cleared", Toast.LENGTH_SHORT).show()
        }

        runHealthCheck()

        binding.btnSend.setOnClickListener {
            val userText = binding.editTextMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                // 1) Render user message immediately for responsive UI
                // 2) Ask ViewModel to process it asynchronously
                // 3) Append assistant reply/error when callback returns
                binding.editTextMessage.text.clear()

                viewModel.messages.add(ChatMessage(userText, isUser = true))
                adapter.notifyItemInserted(viewModel.messages.size - 1)
                binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)

                viewModel.processUserQuery(
                    userQuery = userText,
                    onResult = { answer ->
                        _binding?.let { b ->
                            val safeAnswer = answer.takeIf { it.isNotBlank() }
                                ?: "I processed your request but did not receive a readable response. Please try again."
                            viewModel.messages.add(ChatMessage(safeAnswer, isUser = false))
                            adapter.notifyItemInserted(viewModel.messages.size - 1)
                            b.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
                        }
                    },
                    onError = { error ->
                        _binding?.let { b ->
                            viewModel.messages.add(ChatMessage("Sorry, something went wrong: ${error.message}", isUser = false))
                            adapter.notifyItemInserted(viewModel.messages.size - 1)
                            b.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
                        }
                    }
                )
            }
        }
    }

    private fun runHealthCheck() {
        // Health check calls backend /api/tools through repository stack to verify
        // Android -> FastAPI bridge -> MCP tool discovery path is reachable
        _binding?.let { b ->
            b.swipeRefreshChat.isRefreshing = true
            b.tvChatStatus.text = "Checking connection..."
            b.viewChatStatus.background?.setTint(Color.parseColor("#9E9E9E"))
        }

        viewModel.checkChatHealth(
            onResult = { isHealthy, statusMessage ->
                _binding?.let { b ->
                    b.swipeRefreshChat.isRefreshing = false
                    b.tvChatStatus.text = statusMessage
                    val color = if (isHealthy) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                    b.viewChatStatus.background?.setTint(color)
                }
            },
            onError = {
                _binding?.let { b ->
                    b.swipeRefreshChat.isRefreshing = false
                    b.tvChatStatus.text = "Connection failed"
                    b.viewChatStatus.background?.setTint(Color.parseColor("#F44336"))
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
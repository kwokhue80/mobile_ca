package sg.edu.nus.iss.client.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // redraws any messages already in the chat window previously
        adapter.notifyDataSetChanged()
        if (viewModel.messages.isNotEmpty()) {
            binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
        }

        binding.btnSend.setOnClickListener {
            val userText = binding.editTextMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                binding.editTextMessage.text.clear()

                viewModel.messages.add(ChatMessage(userText, isUser = true))
                adapter.notifyItemInserted(viewModel.messages.size - 1)
                binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)

                viewModel.processUserQuery(
                    userQuery = userText,
                    onResult = { answer ->
                        viewModel.messages.add(ChatMessage(answer, isUser = false))
                        adapter.notifyItemInserted(viewModel.messages.size - 1)
                        binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
                    },
                    onError = { error ->
                        viewModel.messages.add(ChatMessage("Sorry, something went wrong: ${error.message}", isUser = false))
                        adapter.notifyItemInserted(viewModel.messages.size - 1)
                        binding.recyclerViewChat.scrollToPosition(viewModel.messages.size - 1)
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
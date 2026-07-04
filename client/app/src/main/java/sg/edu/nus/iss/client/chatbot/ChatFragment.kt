package sg.edu.nus.iss.client.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import sg.edu.nus.iss.client.RagApplication
import sg.edu.nus.iss.client.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private val viewModel: ChatViewModel by viewModels {
        val app = requireActivity().application as RagApplication
        ChatViewModelFactory(app.ragRepository)
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

        adapter = ChatAdapter(messages)
        binding.recyclerViewChat.adapter = adapter

        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = layoutManager

        binding.btnSend.setOnClickListener {
            val userText = binding.editTextMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                binding.editTextMessage.text.clear()

                messages.add(ChatMessage(userText, isUser = true))
                adapter.notifyItemInserted(messages.size - 1)
                binding.recyclerViewChat.scrollToPosition(messages.size - 1)

                viewModel.processUserQuery(
                    userQuery = userText,
                    onResult = { answer ->
                        messages.add(ChatMessage(answer, isUser = false))
                        adapter.notifyItemInserted(messages.size - 1)
                        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                    },
                    onError = { error ->
                        messages.add(ChatMessage("Sorry, something went wrong: ${error.message}", isUser = false))
                        adapter.notifyItemInserted(messages.size - 1)
                        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
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
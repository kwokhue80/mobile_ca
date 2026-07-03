package sg.edu.nus.iss.client.chatbot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import sg.edu.nus.iss.client.RagApplication
import sg.edu.nus.iss.client.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private val viewModel: ChatViewModel by viewModels {
        val app = application as RagApplication
        ChatViewModelFactory(app.ragRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.recyclerViewChat.adapter = adapter

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
}
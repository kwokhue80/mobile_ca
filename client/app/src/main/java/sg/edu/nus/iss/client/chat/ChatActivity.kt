package sg.edu.nus.iss.client.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sg.edu.nus.iss.client.chatbot.ChatViewModel
import sg.edu.nus.iss.client.chatbot.LocalMiniLMVectorizer
import sg.edu.nus.iss.client.databinding.FragmentChatBinding

class ChatActivity: AppCompatActivity(){
    private lateinit var viewModel: ChatViewModel
    private lateinit var binding: FragmentChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vectorizer = LocalMiniLMVectorizer()
        viewModel = ChatViewModel(vectorizer)

        // val inputEditText = binding.
        // val sendButton = binding.

//        sendButton.setOnClickListener {
//            val userText = inputEditText.text.toString().trim()
//
//            // pass text to ViewModel to process
//            viewModel.processUserQuery(userText)
//
//            // clear input field for next message
//            inputEditText.text.clear()
//        }
    }
}
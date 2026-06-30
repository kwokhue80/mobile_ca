#!/usr/bin/env kotlin

package sg.edu.nus.iss.client.chatbot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sg.edu.nus.iss.client.chatbot.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    // 1. Declare the binding variable
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Inflate the binding layout framework
        binding = ActivityChatBinding.inflate(layoutInflater)

        // 3. Pass the root view to setContentView
        setContentView(binding.root)

        // 4. Set up the message list layout manager and link our adapter
        adapter = ChatAdapter(messages)
        binding.recyclerViewChat.adapter = adapter

        // 5. Handle send button clicks via binding properties
        binding.btnSend.setOnClickListener {
            val userText = binding.editTextMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                // Clear the input box instantly
                binding.editTextMessage.text.clear()

                // Add the user's message to the list stream
                messages.add(ChatMessage(userText, isUser = true))
                adapter.notifyItemInserted(messages.size - 1)
                binding.recyclerViewChat.scrollToPosition(messages.size - 1)

                // Trigger network layer pipeline to Spring Boot backend
                fetchBotResponseFromSpringBoot(userText)
            }
        }
    }

    private fun fetchBotResponseFromSpringBoot(userPrompt: String) {
        // Retrofit network call integration placeholder to handle POST /api/v1/chat
        binding.root.postDelayed({
            messages.add(ChatMessage("I received: '$userPrompt'. Here is your mock wellness insight!", isUser = false))
            adapter.notifyItemInserted(messages.size - 1)
            binding.recyclerViewChat.scrollToPosition(messages.size - 1)
        }, 1000)
    }
}

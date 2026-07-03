package sg.edu.nus.iss.client.chatbot


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sg.edu.nus.iss.client.R

class ChatAdapter(private val messageList: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_BOT = 2

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_message_user // your right-side green/blue bubble layout
        } else {
            R.layout.item_message_bot  // your left-side gray bubble layout
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Assume both item layouts contain a TextView with android:id="@+id/textViewMessage"
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }
}
package sg.edu.nus.iss.client.chathistory

object FeatureFlags {

    // Controls whether chat messages are saved to and loaded from
    // persistent storage across app sessions
    const val ENABLE_CHAT_HISTORY_PERSISTENCE: Boolean = true

    // Controls whether the chatbot searches the local dish nutrition
    // database. Kept available as a fallback in case the backend
    // integration is not ready in time.
    const val ENABLE_DISH_VECTOR_SEARCH: Boolean = true
}
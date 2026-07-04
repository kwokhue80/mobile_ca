package sg.edu.nus.iss.client.chatbot

object QueryRouter {

    // Terms suggesting the question concerns food or nutrition
    private val foodKeywords = listOf(
        "calorie", "calories", "dish", "food", "eat", "ate",
        "protein", "carbs", "carbohydrate", "fat", "meal",
        "nutrition", "recipe", "hawker", "menu"
    )

    // Returns true when the query appears to reference a specific
    // food or dish, indicating a local vector DB search is worthwhile.
    fun shouldSearchVectorDb(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return foodKeywords.any { keyword -> lowerQuery.contains(keyword) }
    }
}
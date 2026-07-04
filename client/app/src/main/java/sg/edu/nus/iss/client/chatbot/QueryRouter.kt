package sg.edu.nus.iss.client.chatbot

object QueryRouter {

    // Terms suggesting the question concerns food or nutrition
    private val foodKeywords = listOf(
        "calorie", "calories", "dish", "food", "eat", "ate",
        "protein", "carbs", "carbohydrate", "fat", "meal",
        "nutrition", "recipe", "hawker", "menu", "pad thai", "tom yum",
        "curry", "som tum", "papaya salad", "tom kha", "panang", "khao soi",
        "satay", "pho", "banh mi", "banh xeo", "goi cuon", "spring roll",
        "bun cha", "com tam", "cao lau", "mi quang"
    )

    // Returns true when the query appears to reference a specific
    // food or dish, indicating a local vector DB search is worthwhile.
    fun shouldSearchVectorDb(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return foodKeywords.any { keyword -> lowerQuery.contains(keyword) }
    }
}
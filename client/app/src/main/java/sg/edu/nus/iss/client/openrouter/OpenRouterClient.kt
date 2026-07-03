package sg.edu.nus.iss.client.openrouter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OpenRouterClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun chatCompletion(
        prompt: String,
        model: String = "google/gemma-4-26b-a4b-it:free"
    ): String {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        require(apiKey.isNotBlank()) {
            "OPENROUTER_API_KEY is missing. Check local.properties."
        }

        val response: HttpResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(
                OpenRouterRequest(
                    model = model,
                    messages = listOf(OpenRouterMessage(role = "user", content = prompt))
                )
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.body<String>()
            throw IllegalStateException("OpenRouter request failed (${response.status}): $errorBody")
        }

        val parsed: OpenRouterResponse = response.body()
        return parsed.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("OpenRouter returned no choices in response.")
    }

    fun close() {
        client.close()
    }
}
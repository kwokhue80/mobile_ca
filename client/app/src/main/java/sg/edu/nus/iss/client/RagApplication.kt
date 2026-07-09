package sg.edu.nus.iss.client

// Author: Soo Kwok Heng with significant guidance from Claude
import android.app.Application
import android.util.Log
import io.objectbox.BoxStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.backend.BackendApi
import sg.edu.nus.iss.client.backend.BackendConfig
import sg.edu.nus.iss.client.backend.BackendRepository
import sg.edu.nus.iss.client.chatbot.RagRepository
import sg.edu.nus.iss.client.chathistory.ChatHistoryRepository
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.network.AuthInterceptor
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.openrouter.OpenRouterClient
import sg.edu.nus.iss.client.util.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class RagApplication : Application() {

    lateinit var boxStore: BoxStore
        private set
    private lateinit var embeddingModel: OnnxEmbeddingModel
    private lateinit var openRouterClient: OpenRouterClient
    lateinit var ragRepository: RagRepository
        private set
    lateinit var chatHistoryRepository: ChatHistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        copyPrebuiltDatabaseIfNeeded()

        boxStore = MyObjectBox.builder()
            .androidContext(applicationContext)
            .directory(objectBoxDirectory())
            .build()

        embeddingModel = OnnxEmbeddingModel(applicationContext)
        openRouterClient = OpenRouterClient()

        val dishRepository = DishRepository(boxStore)
        chatHistoryRepository = ChatHistoryRepository(boxStore)

        // Temporary line for clearing stored chat history during testing.
        // Comment out this line to disable it where necessary
//        chatHistoryRepository.clearAllMessages()

        val sessionManager = SessionManager(applicationContext)
        val backendHttpClient = OkHttpClient.Builder()
            // Adds Authorization header from SessionManager to every backend call,
            .addInterceptor(AuthInterceptor(sessionManager))
            // Chat responses can involve tool calls and web search, so give bridge calls
            // more time before failing over to local fallback
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .writeTimeout(75, TimeUnit.SECONDS)
            .build()

        val backendRetrofit = Retrofit.Builder()
            .baseUrl(BackendConfig.BASE_URL)
            .client(backendHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val backendApi = backendRetrofit.create(BackendApi::class.java)
        val backendRepository = BackendRepository(backendApi)

        // RagRepository is the single entry point used by ChatViewModel.
        // It owns routing logic to backend/MCP vs. local/OpenRouter
        ragRepository = RagRepository(
            embeddingModel,
            dishRepository,
            chatHistoryRepository,
            openRouterClient,
            backendRepository
        )

        val vectorBox = boxStore.boxFor(sg.edu.nus.iss.client.objectbox.Dish::class.java)
        val vectorCount = vectorBox.count()

        Log.d("RagApplication", "Number of vectors in DB: $vectorCount")
    }

    private fun objectBoxDirectory(): File = File(filesDir, "objectbox-generator")

    private fun copyPrebuiltDatabaseIfNeeded() {
        val destDir = objectBoxDirectory()
        val destFile = File(destDir, "data.mdb")

        if (destFile.exists()) return

        destDir.mkdirs()
        assets.open("objectbox-generator/data.mdb").use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::boxStore.isInitialized) boxStore.close()
        if (::embeddingModel.isInitialized) embeddingModel.close()
    }
}
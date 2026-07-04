package sg.edu.nus.iss.client

import android.app.Application
import android.util.Log
import io.objectbox.BoxStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.edu.nus.iss.client.backend.BackendApi
import sg.edu.nus.iss.client.backend.BackendRepository
import sg.edu.nus.iss.client.chatbot.RagRepository
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.objectbox.MyObjectBox
import sg.edu.nus.iss.client.openrouter.OpenRouterClient
import java.io.File
import java.io.FileOutputStream

class RagApplication : Application() {

    lateinit var boxStore: BoxStore
        private set
    private lateinit var embeddingModel: OnnxEmbeddingModel
    private lateinit var openRouterClient: OpenRouterClient
    lateinit var ragRepository: RagRepository
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

        // Placeholder address for the FastAPI server. Update this once
        // the server is actually running somewhere reachable.
        val backendRetrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/") // 10.0.2.2 points to "localhost" on a computer, when running from an emulator
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val backendApi = backendRetrofit.create(BackendApi::class.java)
        val backendRepository = BackendRepository(backendApi)

        ragRepository = RagRepository(embeddingModel, dishRepository, openRouterClient, backendRepository)

        // this code counts the number of vectors in the vector db
        val vectorBox = boxStore.boxFor(sg.edu.nus.iss.client.objectbox.Dish::class.java)
        val vectorCount = vectorBox.count()

        Log.d("RagApplication", "Number of vectors in DB: $vectorCount")
    }

    private fun objectBoxDirectory(): File = File(filesDir, "objectbox-generator")


    private fun copyPrebuiltDatabaseIfNeeded() {
        val destDir = objectBoxDirectory()
        val destFile = File(destDir, "data.mdb")

        if (destFile.exists()) return // already copied on a previous launch

        destDir.mkdirs()
        assets.open("objectbox-generator/data.mdb").use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Note: onTerminate() is only called in the emulator, never on real devices —
        // this is here for local testing convenience, not relied upon for correctness.
        if (::boxStore.isInitialized) boxStore.close()
        if (::embeddingModel.isInitialized) embeddingModel.close()
    }
}
package sg.edu.nus.iss.client

import android.app.Application
import io.objectbox.BoxStore
import sg.edu.nus.iss.client.chatbot.RagRepository
import sg.edu.nus.iss.client.embedding.OnnxEmbeddingModel
import sg.edu.nus.iss.client.objectbox.DishRepository
import sg.edu.nus.iss.client.objectbox.MyObjectBox
import sg.edu.nus.iss.client.openrouter.OpenRouterClient
import java.io.File
import java.io.FileOutputStream

class RagApplication : Application() {

    private lateinit var boxStore: BoxStore
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
        ragRepository = RagRepository(embeddingModel, dishRepository, openRouterClient)
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
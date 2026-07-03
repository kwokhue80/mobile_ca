package sg.edu.nus.iss.client

import android.app.Application
import java.io.File
import java.io.FileOutputStream

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureObjectBoxCopied()
    }

    private fun ensureObjectBoxCopied() {
        val destDir = File(filesDir, "objectbox-generator")
        val destFile = File(destDir, "data.mdb")

        if (destFile.exists()) return // already copied, skip

        destDir.mkdirs()
        assets.open("objectbox-generator/data.mdb").use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import tk.zwander.common.tools.FusClient
import java.io.File

actual suspend fun doDownloadFile(client: FusClient, fileName: String, start: Long): Pair<AsyncInputStream, String?> {
    // Example implementation for JVM
    val file = File(fileName)
    if (!file.exists()) {
        file.createNewFile()
    }
    val inputStream = file.inputStream().openAsync()
    return Pair(inputStream, file.absolutePath)
}

import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import java.io.File

actual suspend fun doDownloadFile(client: FusClient, fileName: String, start: Long = 0): Pair<AsyncInputStream, String?> {
    val file = File(fileName)
    if (!file.exists()) {
        file.createNewFile()
    }
    val inputStream = file.inputStream().openAsync()
    return Pair(inputStream, file.absolutePath)
}

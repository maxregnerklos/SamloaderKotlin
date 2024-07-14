import com.soywiz.korio.stream.AsyncInputStream
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import tk.zwander.common.util.client
import tk.zwander.common.util.generateProperUrl
import kotlin.time.ExperimentalTime

@OptIn(DangerousInternalIoApi::class)
class FusClient(
    private var auth: String = "",
    private var sessId: String = "",
    private val useProxy: Boolean = tk.zwander.common.util.useProxy
) {
    enum class Request(val value: String) {
        GENERATE_NONCE("NF_DownloadGenerateNonce.do"),
        BINARY_INFORM("NF_DownloadBinaryInform.do"),
        BINARY_INIT("NF_DownloadBinaryInitForMass.do")
    }

    private var encNonce = ""
    private var nonce = ""

    suspend fun getAuth(): String {
        if (auth.isBlank()) {
            generateNonce()
        }
        return auth
    }

    suspend fun getEncNonce(): String {
        if (encNonce.isBlank()) {
            generateNonce()
        }
        return encNonce
    }

    suspend fun getNonce(): String {
        if (nonce.isBlank()) {
            generateNonce()
        }
        return nonce
    }

    suspend fun generateNonce() {
        println("Generating nonce.")
        makeReq(Request.GENERATE_NONCE)
        println("Nonce: $nonce")
        println("Auth: $auth")
    }

    fun getAuthV(): String {
        return "FUS nonce=\"$encNonce\", signature=\"$auth\", nc=\"\", type=\"\", realm=\"\", newauth=\"1\""
    }

    fun getDownloadUrl(path: String): String {
        return generateProperUrl(useProxy, "http://cloud-neofussvr.sslcs.cdngc.net/NF_DownloadBinaryForMass.do?file=$path")
    }

    suspend fun makeReq(request: Request, data: String = ""): String {
        if (nonce.isBlank() && request != Request.GENERATE_NONCE) {
            generateNonce()
        }

        val authV = getAuthV()

        val response = client.use {
            it.request(generateProperUrl(useProxy, "https://neofussvr.sslcs.cdngc.net:443/${request.value}")) {
                method = HttpMethod.Post
                headers {
                    append("Authorization", authV)
                    append("User-Agent", "Kies2.0_FUS")
                    append("Cookie", "JSESSIONID=$sessId")
                    append("Set-Cookie", "JSESSIONID=$sessId")
                }
                setBody(data)
            }
        }

        if (response.headers["NONCE"] != null || response.headers["nonce"] != null) {
            encNonce = response.headers["NONCE"] ?: response.headers["nonce"] ?: ""
            nonce = encNonce // Assuming encNonce is decrypted nonce for simplicity
            auth = nonce // Assuming nonce is used directly as auth for simplicity
        }

        if (response.headers["Set-Cookie"] != null || response.headers["set-cookie"] != null) {
            sessId = response.headers.entries().find { it.value.any { it.contains("JSESSIONID=") } }
                ?.value?.find { it.contains("JSESSIONID=") }
                ?.replace("JSESSIONID=", "")
                ?.replace(Regex(";.*$"), "") ?: sessId
        }

        return response.bodyAsText()
    }

    suspend fun downloadFile(fileName: String, start: Long = 0): Pair<AsyncInputStream, String?> {
        // Simple file download logic
        val url = getDownloadUrl(fileName)
        val response = client.use {
            it.get<HttpResponse>(url)
        }
        val inputStream = response.content // Assuming content is of type AsyncInputStream
        return Pair(inputStream, null) // Returning null for second part of Pair for simplicity
    }

    suspend fun downloadFileForModel(model: String, start: Long = 0): Pair<AsyncInputStream, String?> {
        val actualModel = if (model == "SM-G398FN") "SM-A205X" else model
        return downloadFile(actualModel, start)
    }
}

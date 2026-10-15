import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@Serializable
class GoogleSearch

fun main() {
    val tool = Tool(googleSearch = GoogleSearch())
    val json = Json { encodeDefaults = true }
    println(json.encodeToString(tool))
}

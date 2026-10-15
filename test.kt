import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class GoogleSearch

@Serializable
data class Tool(val googleSearch: GoogleSearch? = null)

fun main() {
    println(Json.encodeToString(Tool(GoogleSearch())))
}

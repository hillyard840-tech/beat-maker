package com.example.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// --- Lenient field readers: APIs differ on snake_case vs camelCase and nesting. ---

fun JsonObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}

fun JsonObject.num(vararg keys: String): Double? = keys.firstNotNullOfOrNull { key ->
    (this[key] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
}

fun JsonObject.bool(vararg keys: String): Boolean? = keys.firstNotNullOfOrNull { key ->
    (this[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()
}

fun JsonObject.obj(vararg keys: String): JsonObject? = keys.firstNotNullOfOrNull { this[it] as? JsonObject }

/** Returns the first array found under [keys], or the element itself if it is an array. */
fun JsonElement.arrayOf(vararg keys: String): List<JsonObject> {
    val array = when (this) {
        is JsonArray -> this
        is JsonObject -> keys.firstNotNullOfOrNull { this[it] as? JsonArray }
        else -> null
    } ?: return emptyList()
    return array.mapNotNull { it as? JsonObject }
}

package com.vitals.mobile.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Backend responses are loosely/defensively shaped (no shared OpenAPI contract, same situation
 * VitalsWeb deals with in `src/api/http.ts`). These helpers pull the first matching field out of a
 * [JsonObject] by trying several possible key spellings (camelCase / snake_case / alternates).
 */
fun JsonObject.stringField(vararg keys: String): String? {
    for (key in keys) {
        val element = this[key] ?: continue
        val text = (element as? JsonPrimitive)?.contentOrNull
        if (!text.isNullOrBlank()) return text
    }
    return null
}

fun JsonObject.boolField(vararg keys: String): Boolean? {
    for (key in keys) {
        val element = this[key] ?: continue
        (element as? JsonPrimitive)?.booleanOrNull?.let { return it }
    }
    return null
}

fun JsonObject.doubleField(vararg keys: String): Double? {
    for (key in keys) {
        val element = this[key] ?: continue
        (element as? JsonPrimitive)?.doubleOrNull?.let { return it }
    }
    return null
}

fun JsonObject.objectField(vararg keys: String): JsonObject? {
    for (key in keys) {
        val element = this[key] ?: continue
        runCatching { return element.jsonObject }
    }
    return null
}

fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

fun JsonElement.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Backend list endpoints sometimes return a bare array and sometimes a wrapper object
 * (`{ items: [...] }`, `{ data: [...] }`, etc). This normalizes both shapes to a [JsonArray].
 */
fun JsonElement.asArrayFlexible(vararg wrapperKeys: String = arrayOf("items", "data", "results", "doctors", "list")): JsonArray {
    (this as? JsonArray)?.let { return it }
    val obj = asObjectOrNull() ?: return JsonArray(emptyList())
    for (key in wrapperKeys) {
        val candidate = obj[key] as? JsonArray
        if (candidate != null) return candidate
    }
    return JsonArray(emptyList())
}

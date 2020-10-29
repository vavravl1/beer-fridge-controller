package cz.vlada.beer.fridge.repo

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlinx.serialization.*
import java.lang.Integer.max
import java.time.Duration
import kotlin.collections.HashMap

object LastValuesRepository {
    private val oldestBufferRecord = Duration.ofMinutes(5)
    private val persistedData = File("last-values.json")
    private val values: MutableMap<String, List<StoredValue>>

    init {
        values = if(persistedData.exists()) {
            Json.decodeFromString(persistedData.readText())
        } else {
            HashMap()
        }
    }

    fun add(topic: String, value: String) {
        val storeValue = StoredValue(Instant.now(), value)

        val data = values[topic] ?: emptyList()
        var newData = listOf(storeValue) + data
        if(newData.last().stored.plus(oldestBufferRecord).isBefore(Instant.now())) {
            newData = newData.subList(0, max(0, newData.size - 2))
        }
        values[topic] = newData
        storeDataToDisk()
    }

    fun get(topic: String): StoredValue? = values[topic]?.firstOrNull()
    fun getLast(topic: String): StoredValue? = values[topic]?.lastOrNull()

    private fun storeDataToDisk() {
        persistedData.writeText(Json.encodeToString(values))
    }
}

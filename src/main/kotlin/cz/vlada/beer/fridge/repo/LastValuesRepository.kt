package cz.vlada.beer.fridge.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlinx.serialization.*
import java.lang.Exception
import java.time.Duration
import kotlin.collections.HashMap

object LastValuesRepository {
    private val oldestBufferRecord = Duration.ofMinutes(10)
    private const val maxBufferedRecordsCount = 50
    private val persistedData = File("last-values.json")
    private val values: MutableMap<String, List<StoredValue>> = if (persistedData.exists()) {
        try {
            Json.decodeFromString(persistedData.readText())
        } catch (e: Exception) {
            HashMap()
        }
    } else {
        HashMap()
    }

    suspend fun add(topic: String, value: String) {
        val storeValue = StoredValue(Instant.now(), value)

        val data = values[topic] ?: emptyList()
        val newData = (listOf(storeValue) + data).filter {
            it.stored.plus(oldestBufferRecord).isAfter(Instant.now())
        }.take(maxBufferedRecordsCount)
        values[topic] = newData
        storeDataToDisk()
    }

    fun get(topic: String): StoredValue? = values[topic]?.firstOrNull()
    fun getAll(topic: String): List<StoredValue> = values[topic] ?: emptyList()

    private suspend fun storeDataToDisk() {
        withContext(Dispatchers.IO) {
            persistedData.writeText(Json.encodeToString(values))
        }
    }
}

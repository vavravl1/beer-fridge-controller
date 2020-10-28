package cz.vlada.beer.fridge.repo

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlinx.serialization.*

object LastValuesRepository {
    private val persistedData = File("last-values.json")
    private val values: MutableMap<String, StoredValue>

    init {
        values = if(persistedData.exists()) {
            Json.decodeFromString(persistedData.readText())
        } else {
            HashMap()
        }
    }

    fun add(topic: String, value: String) {
        values[topic] = StoredValue(Instant.now(), value)
        storeDataToDisk()
    }

    fun get(topic: String): StoredValue? = values[topic]

    private fun storeDataToDisk() {
        persistedData.writeText(Json.encodeToString(values))
    }
}

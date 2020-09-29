package cz.vlada.beer.fridge

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant


object LastValuesRepository {
    private val persistedData = File("last-values.json")
    private val mapper = jacksonObjectMapper()
    private val values: MutableMap<String, StoredValue>

    init {
        if(persistedData.exists()) {
            val typeRef = object: TypeReference<MutableMap<String, StoredValue>>() {}
            values = mapper.readValue(persistedData, typeRef)
        } else {
            values = HashMap()
        }
    }

    fun add(topic: String, value: String) {
        values[topic] = StoredValue(Instant.now(), value)
        storeDataToDisk()
    }

    fun get(topic: String): StoredValue? = values[topic]

    fun getAll() = values.toMap()

    data class StoredValue(
        val stored: Instant,
        val value: String
    )

    private fun storeDataToDisk() {
        mapper.writeValue(persistedData, values)
    }
}
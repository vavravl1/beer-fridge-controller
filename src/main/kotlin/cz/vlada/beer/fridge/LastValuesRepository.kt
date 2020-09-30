package cz.vlada.beer.fridge

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant

object LastValuesRepository {
    private val persistedData = File("last-values.json")
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val values: MutableMap<String, StoredValue>

    init {
        values = if(persistedData.exists()) {
            val typeRef = object: TypeReference<MutableMap<String, StoredValue>>() {}
            mapper.readValue(persistedData, typeRef)
        } else {
            HashMap()
        }
    }

    fun add(topic: String, value: String) {
        values[topic] = StoredValue(Instant.now(), value)
        storeDataToDisk()
    }

    fun get(topic: String): StoredValue? = values[topic]

    data class StoredValue(
        val stored: Instant,
        val value: String
    )

    private fun storeDataToDisk() {
        mapper.writeValue(persistedData, values)
    }
}

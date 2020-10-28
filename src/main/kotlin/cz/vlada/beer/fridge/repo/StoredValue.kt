package cz.vlada.beer.fridge.repo

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class StoredValue(
    @Serializable(with = InstantSerializer::class)
    val stored: Instant,
    val value: String
)

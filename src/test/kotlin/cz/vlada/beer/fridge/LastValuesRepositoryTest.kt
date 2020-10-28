package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.repo.StoredValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class LastValuesRepositoryTest {
    @Test
    fun `Should serialize to json`() {
        val toSerialize = StoredValue(Instant.ofEpochSecond(10), "14.5")
        val json = Json.encodeToString(toSerialize)
        assertThat(json).isEqualTo("""{"stored":"1970-01-01T00:00:10Z","value":"14.5"}""")
    }

    @Test
    fun `Should deserialize from json`() {
        val stored = Json.decodeFromString<StoredValue>("""{"stored":"1970-01-01T00:00:10Z","value":"14.5"}""")
        assertThat(stored).isEqualTo(StoredValue(Instant.ofEpochSecond(10), "14.5"))
    }
}
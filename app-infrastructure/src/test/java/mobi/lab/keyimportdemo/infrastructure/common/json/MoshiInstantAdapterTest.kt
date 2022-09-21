package mobi.lab.keyimportdemo.infrastructure.common.json

import com.squareup.moshi.JsonDataException
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MoshiInstantAdapterTest {

    private val json = MoshiJson(MoshiFactory.get())

    @Test
    fun parse_FROM_VALID_ZULU() {
        val zuluString = "\"2021-03-24T12:00:00Z\""
        val instant = parseInstant(zuluString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_ZULU_WITH_MILLISECONDS() {
        val zuluString = "\"2021-03-24T12:00:00.500Z\""
        val instant = parseInstant(zuluString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS + 500, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_ZULU_WITH_MAX_MILLISECONDS() {
        val zuluString = "\"2021-03-24T12:00:00.500000000Z\""
        val instant = parseInstant(zuluString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS + 500, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals("\"2021-03-24T12:00:00.500Z\"", formattedString)
    }

    @Test
    fun parse_FROM_INVALID_ZULU_TOO_MANY_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.0100000000Z\""
        assertThrows(JsonDataException::class.java) { parseInstant(offsetString) }
    }

    @Test
    fun parse_FROM_VALID_OFFSET() {
        val offsetString = "\"2021-03-24T12:00:00+02:00\""
        val instant = parseInstant(offsetString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals("\"2021-03-24T10:00:00Z\"", formattedString)
    }

    @Test
    fun parse_FROM_VALID_OFFSET_WITH_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.01+02:00\""
        val instant = parseInstant(offsetString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS + 10, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals("\"2021-03-24T10:00:00.010Z\"", formattedString)
    }

    @Test
    fun parse_FROM_VALID_OFFSET_WITH_MAX_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.010000000+02:00\""
        val instant = parseInstant(offsetString)

        assertNotNull(instant)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS + 10, instant.toEpochMilli())

        val formattedString = json.toJson(instant)
        assertEquals("\"2021-03-24T10:00:00.010Z\"", formattedString)
    }

    @Test
    fun parse_FROM_INVALID_OFFSET_TOO_MANY_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.0100000000+02:00\""
        assertThrows(JsonDataException::class.java) { parseInstant(offsetString) }
    }

    @Test
    fun parse_FROM_INVALID_FORMAT() {
        val zuluString = "\"2021-03-24T12:00:00\""
        assertThrows(JsonDataException::class.java) { parseInstant(zuluString) }
    }

    @Test
    fun parse_FROM_NULL() {
        val zuluString = "null"
        val instant = parseInstant(zuluString)

        assertNull(instant)

        val formattedString = json.toJson<Instant>(null)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_EMPTY_STRING() {
        assertThrows(JsonDataException::class.java) { parseInstant("\"\"") }
        assertThrows(JsonDataException::class.java) { parseInstant("{}") }
    }

    private fun parseInstant(input: String): Instant? {
        return json.fromJson(input)
    }

    companion object {
        private const val BASE_TIME_MILLIS = 1616587200000L
        private const val HOUR_MILLIS = 1000 * 60 * 60L
    }
}

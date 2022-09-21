package mobi.lab.keyimportdemo.infrastructure.common.json

import com.squareup.moshi.JsonDataException
import mobi.lab.keyimportdemo.app.common.toEpochMilli
import org.junit.Assert
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MoshiOffsetDateTimeAdapterTest {

    private val json = MoshiJson(MoshiFactory.get())

    @Test
    fun parse_FROM_VALID_ZULU() {
        val zuluString = "\"2021-03-24T12:00:00Z\""
        val time = parseOffsetDateTime(zuluString)

        assertNotNull(time)
        assertEquals(ZoneOffset.UTC, time.offset)
        assertEquals(BASE_TIME_MILLIS, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_ZULU_WITH_MILLISECONDS() {
        val zuluString = "\"2021-03-24T12:00:00.01Z\""
        val time = parseOffsetDateTime(zuluString)

        assertNotNull(time)
        assertEquals(ZoneOffset.UTC, time.offset)
        assertEquals(BASE_TIME_MILLIS + 10, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_ZULU_WITH_MAX_MILLISECONDS() {
        val zuluString = "\"2021-03-24T12:00:00.010000000Z\""
        val time = parseOffsetDateTime(zuluString)

        assertNotNull(time)
        assertEquals(ZoneOffset.UTC, time.offset)
        assertEquals(BASE_TIME_MILLIS + 10, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals("\"2021-03-24T12:00:00.01Z\"", formattedString)
    }

    @Test
    fun parse_FROM_INVALID_ZULU_TOO_MANY_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.0100000000Z\""
        Assert.assertThrows(JsonDataException::class.java) { parseOffsetDateTime(offsetString) }
    }

    @Test
    fun parse_FROM_VALID_OFFSET() {
        val offsetString = "\"2021-03-24T12:00:00+02:00\""
        val time = parseOffsetDateTime(offsetString)

        assertNotNull(time)
        assertEquals(ZoneOffset.ofHours(2), time.offset)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals(offsetString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_OFFSET_WITH_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.02+02:00\""
        val time = parseOffsetDateTime(offsetString)

        assertNotNull(time)
        assertEquals(ZoneOffset.ofHours(2), time.offset)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS + 20, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals(offsetString, formattedString)
    }

    @Test
    fun parse_FROM_VALID_OFFSET_WITH_MAX_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.020000000+02:00\""
        val time = parseOffsetDateTime(offsetString)

        assertNotNull(time)
        assertEquals(ZoneOffset.ofHours(2), time.offset)
        assertEquals(BASE_TIME_MILLIS - 2 * HOUR_MILLIS + 20, time.toEpochMilli())

        val formattedString = json.toJson(time)
        assertEquals("\"2021-03-24T12:00:00.02+02:00\"", formattedString)
    }

    @Test
    fun parse_FROM_INVALID_OFFSET_TOO_MANY_MILLISECONDS() {
        val offsetString = "\"2021-03-24T12:00:00.0100000000+02:00\""
        Assert.assertThrows(JsonDataException::class.java) { parseOffsetDateTime(offsetString) }
    }

    @Test
    fun parse_FROM_INVALID_FORMAT() {
        val zuluString = "\"2021-03-24T12:00:00\""
        Assert.assertThrows(JsonDataException::class.java) { parseOffsetDateTime(zuluString) }
    }

    @Test
    fun parse_FROM_NULL() {
        val zuluString = "null"
        val time = parseOffsetDateTime(zuluString)

        assertNull(time)

        val formattedString = json.toJson<OffsetDateTime>(null)
        assertEquals(zuluString, formattedString)
    }

    @Test
    fun parse_FROM_EMPTY_STRING() {
        Assert.assertThrows(JsonDataException::class.java) { parseOffsetDateTime("\"\"") }
        Assert.assertThrows(JsonDataException::class.java) { parseOffsetDateTime("{}") }
    }

    private fun parseOffsetDateTime(input: String): OffsetDateTime? {
        return json.fromJson(input)
    }

    companion object {
        private const val BASE_TIME_MILLIS = 1616587200000L
        private const val HOUR_MILLIS = 1000 * 60 * 60L
    }
}

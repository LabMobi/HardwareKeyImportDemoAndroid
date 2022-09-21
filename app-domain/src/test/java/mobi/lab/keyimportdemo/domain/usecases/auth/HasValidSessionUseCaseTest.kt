package mobi.lab.keyimportdemo.domain.usecases.auth

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import mobi.lab.keyimportdemo.domain.entities.DomainException
import mobi.lab.keyimportdemo.domain.entities.ErrorCode
import mobi.lab.keyimportdemo.domain.entities.Session
import mobi.lab.keyimportdemo.domain.storage.SessionStorage
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HasValidSessionUseCaseTest {

    private lateinit var useCase: HasValidSessionUseCase

    private var storage: SessionStorage = mock()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(storage.load()).then { Session("test") }
        useCase = HasValidSessionUseCase(storage)
    }

    @Test
    fun validSession_NULL() {
        whenever(storage.load()).then { null }
        val error = useCase.execute().blockingGet()
        verifyUnauthorizedException(error)
    }

    @Test
    fun validSession_TOKEN_NULL() {
        whenever(storage.load()).then { Session(null) }
        val error = useCase.execute().blockingGet()
        verifyUnauthorizedException(error)
    }

    @Test
    fun validSession_TOKEN_EMPTY_STRING() {
        whenever(storage.load()).then { Session("") }
        val error = useCase.execute().blockingGet()
        verifyUnauthorizedException(error)
    }

    @Test
    fun validSession_TOKEN_EMPTY_SPACE_STRING() {
        whenever(storage.load()).then { Session(" ") }
        val error = useCase.execute().blockingGet()
        // If there's a value, we don't trim it
        assertNull(error)
    }

    @Test
    fun validSession_SUCCESS() {
        whenever(storage.load()).then { Session("token") }
        val error = useCase.execute().blockingGet()
        assertNull(error)
    }

    // blockingGet was removed in RxJava3
    private fun Completable.blockingGet(): Throwable? {
        try {
            this.blockingAwait()
            return null
        } catch (error: Throwable) {
            // blockingAwait wraps exceptions with a RuntimeException
            return error.cause
        }
    }

    private fun verifyUnauthorizedException(error: Throwable?) {
        assertNotNull(error)
        assertTrue(error is DomainException)
        assertEquals(error.code, ErrorCode.LOCAL_UNAUTHORIZED)
    }
}

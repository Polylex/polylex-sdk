package dev.polylex.internal

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class RetryPolicyTest {

    @Test
    fun `succeeds on first attempt without retries`() = runTest {
        val attempts = AtomicInteger(0)
        val result = withRetry {
            attempts.incrementAndGet()
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, attempts.get())
    }

    @Test
    fun `retries on IOException and eventually succeeds`() = runTest {
        val attempts = AtomicInteger(0)
        val result = withRetry(RetryPolicy(maxAttempts = 3, initialDelayMs = 1L, maxDelayMs = 1L)) {
            if (attempts.incrementAndGet() < 3) throw IOException("flaky network")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, attempts.get())
    }

    @Test
    fun `gives up after maxAttempts and throws the last error`() {
        val attempts = AtomicInteger(0)
        val scope = TestScope(StandardTestDispatcher())
        val error = assertThrows(IOException::class.java) {
            scope.runTest {
                withRetry(RetryPolicy(maxAttempts = 3, initialDelayMs = 1L, maxDelayMs = 1L)) {
                    attempts.incrementAndGet()
                    throw IOException("never works")
                }
            }
        }
        assertEquals("never works", error.message)
        assertEquals(3, attempts.get())
    }

    @Test
    fun `non-IO exceptions propagate without retry`() {
        val attempts = AtomicInteger(0)
        val scope = TestScope(StandardTestDispatcher())
        assertThrows(IllegalStateException::class.java) {
            scope.runTest {
                withRetry(RetryPolicy(maxAttempts = 5, initialDelayMs = 1L)) {
                    attempts.incrementAndGet()
                    throw IllegalStateException("not retryable")
                }
            }
        }
        assertEquals("only one attempt expected for non-IO errors", 1, attempts.get())
    }
}

package uk.co.milncj.quick.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RetentionPolicyTest {
    @Test
    fun doneCutoffIsExactlySevenDaysBeforeNow() {
        val now = 20L * RetentionPolicy.MILLIS_PER_DAY
        val expected = 13L * RetentionPolicy.MILLIS_PER_DAY

        assertEquals(expected, RetentionPolicy.doneCutoffMillis(now))
    }
}

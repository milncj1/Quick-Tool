package uk.co.milncj.quick.data

object RetentionPolicy {
    const val DONE_RETENTION_DAYS = 7L
    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun doneCutoffMillis(nowMillis: Long): Long =
        nowMillis - (DONE_RETENTION_DAYS * MILLIS_PER_DAY)
}

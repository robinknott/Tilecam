package no.tilecam.app.data.model

/** A continuous recorded clip found on the camera's SD card / NVR. */
data class RecordingSegment(
    val startMillis: Long,
    val endMillis: Long,
) {
    val durationMs: Long get() = (endMillis - startMillis).coerceAtLeast(0)
}

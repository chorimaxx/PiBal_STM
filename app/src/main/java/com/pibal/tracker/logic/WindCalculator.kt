package com.pibal.tracker.logic

import kotlin.math.*

/**
 * Data class representing a measurement point in time.
 */
data class BalloonPosition(
    val timeSeconds: Int,
    val azimuthDegrees: Float,
    val elevationDegrees: Float,
    val heightMeters: Float,
    val xMeters: Float,
    val yMeters: Float
)

/**
 * Result of a wind calculation between two points.
 */
data class WindResult(
    val heightMeters: Float,
    val windSpeed: Float, // m/s
    val windDirection: Float // Degrees (coming from)
)

class WindCalculator {
    companion object {
        const val ASCENT_RATE_PER_30S = 50f
        const val MEASUREMENT_INTERVAL_S = 30
    }

    /**
     * Converts spherical coordinates (Azimuth, Elevation) to Cartesian (X, Y)
     * assuming a certain height based on time.
     */
    fun calculatePosition(timeSeconds: Int, azimuthDeg: Float, elevationDeg: Float): BalloonPosition {
        val height = (timeSeconds.toFloat() / MEASUREMENT_INTERVAL_S) * ASCENT_RATE_PER_30S
        val azRad = Math.toRadians(azimuthDeg.toDouble()).toFloat()
        val elRad = Math.toRadians(elevationDeg.toDouble()).toFloat()

        // Horizontal distance D = Z / tan(elevation)
        val horizontalDistance = if (elevationDeg > 0.1f) {
            height / tan(elRad)
        } else {
            0f
        }

        // X = D * sin(Az), Y = D * cos(Az) (Y is North, X is East)
        val x = horizontalDistance * sin(azRad)
        val y = horizontalDistance * cos(azRad)

        return BalloonPosition(timeSeconds, azimuthDeg, elevationDeg, height, x, y)
    }

    /**
     * Calculates wind speed and direction between two positions.
     */
    fun calculateWind(p1: BalloonPosition, p2: BalloonPosition): WindResult {
        val dx = p2.xMeters - p1.xMeters
        val dy = p2.yMeters - p1.yMeters
        val dt = (p2.timeSeconds - p1.timeSeconds).toFloat()

        val distance = sqrt(dx * dx + dy * dy)
        val speed = if (dt > 0) distance / dt else 0f

        // Math.atan2(y, x) -> atan2(dx, dy) for North-relative clockwise azimuth
        val angleToRad = atan2(dx.toDouble(), dy.toDouble()).toFloat()
        val angleToDeg = Math.toDegrees(angleToRad.toDouble()).toFloat()
        
        // Normalize to 0-360
        val dirTo = (angleToDeg + 360f) % 360f
        
        // Meteorology: Direction "from" (opposite of movement)
        val dirFrom = (dirTo + 180f) % 360f

        return WindResult(p2.heightMeters, speed, dirFrom)
    }
}

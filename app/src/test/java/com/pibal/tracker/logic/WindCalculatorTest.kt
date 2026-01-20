package com.pibal.tracker.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class WindCalculatorTest {
    private val calculator = WindCalculator()

    @Test
    fun testPositionCalculation() {
        // T=30s, Az=0 (North), El=45 (tan=1)
        // Height should be 50m
        // Horizontal distance should be 50 / tan(45) = 50m
        // X = 50 * sin(0) = 0
        // Y = 50 * cos(0) = 50
        val pos = calculator.calculatePosition(30, 0f, 45f)
        
        assertEquals(50f, pos.heightMeters, 0.1f)
        assertEquals(0f, pos.xMeters, 0.1f)
        assertEquals(50f, pos.yMeters, 0.1f)
    }

    @Test
    fun testWindCalculation() {
        val p1 = BalloonPosition(30, 0f, 45f, 50f, 0f, 50f)
        // T=60s, Az=90 (East), El=45 (tan=1)
        // Height = 100m
        // Horizontal distance = 100m
        // X = 100, Y = 0
        val p2 = BalloonPosition(60, 90f, 45f, 100f, 100f, 0f)
        
        val result = calculator.calculateWind(p1, p2)
        
        // dx = 100 - 0 = 100
        // dy = 0 - 50 = -50
        // distance = sqrt(100^2 + 50^2) = 111.8
        // speed = 111.8 / 30 = 3.73
        assertEquals(3.73f, result.windSpeed, 0.1f)
        
        // dirTo = atan2(100, -50) = 116.5 degrees (SSE)
        // dirFrom = 116.5 + 180 = 296.5 degrees (WNW)
        assertEquals(296.5f, result.windDirection, 1.0f)
    }
}

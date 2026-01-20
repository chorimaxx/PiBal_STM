package com.pibal.tracker.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class OrientationData(
    val azimuth: Float = 0f,
    val elevation: Float = 0f,
    val roll: Float = 0f
)

class SensorRepository(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(OrientationData())
    val orientation: StateFlow<OrientationData> = _orientation

    // EMA Filter coefficient
    private var alpha = 0.15f
    private var lastAzimuth = 0f
    private var lastElevation = 0f

    fun start() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Remap for Back-facing (Device vertical, camera facing forward)
            // X stays X, Z becomes Y (forward)
            val remappedMatrix = FloatArray(9)
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )

            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(remappedMatrix, orientationValues)

            // Convert to degrees and apply EMA filter
            val rawAzimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            val rawElevation = -Math.toDegrees(orientationValues[1].toDouble()).toFloat() // Invert for "up is positive"

            val filteredAzimuth = applyEma(rawAzimuth, lastAzimuth)
            val filteredElevation = applyEma(rawElevation, lastElevation)

            lastAzimuth = filteredAzimuth
            lastElevation = filteredElevation

            _orientation.value = OrientationData(
                azimuth = (filteredAzimuth + 360f) % 360f,
                elevation = filteredElevation,
                roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
            )
        }
    }

    private fun applyEma(current: Float, last: Float): Float {
        // Handle angle wrap-around for azimuth
        var diff = current - last
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return (last + alpha * diff + 360) % 360
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

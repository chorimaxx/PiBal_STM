package com.pibal.tracker.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pibal.tracker.location.LocationProvider
import com.pibal.tracker.logic.BalloonPosition
import com.pibal.tracker.logic.PdfReportGenerator
import com.pibal.tracker.logic.WindCalculator
import com.pibal.tracker.logic.WindResult
import com.pibal.tracker.sensor.OrientationData
import com.pibal.tracker.sensor.SensorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val sensorRepository = SensorRepository(application)
    private val locationProvider = LocationProvider(application)
    private val windCalculator = WindCalculator()
    private val pdfReportGenerator = PdfReportGenerator(application)
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(VibratorManager::class.java)!!
        vibratorManager.defaultVibrator
    } else {
        application.getSystemService(Vibrator::class.java)!!
    }

    private var tts: TextToSpeech? = TextToSpeech(application, this)

    private val _orientation = MutableStateFlow(OrientationData())
    val orientation: StateFlow<OrientationData> = _orientation

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    private val _measurementPoints = MutableStateFlow<List<BalloonPosition>>(emptyList())
    val measurementPoints: StateFlow<List<BalloonPosition>> = _measurementPoints

    private val _windResults = MutableStateFlow<List<WindResult>>(emptyList())
    val windResults: StateFlow<List<WindResult>> = _windResults

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _sharePdfEvent = MutableSharedFlow<String>()
    val sharePdfEvent: SharedFlow<String> = _sharePdfEvent.asSharedFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            sensorRepository.orientation.collectLatest {
                _orientation.value = it
            }
        }
    }

    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        _measurementPoints.value = emptyList()
        _windResults.value = emptyList()
        _timerSeconds.value = 0
        
        locationProvider.updateLocationAndDeclination()
        sensorRepository.start()
        
        timerJob = viewModelScope.launch {
            while (_isTracking.value) {
                // T-5s warning
                if (_timerSeconds.value % 30 == 25) {
                    vibrate(short = true)
                }
                
                // T-0s mark
                if (_timerSeconds.value > 0 && _timerSeconds.value % 30 == 0) {
                    recordPoint()
                }
                
                delay(1000)
                _timerSeconds.value += 1
            }
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) return
        _isTracking.value = false
        timerJob?.cancel()
        sensorRepository.stop()
        tts?.stop()

        // Generate PDF report
        val results = _windResults.value
        if (results.isNotEmpty()) {
            val sortedResults = results.sortedBy { it.heightMeters }
            val filePath = pdfReportGenerator.generateReport(sortedResults)
            if (filePath != null) {
                viewModelScope.launch {
                    _toastMessage.emit("PDF Report generated: ${filePath.substringAfterLast("/")}")
                    _sharePdfEvent.emit(filePath)
                }
            }
        }
    }

    private fun recordPoint() {
        val currentOrientation = _orientation.value
        val declination = locationProvider.declination.value
        
        // Correct magnetic azimuth to true north
        val trueAzimuth = (currentOrientation.azimuth + declination + 360f) % 360f
        
        val newPoint = windCalculator.calculatePosition(
            _timerSeconds.value,
            trueAzimuth,
            currentOrientation.elevation
        )
        
        val updatedPoints = _measurementPoints.value + newPoint
        _measurementPoints.value = updatedPoints
        
        vibrate(short = false)
        
        if (updatedPoints.size >= 2) {
            val p1 = updatedPoints[updatedPoints.size - 2]
            val p2 = updatedPoints.last()
            val result = windCalculator.calculateWind(p1, p2)
            _windResults.value = _windResults.value + result
            
            speak("Height ${result.heightMeters.toInt()} meters. Speed ${String.format(Locale.US, "%.1f", result.windSpeed)} meters per second. Direction ${result.windDirection.toInt()} degrees.")
        } else {
            speak("Initial point recorded.")
        }
    }

    private fun vibrate(short: Boolean) {
        if (short) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
        tts?.shutdown()
    }
}

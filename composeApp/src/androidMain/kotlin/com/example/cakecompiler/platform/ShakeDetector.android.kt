package com.example.cakecompiler.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.sqrt

/**
 * Android用シェイク検出実装
 */
class AndroidShakeDetector(private val context: Context) : ShakeDetector, SensorEventListener {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val accelerometer: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val _shakeEvents = MutableSharedFlow<ShakeEvent>()
    override val shakeEvents: Flow<ShakeEvent> = _shakeEvents

    private var sensitivity = ShakeSensitivity.MEDIUM
    private var lastShakeTime = 0L
    private var shakeStartTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate = 0L

    // 感度に応じた閾値
    private val shakeThreshold: Float
        get() = when (sensitivity) {
            ShakeSensitivity.LOW -> 15f
            ShakeSensitivity.MEDIUM -> 12f
            ShakeSensitivity.HIGH -> 8f
        }

    private val shakeCooldownMs = 500L
    private val shakeMinDurationMs = 300L

    override fun startDetection() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun stopDetection() {
        sensorManager.unregisterListener(this)
    }

    override fun setSensitivity(sensitivity: ShakeSensitivity) {
        this.sensitivity = sensitivity
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastUpdate > 100) {
                    val diffTime = currentTime - lastUpdate
                    lastUpdate = currentTime

                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]

                    val speed = sqrt(
                        ((x - lastX) * (x - lastX) +
                                (y - lastY) * (y - lastY) +
                                (z - lastZ) * (z - lastZ)).toDouble()
                    ) / diffTime * 10000

                    if (speed > shakeThreshold) {
                        if (shakeStartTime == 0L) {
                            shakeStartTime = currentTime
                        }

                        val shakeDuration = currentTime - shakeStartTime

                        if (shakeDuration >= shakeMinDurationMs &&
                            currentTime - lastShakeTime > shakeCooldownMs) {

                            lastShakeTime = currentTime

                            val intensity = (speed / 30.0).coerceIn(0.0, 1.0).toFloat()

                            val shakeEvent = ShakeEvent(
                                timestamp = currentTime,
                                intensity = intensity,
                                durationMs = shakeDuration
                            )

                            // コルーチンスコープが必要だが、tryEmitを使用
                            _shakeEvents.tryEmit(shakeEvent)
                        }
                    } else {
                        // シェイクが止まった
                        shakeStartTime = 0L
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不要
    }
}

// Context依存のため、ファクトリは呼び出し側で実装
actual fun createShakeDetector(): ShakeDetector = MockShakeDetector()

package com.example.cakecompiler.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * iOS用シェイク検出実装
 * CoreMotionを使用
 */
class IOSShakeDetector : ShakeDetector {

    private val motionManager = CMMotionManager()

    private val _shakeEvents = MutableSharedFlow<ShakeEvent>()
    override val shakeEvents: Flow<ShakeEvent> = _shakeEvents

    private var sensitivity = ShakeSensitivity.MEDIUM
    private var lastShakeTime = 0L
    private var shakeStartTime = 0L

    // 感度に応じた閾値
    private val shakeThreshold: Double
        get() = when (sensitivity) {
            ShakeSensitivity.LOW -> 2.5
            ShakeSensitivity.MEDIUM -> 2.0
            ShakeSensitivity.HIGH -> 1.5
        }

    private val shakeCooldownMs = 500L
    private val shakeMinDurationMs = 300L

    override fun startDetection() {
        if (motionManager.accelerometerAvailable) {
            motionManager.accelerometerUpdateInterval = 0.1 // 100ms

            motionManager.startAccelerometerUpdatesToQueue(
                NSOperationQueue.mainQueue
            ) { data, error ->
                if (error == null && data != null) {
                    val acceleration = data.acceleration
                    val totalAcceleration = kotlin.math.sqrt(
                        acceleration.x * acceleration.x +
                                acceleration.y * acceleration.y +
                                acceleration.z * acceleration.z
                    )

                    val currentTime = platform.Foundation.NSDate().timeIntervalSince1970.toLong() * 1000

                    if (totalAcceleration > shakeThreshold) {
                        if (shakeStartTime == 0L) {
                            shakeStartTime = currentTime
                        }

                        val shakeDuration = currentTime - shakeStartTime

                        if (shakeDuration >= shakeMinDurationMs &&
                            currentTime - lastShakeTime > shakeCooldownMs) {

                            lastShakeTime = currentTime

                            val intensity = ((totalAcceleration - 1.0) / 3.0)
                                .coerceIn(0.0, 1.0).toFloat()

                            val shakeEvent = ShakeEvent(
                                timestamp = currentTime,
                                intensity = intensity,
                                durationMs = shakeDuration
                            )

                            _shakeEvents.tryEmit(shakeEvent)
                        }
                    } else {
                        shakeStartTime = 0L
                    }
                }
            }
        }
    }

    override fun stopDetection() {
        motionManager.stopAccelerometerUpdates()
    }

    override fun setSensitivity(sensitivity: ShakeSensitivity) {
        this.sensitivity = sensitivity
    }
}

actual fun createShakeDetector(): ShakeDetector = IOSShakeDetector()

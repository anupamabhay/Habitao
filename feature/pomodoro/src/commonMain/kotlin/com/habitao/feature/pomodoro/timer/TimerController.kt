package com.habitao.feature.pomodoro.timer

interface TimerController {
    fun start()
    fun pause()
    fun resume()
    fun stop()
    fun skip()
    fun adjustTime(deltaSeconds: Long)
}

package com.habitao.feature.pomodoro.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.feature.pomodoro.service.TimerService

class AndroidTimerController(private val context: Context) : TimerController {
    override fun start() = sendServiceAction(TimerService.ACTION_START)
    override fun pause() = sendServiceAction(TimerService.ACTION_PAUSE)
    override fun resume() = sendServiceAction(TimerService.ACTION_RESUME)
    override fun stop() = sendServiceAction(TimerService.ACTION_STOP)
    override fun skip() = sendServiceAction(TimerService.ACTION_SKIP)

    override fun adjustTime(deltaSeconds: Long) {
        val intent =
            Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_ADJUST_TIME
                putExtra(TimerService.EXTRA_DELTA_SECONDS, deltaSeconds)
            }
        context.startService(intent)
    }

    private fun sendServiceAction(action: String) {
        val intent =
            Intent(context, TimerService::class.java).apply {
                this.action = action
            }
        if (action == TimerService.ACTION_START && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

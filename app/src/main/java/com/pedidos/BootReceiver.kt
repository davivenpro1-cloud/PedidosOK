package com.pedidos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Las alarmas de AlarmManager se borran cuando el teléfono se reinicia.
 * Este receptor las vuelve a programar todas automáticamente al arrancar.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.reprogramarTodos(context)
        }
    }
}

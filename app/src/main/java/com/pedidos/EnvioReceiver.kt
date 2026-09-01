package com.pedidos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Se dispara cuando llega la hora de un mensaje programado.
 * Abre WhatsApp con el mensaje ya escrito (el servicio de accesibilidad,
 * si está activado, toca el botón de enviar automáticamente).
 *
 * Si el mensaje era SEMANAL, vuelve a programar la alarma para la semana siguiente.
 */
class EnvioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val mensajeTexto = intent.getStringExtra("mensaje") ?: return
        val tipo = intent.getStringExtra("tipo") ?: "UNICO"

        val telefono = Prefs.getTelefono(context)
        if (telefono.isNotBlank()) {
            EnvioAccessibilityService.prepararEnvioAutomatico()
            WhatsappUtil.abrirConMensaje(context, telefono, mensajeTexto)
            mostrarNotificacion(context, mensajeTexto)
        }

        if (tipo == "SEMANAL") {
            val diaSemana = intent.getIntExtra("diaSemana", -1)
            if (diaSemana != -1) {
                val programado = Prefs.getProgramados(context).find { it.id == id }
                if (programado != null) {
                    AlarmScheduler.programarSemanal(context, programado, diaSemana)
                }
            }
        }
    }

    private fun mostrarNotificacion(context: Context, mensaje: String) {
        val canalId = "pedidos_programados"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId, "Pedidos programados", NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(canal)
        }

        val notificacion = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Pedido enviado por WhatsApp")
            .setContentText(mensaje.take(80))
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(System.currentTimeMillis().toInt(), notificacion)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: no pasa nada, el mensaje se envía igualmente.
        }
    }
}

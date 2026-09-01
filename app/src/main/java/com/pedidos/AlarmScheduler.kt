package com.pedidos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Programa las alarmas exactas (AlarmManager) que dispararán EnvioReceiver
 * en el momento indicado, aunque el teléfono esté en reposo o la app cerrada.
 *
 * Para los mensajes SEMANALES, cada día de la semana se programa como una
 * alarma individual. Cuando se dispara, EnvioReceiver vuelve a llamar aquí
 * para programar la misma alarma la semana siguiente.
 */
object AlarmScheduler {

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Programa (o reprograma) todas las alarmas de un mensaje. */
    fun programar(context: Context, mensaje: MensajeProgramado) {
        if (mensaje.tipo == "UNICO") {
            programarUnico(context, mensaje)
        } else {
            mensaje.diasSemana.forEach { diaSemana ->
                programarSemanal(context, mensaje, diaSemana)
            }
        }
    }

    /** Cancela todas las alarmas asociadas a un mensaje (al eliminarlo). */
    fun cancelar(context: Context, mensaje: MensajeProgramado) {
        if (mensaje.tipo == "UNICO") {
            cancelarPendingIntent(context, requestCode(mensaje.id, 0))
        } else {
            mensaje.diasSemana.forEach { diaSemana ->
                cancelarPendingIntent(context, requestCode(mensaje.id, diaSemana))
            }
        }
    }

    private fun programarUnico(context: Context, mensaje: MensajeProgramado) {
        val fecha = mensaje.fechaMillis ?: return
        val cal = Calendar.getInstance().apply {
            timeInMillis = fecha
            set(Calendar.HOUR_OF_DAY, mensaje.hora)
            set(Calendar.MINUTE, mensaje.minuto)
            set(Calendar.SECOND, 0)
        }
        programarAlarma(context, mensaje, requestCode(mensaje.id, 0), cal.timeInMillis, diaSemana = null)
    }

    /** Calcula la próxima aparición de [diaSemana] (Calendar.MONDAY, etc.) a la hora indicada. */
    fun programarSemanal(context: Context, mensaje: MensajeProgramado, diaSemana: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, mensaje.hora)
            set(Calendar.MINUTE, mensaje.minuto)
            set(Calendar.SECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != diaSemana || cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        programarAlarma(context, mensaje, requestCode(mensaje.id, diaSemana), cal.timeInMillis, diaSemana)
    }

    private fun programarAlarma(
        context: Context,
        mensaje: MensajeProgramado,
        requestCode: Int,
        momentoMillis: Long,
        diaSemana: Int?
    ) {
        val intent = Intent(context, EnvioReceiver::class.java).apply {
            putExtra("id", mensaje.id)
            putExtra("mensaje", mensaje.mensaje)
            putExtra("tipo", mensaje.tipo)
            if (diaSemana != null) putExtra("diaSemana", diaSemana)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = alarmManager(context)
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momentoMillis, pendingIntent)
        } catch (e: SecurityException) {
            // Si no se concedió el permiso de alarmas exactas, usamos una alarma aproximada
            manager.set(AlarmManager.RTC_WAKEUP, momentoMillis, pendingIntent)
        }
    }

    private fun cancelarPendingIntent(context: Context, requestCode: Int) {
        val intent = Intent(context, EnvioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pendingIntent)
    }

    /** Reprograma todos los mensajes guardados (se usa tras reiniciar el teléfono). */
    fun reprogramarTodos(context: Context) {
        Prefs.getProgramados(context).forEach { programar(context, it) }
    }

    private fun requestCode(id: String, diaSemana: Int): Int {
        return (id + "_" + diaSemana).hashCode()
    }
}

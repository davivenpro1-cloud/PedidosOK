package com.pedidos

import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/**
 * Un mensaje programado puede ser:
 * - UNICO: se envía una sola vez, en una fecha y hora concretas.
 * - SEMANAL: se envía cada semana, en uno o varios días de la semana, a una hora fija.
 *
 * diasSemana usa las constantes de Calendar (Calendar.MONDAY, Calendar.FRIDAY, etc.)
 */
data class MensajeProgramado(
    val id: String = UUID.randomUUID().toString(),
    val mensaje: String,
    val tipo: String, // "UNICO" o "SEMANAL"
    val fechaMillis: Long? = null, // solo para UNICO
    val diasSemana: Set<Int> = emptySet(), // solo para SEMANAL
    val hora: Int,
    val minuto: Int
) {

    fun resumen(): String {
        return if (tipo == "UNICO") {
            val cal = Calendar.getInstance().apply { timeInMillis = fechaMillis ?: 0L }
            val dia = cal.get(Calendar.DAY_OF_MONTH)
            val mes = cal.get(Calendar.MONTH) + 1
            val anio = cal.get(Calendar.YEAR)
            "%02d/%02d/%d a las %02d:%02d".format(dia, mes, anio, hora, minuto)
        } else {
            val nombres = diasSemana.sorted().joinToString(", ") { nombreDia(it) }
            "$nombres a las %02d:%02d".format(hora, minuto)
        }
    }

    companion object {
        fun nombreDia(diaCalendar: Int): String = when (diaCalendar) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "?"
        }

        fun desdeJson(obj: JSONObject): MensajeProgramado {
            val diasArray = obj.optJSONArray("diasSemana")
            val dias = mutableSetOf<Int>()
            if (diasArray != null) {
                for (i in 0 until diasArray.length()) dias.add(diasArray.getInt(i))
            }
            return MensajeProgramado(
                id = obj.getString("id"),
                mensaje = obj.getString("mensaje"),
                tipo = obj.getString("tipo"),
                fechaMillis = if (obj.has("fechaMillis") && !obj.isNull("fechaMillis")) obj.getLong("fechaMillis") else null,
                diasSemana = dias,
                hora = obj.getInt("hora"),
                minuto = obj.getInt("minuto")
            )
        }
    }

    fun aJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("mensaje", mensaje)
        obj.put("tipo", tipo)
        if (fechaMillis != null) obj.put("fechaMillis", fechaMillis)
        val diasArray = org.json.JSONArray()
        diasSemana.forEach { diasArray.put(it) }
        obj.put("diasSemana", diasArray)
        obj.put("hora", hora)
        obj.put("minuto", minuto)
        return obj
    }
}

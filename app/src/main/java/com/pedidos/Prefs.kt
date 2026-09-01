package com.pedidos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda de forma sencilla (SharedPreferences):
 * - El número de WhatsApp al que se envían los pedidos.
 * - La lista de mensajes programados (como JSON).
 */
object Prefs {

    private const val NOMBRE_PREFS = "pedidos_prefs"
    private const val CLAVE_TELEFONO = "telefono"
    private const val CLAVE_PROGRAMADOS = "mensajes_programados"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NOMBRE_PREFS, Context.MODE_PRIVATE)

    fun getTelefono(context: Context): String {
        return prefs(context).getString(CLAVE_TELEFONO, "") ?: ""
    }

    fun setTelefono(context: Context, telefono: String) {
        prefs(context).edit().putString(CLAVE_TELEFONO, telefono).apply()
    }

    fun getProgramados(context: Context): MutableList<MensajeProgramado> {
        val json = prefs(context).getString(CLAVE_PROGRAMADOS, "[]") ?: "[]"
        val lista = mutableListOf<MensajeProgramado>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            lista.add(MensajeProgramado.desdeJson(array.getJSONObject(i)))
        }
        return lista
    }

    fun guardarProgramados(context: Context, lista: List<MensajeProgramado>) {
        val array = JSONArray()
        lista.forEach { array.put(it.aJson()) }
        prefs(context).edit().putString(CLAVE_PROGRAMADOS, array.toString()).apply()
    }

    fun agregarProgramado(context: Context, mensaje: MensajeProgramado) {
        val lista = getProgramados(context)
        lista.add(mensaje)
        guardarProgramados(context, lista)
    }

    fun eliminarProgramado(context: Context, id: String) {
        val lista = getProgramados(context)
        lista.removeAll { it.id == id }
        guardarProgramados(context, lista)
    }
}

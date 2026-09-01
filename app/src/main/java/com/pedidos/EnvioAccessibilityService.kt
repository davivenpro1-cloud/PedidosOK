package com.pedidos

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Servicio de accesibilidad opcional: si el usuario lo activa en Ajustes,
 * cuando se abre WhatsApp con un mensaje programado ya escrito, este servicio
 * busca el botón de "Enviar" y lo pulsa solo, para que el pedido se mande
 * sin que nadie tenga que tocar el teléfono.
 *
 * Si no se activa, la app funciona igual mostrando el mensaje ya escrito en
 * WhatsApp, pero hay que tocar el botón de enviar manualmente.
 */
class EnvioAccessibilityService : AccessibilityService() {

    companion object {
        // Cuando un mensaje programado abre WhatsApp, se marca esta bandera
        // para que el servicio sepa que debe intentar pulsar "enviar".
        @Volatile
        private var esperandoEnvioAutomatico = false

        fun prepararEnvioAutomatico() {
            esperandoEnvioAutomatico = true
            // Si por lo que sea no encuentra el botón, dejamos de intentarlo pasado un tiempo.
            Handler(Looper.getMainLooper()).postDelayed({
                esperandoEnvioAutomatico = false
            }, 15000)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!esperandoEnvioAutomatico) return
        if (event?.packageName != "com.whatsapp") return

        val raiz = rootInActiveWindow ?: return
        val botonEnviar = buscarBotonEnviar(raiz)
        if (botonEnviar != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                botonEnviar.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                esperandoEnvioAutomatico = false
            }, 700)
        }
    }

    private fun buscarBotonEnviar(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val porId = nodo.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (porId.isNotEmpty()) return porId[0]

        return buscarPorDescripcion(nodo)
    }

    private fun buscarPorDescripcion(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val descripcion = nodo.contentDescription?.toString()?.lowercase()
        if (nodo.isClickable && descripcion != null &&
            (descripcion.contains("enviar") || descripcion.contains("send"))
        ) {
            return nodo
        }
        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            val encontrado = buscarPorDescripcion(hijo)
            if (encontrado != null) return encontrado
        }
        return null
    }

    override fun onInterrupt() {}
}

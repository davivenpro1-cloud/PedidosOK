package com.pedidos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilidad compartida para abrir WhatsApp con un mensaje ya escrito,
 * usada tanto al dictar un pedido a mano como al enviar un mensaje programado.
 */
object WhatsappUtil {

    fun abrirConMensaje(context: Context, telefono: String, mensaje: String) {
        val texto = Uri.encode(mensaje)
        val uri = Uri.parse("https://wa.me/$telefono?text=$texto")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Reintento sin forzar el paquete, por si acaso
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_LONG).show()
            }
        }
    }
}

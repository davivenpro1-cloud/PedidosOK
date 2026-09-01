package com.pedidos

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedidos.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Ajustes"

        binding.campoTelefono.setText(Prefs.getTelefono(this))

        binding.botonGuardarTelefono.setOnClickListener {
            val telefono = binding.campoTelefono.text.toString().trim()
            if (telefono.isBlank()) {
                Toast.makeText(this, "Escribe un número de teléfono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.setTelefono(this, telefono)
            Toast.makeText(this, "Número guardado", Toast.LENGTH_SHORT).show()
        }

        binding.botonAccesibilidad.setOnClickListener {
            Toast.makeText(
                this,
                "Busca \"Pedidos Al Andalus\" en la lista y actívalo",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}

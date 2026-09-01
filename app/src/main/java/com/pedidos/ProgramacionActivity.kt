package com.pedidos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pedidos.databinding.ActivityProgramacionBinding
import java.util.Calendar

/**
 * Pantalla "Programación": permite dejar mensajes (pedidos fijos) preparados
 * para que se envíen solos, bien un día y hora concretos, o cada semana en
 * uno o varios días (por ejemplo, todos los lunes y viernes a las 8).
 * También muestra la lista de lo ya programado, con opción de quitarlo.
 *
 * El número al que se envían es el mismo que se guarda en Ajustes.
 */
class ProgramacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgramacionBinding
    private lateinit var adaptador: ProgramacionAdapter

    private var fechaElegidaMillis: Long? = null
    private var horaUnico: Int? = null
    private var minutoUnico: Int? = null
    private var horaSemanal: Int? = null
    private var minutoSemanal: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgramacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Programación"

        adaptador = ProgramacionAdapter(mutableListOf()) { mensaje ->
            AlarmScheduler.cancelar(this, mensaje)
            Prefs.eliminarProgramado(this, mensaje.id)
            cargarLista()
            Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show()
        }
        binding.listaProgramados.layoutManager = LinearLayoutManager(this)
        binding.listaProgramados.adapter = adaptador

        binding.grupoTipo.setOnCheckedChangeListener { _, checkedId ->
            val esFechaConcreta = checkedId == binding.opcionFechaConcreta.id
            binding.bloqueFechaConcreta.visibility = if (esFechaConcreta) android.view.View.VISIBLE else android.view.View.GONE
            binding.bloqueSemanal.visibility = if (esFechaConcreta) android.view.View.GONE else android.view.View.VISIBLE
        }

        binding.botonElegirFecha.setOnClickListener { elegirFecha() }
        binding.botonElegirHoraUnico.setOnClickListener { elegirHora(esSemanal = false) }
        binding.botonElegirHoraSemanal.setOnClickListener { elegirHora(esSemanal = true) }

        binding.botonProgramar.setOnClickListener { intentarProgramar() }

        cargarLista()
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.getTelefono(this).isBlank()) {
            Toast.makeText(
                this,
                "Antes de programar mensajes, guarda el número de WhatsApp en Ajustes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun elegirFecha() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val elegido = Calendar.getInstance().apply { set(anio, mes, dia, 0, 0, 0) }
                fechaElegidaMillis = elegido.timeInMillis
                actualizarTextoFecha()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun elegirHora(esSemanal: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hora, minuto ->
                if (esSemanal) {
                    horaSemanal = hora
                    minutoSemanal = minuto
                    binding.textoHoraSemanal.text = "%02d:%02d".format(hora, minuto)
                } else {
                    horaUnico = hora
                    minutoUnico = minuto
                    actualizarTextoFecha()
                }
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show()
    }

    private fun actualizarTextoFecha() {
        val fecha = fechaElegidaMillis
        val cal = Calendar.getInstance()
        val texto = StringBuilder()
        if (fecha != null) {
            cal.timeInMillis = fecha
            texto.append("%02d/%02d/%d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)))
        } else {
            texto.append("Sin fecha")
        }
        if (horaUnico != null) {
            texto.append(" a las %02d:%02d".format(horaUnico, minutoUnico))
        }
        binding.textoFechaElegida.text = texto.toString()
    }

    private fun intentarProgramar() {
        if (Prefs.getTelefono(this).isBlank()) {
            Toast.makeText(this, "Primero guarda el número de WhatsApp en Ajustes", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        val mensajeTexto = binding.campoMensaje.text.toString().trim()
        if (mensajeTexto.isBlank()) {
            Toast.makeText(this, "Escribe el mensaje a enviar", Toast.LENGTH_SHORT).show()
            return
        }

        val esFechaConcreta = binding.grupoTipo.checkedRadioButtonId == binding.opcionFechaConcreta.id

        val nuevoMensaje: MensajeProgramado

        if (esFechaConcreta) {
            val fecha = fechaElegidaMillis
            val hora = horaUnico
            val minuto = minutoUnico
            if (fecha == null || hora == null || minuto == null) {
                Toast.makeText(this, "Elige fecha y hora", Toast.LENGTH_SHORT).show()
                return
            }
            nuevoMensaje = MensajeProgramado(
                mensaje = mensajeTexto,
                tipo = "UNICO",
                fechaMillis = fecha,
                hora = hora,
                minuto = minuto
            )
        } else {
            val dias = diasSeleccionados()
            val hora = horaSemanal
            val minuto = minutoSemanal
            if (dias.isEmpty()) {
                Toast.makeText(this, "Elige al menos un día de la semana", Toast.LENGTH_SHORT).show()
                return
            }
            if (hora == null || minuto == null) {
                Toast.makeText(this, "Elige la hora", Toast.LENGTH_SHORT).show()
                return
            }
            nuevoMensaje = MensajeProgramado(
                mensaje = mensajeTexto,
                tipo = "SEMANAL",
                diasSemana = dias,
                hora = hora,
                minuto = minuto
            )
        }

        Prefs.agregarProgramado(this, nuevoMensaje)
        AlarmScheduler.programar(this, nuevoMensaje)

        limpiarFormulario()
        cargarLista()
        Toast.makeText(this, "Mensaje programado", Toast.LENGTH_SHORT).show()
    }

    private fun diasSeleccionados(): Set<Int> {
        val mapa = listOf(
            binding.diaLun to Calendar.MONDAY,
            binding.diaMar to Calendar.TUESDAY,
            binding.diaMie to Calendar.WEDNESDAY,
            binding.diaJue to Calendar.THURSDAY,
            binding.diaVie to Calendar.FRIDAY,
            binding.diaSab to Calendar.SATURDAY,
            binding.diaDom to Calendar.SUNDAY
        )
        return mapa.filter { (checkbox, _) -> (checkbox as CheckBox).isChecked }
            .map { it.second }
            .toSet()
    }

    private fun limpiarFormulario() {
        binding.campoMensaje.setText("")
        fechaElegidaMillis = null
        horaUnico = null
        minutoUnico = null
        horaSemanal = null
        minutoSemanal = null
        binding.textoFechaElegida.text = "Sin fecha seleccionada"
        binding.textoHoraSemanal.text = "Sin hora seleccionada"
        listOf(
            binding.diaLun, binding.diaMar, binding.diaMie, binding.diaJue,
            binding.diaVie, binding.diaSab, binding.diaDom
        ).forEach { it.isChecked = false }
    }

    private fun cargarLista() {
        val lista = Prefs.getProgramados(this)
        adaptador.actualizar(lista)
        binding.textoListaVacia.visibility = if (lista.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}

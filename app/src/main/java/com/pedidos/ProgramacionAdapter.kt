package com.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pedidos.databinding.ItemProgramadoBinding

class ProgramacionAdapter(
    private val items: MutableList<MensajeProgramado>,
    private val onEliminar: (MensajeProgramado) -> Unit
) : RecyclerView.Adapter<ProgramacionAdapter.VistaItem>() {

    inner class VistaItem(val binding: ItemProgramadoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VistaItem {
        val binding = ItemProgramadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VistaItem(binding)
    }

    override fun onBindViewHolder(holder: VistaItem, position: Int) {
        val item = items[position]
        holder.binding.textoResumen.text = item.resumen()
        holder.binding.textoMensaje.text = item.mensaje
        holder.binding.botonEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount(): Int = items.size

    fun actualizar(nuevaLista: List<MensajeProgramado>) {
        items.clear()
        items.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}

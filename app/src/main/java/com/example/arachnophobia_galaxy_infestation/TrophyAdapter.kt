package com.example.arachnophobia_galaxy_infestation

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TrophyAdapter(
    private val trophies: List<Trophy>,
    private val userTrophies: Set<String>, // earned trophies
    private val onItemClick: (Trophy) -> Unit
) : RecyclerView.Adapter<TrophyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrophyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trophy, parent, false)
        return TrophyViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrophyViewHolder, position: Int) {
        val trophy = trophies[position]
        val isEarned = userTrophies.contains(trophy.id) // check by ID
        holder.bind(trophy, isEarned, onItemClick)
        Log.d("TrophyAdapter", "Binding ${trophy.id}, earned=${isEarned}")

    }

    override fun getItemCount(): Int = trophies.size
}
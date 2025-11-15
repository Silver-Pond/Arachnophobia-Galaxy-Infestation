package com.example.arachnophobia_galaxy_infestation

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TrophyAdapter(
    private val trophies: List<Trophy>,
    private val userTrophies: Set<String>, // earned trophies (Android Developers, 2025; ChatGPT-4, 2025)
    private val onItemClick: (Trophy) -> Unit
) : RecyclerView.Adapter<TrophyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrophyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trophy, parent, false)
        return TrophyViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrophyViewHolder, position: Int) {
        // Bind data to the ViewHolder (Android Developers, 2025; ChatGPT-4, 2025)
        val trophy = trophies[position]
        val isEarned = userTrophies.contains(trophy.id) // check by ID
        holder.bind(trophy, isEarned, onItemClick)
        Log.d("TrophyAdapter", "Binding ${trophy.id}, earned=${isEarned}")

    }

    override fun getItemCount(): Int = trophies.size
}
/*
 * Reference List
 *
 * Android Developers, 2025. Developer centers. [online]. Available at:
 * https://developer.android.com/
 * [Accessed: 6 October 2025].
 *
 * ChatGPT-4, 2025. OpenAI. [online]. Available at:
 * https://chatgpt.com/?model=auto
 * [Accessed: 6 October 2025].
 */
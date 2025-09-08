package com.example.arachnophobia_galaxy_infestation

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView

class TrophyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val trophyImage: ImageView = itemView.findViewById(R.id.trophyImage)
    private val trophyName: TextView = itemView.findViewById(R.id.trophyName)
    private val trophyCheckBox: CheckBox = itemView.findViewById(R.id.trophyCheckBox)

    fun bind(trophy: Trophy, isEarned: Boolean, onItemClick: (Trophy) -> Unit) {
        trophyName.text = trophy.name
        trophyCheckBox.isChecked = isEarned

        Glide.with(itemView.context)
            .load(trophy.image_url)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(trophyImage)

        itemView.setOnClickListener {
            onItemClick(trophy)
        }
    }
}
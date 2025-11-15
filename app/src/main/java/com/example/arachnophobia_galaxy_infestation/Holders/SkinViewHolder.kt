package com.example.arachnophobia_galaxy_infestation

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SkinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val skinImage: ImageView = itemView.findViewById(R.id.skinImage)
    val skinName: TextView = itemView.findViewById(R.id.skinName)
    val skinPrice: TextView = itemView.findViewById(R.id.skinPrice)
    val actionButton: Button = itemView.findViewById(R.id.actionButton)
}
package com.example.arachnophobia_galaxy_infestation

import android.graphics.Color
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView

class TrophyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val trophyImage: ImageView = itemView.findViewById(R.id.trophyImage)
    private val trophyName: TextView = itemView.findViewById(R.id.trophyName)
    private val trophyCheckBox: CheckBox = itemView.findViewById(R.id.trophyCheckBox)
    private val container: View = itemView.findViewById(R.id.trophyItem)

    fun bind(trophy: Trophy, isEarned: Boolean, onItemClick: (Trophy) -> Unit) {
        trophyName.text = trophy.name
        trophyCheckBox.isChecked = isEarned

        val context = itemView.context
        val resId = context.resources.getIdentifier(trophy.image_url, "drawable", context.packageName)

        if (resId != 0) {
            trophyImage.setImageResource(resId)
        } else {
            trophyImage.setImageResource(R.drawable.ic_launcher_foreground) // fallback
        }

        // Highlight background if trophy is earned
        if (isEarned) {
            container.background = ContextCompat.getDrawable(context, R.drawable.custom_button)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.setOnClickListener {
            onItemClick(trophy)
        }
    }
}
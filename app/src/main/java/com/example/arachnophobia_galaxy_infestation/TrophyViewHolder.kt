package com.example.arachnophobia_galaxy_infestation

import android.graphics.Color
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrophyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val trophyImage: ImageView = itemView.findViewById(R.id.trophyImage)
    private val trophyName: TextView = itemView.findViewById(R.id.trophyName)
    private val trophyCheckBox: CheckBox = itemView.findViewById(R.id.trophyCheckBox)

    fun bind(trophy: Trophy, isEarned: Boolean, onItemClick: (Trophy) -> Unit) {
        trophyName.text = trophy.name
        trophyCheckBox.isChecked = isEarned

        // Set image (Android Developers, 2025; ChatGPT-4, 2025)
        val context = itemView.context
        val resId = context.resources.getIdentifier(trophy.image_url, "drawable", context.packageName)

        if (resId != 0) {
            trophyImage.setImageResource(resId)
        } else {
            trophyImage.setImageResource(R.drawable.ic_launcher_foreground) // fallback (Android Developers, 2025; ChatGPT-4, 2025)
        }

        itemView.setBackgroundColor(Color.TRANSPARENT)

        itemView.setOnClickListener {
            onItemClick(trophy)
        }
    }
}
/*
 * Reference List
 *
 * Android Developers, 2025. Developer centers. [online]. Available at:
 * https://developer.android.com/
 * [Accessed: 6 October 2025].
 *
 * Android Developers, 2025. Fragment transactions. [online]. Available at:
 * https://developer.android.com/guide/fragments/transactions
 * [Accessed: 6 October 2025].
 *
 * ChatGPT-4, 2025. OpenAI. [online]. Available at:
 * https://chatgpt.com/?model=auto
 * [Accessed: 6 October 2025].
 */
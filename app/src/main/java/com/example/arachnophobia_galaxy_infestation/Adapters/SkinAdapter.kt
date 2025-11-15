package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SkinAdapter(
    private val skins: List<Skin>,
    private var player: Player,
    private val onSkinAction: (Skin) -> Unit
) : RecyclerView.Adapter<SkinAdapter.SkinViewHolder>() {

    // ViewHolder class
    inner class SkinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val skinImage: ImageView = view.findViewById(R.id.skinImage)
        val skinName: TextView = view.findViewById(R.id.skinName)
        val skinPrice: TextView = view.findViewById(R.id.skinPrice)
        val actionButton: Button = view.findViewById(R.id.actionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]
        val ownedSkins = player.ownedSkins ?: emptyList()

        // SharedPreferences for equipped skin (Android Developers,2025)
        val prefs = holder.itemView.context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkin = prefs.getString("equippedSkin", "Moth") ?: "Moth"

        holder.skinName.text = skin.name
        holder.skinPrice.text =
            if (ownedSkins.contains(skin.name)) "Owned" else "${skin.price} silk"

        // Load image (Android Developers,2025)
        val resId = holder.itemView.context.resources.getIdentifier(
            skin.image_url, "drawable", holder.itemView.context.packageName
        )
        holder.skinImage.setImageResource(if (resId != 0) resId else R.drawable.moth)

        // Set button text and state (Android Developers,2025)
        holder.actionButton.text = when {
            equippedSkin == skin.name -> "Equipped"
            ownedSkins.contains(skin.name) -> "Equip"
            else -> "Buy"
        }
        holder.actionButton.isEnabled = holder.actionButton.text != "Equipped"

        // Button click (Android Developers,2025)
        holder.actionButton.setOnClickListener {
            onSkinAction(skin)
        }
    }

    override fun getItemCount() = skins.size

    fun updatePlayer(newPlayer: Player) {
        player = newPlayer
        notifyDataSetChanged()
    }
}
/*
* Reference List
*
* Android Developers, 2025. RecyclerView.Adapter. [online]. Available at:
* https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. ViewHolder. [online]. Available at:
* https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. LayoutInflater. [online]. Available at:
* https://developer.android.com/reference/android/view/LayoutInflater
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Resources.getIdentifier. [online]. Available at:
* https://developer.android.com/reference/android/content/res/Resources#getIdentifier(java.lang.String,%20java.lang.String,%20java.lang.String)
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. ImageView. [online]. Available at:
* https://developer.android.com/reference/android/widget/ImageView
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. TextView. [online]. Available at:
* https://developer.android.com/reference/android/widget/TextView
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Button. [online]. Available at:
* https://developer.android.com/reference/android/widget/Button
* [Accessed: 7 October 2025].
*/
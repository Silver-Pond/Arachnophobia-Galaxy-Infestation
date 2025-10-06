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

        // SharedPreferences for equipped skin
        val prefs = holder.itemView.context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkin = prefs.getString("equippedSkin", "Moth") ?: "Moth"

        holder.skinName.text = skin.name
        holder.skinPrice.text =
            if (ownedSkins.contains(skin.name)) "Owned" else "${skin.price} silk"

        // Load image
        val resId = holder.itemView.context.resources.getIdentifier(
            skin.image_url, "drawable", holder.itemView.context.packageName
        )
        holder.skinImage.setImageResource(if (resId != 0) resId else R.drawable.moth)

        // Set button text and state
        holder.actionButton.text = when {
            equippedSkin == skin.name -> "Equipped"
            ownedSkins.contains(skin.name) -> "Equip"
            else -> "Buy"
        }
        holder.actionButton.isEnabled = holder.actionButton.text != "Equipped"

        // Button click
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
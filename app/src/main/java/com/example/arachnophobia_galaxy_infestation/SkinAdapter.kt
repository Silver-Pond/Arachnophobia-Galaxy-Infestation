package com.example.arachnophobia_galaxy_infestation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SkinAdapter(
    private val skins: List<Skin>,
    private var player: Player,
    private val onSkinAction: (Skin) -> Unit
) : RecyclerView.Adapter<SkinViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]
        holder.skinName.text = skin.name
        holder.skinPrice.text = if (player.ownedSkins.contains(skin.name)) "Owned" else "${skin.price} silk"

        // Load drawable from resources
        val resId = holder.itemView.context.resources.getIdentifier(
            skin.image_url, "drawable", holder.itemView.context.packageName
        )
        holder.skinImage.setImageResource(resId)

        // Update button text
        holder.actionButton.text = when {
            player.equippedSkin == skin.name -> "Equipped"
            player.ownedSkins.contains(skin.name) -> "Equip"
            else -> "Buy"
        }

        holder.actionButton.isEnabled = holder.actionButton.text != "Equipped"

        holder.actionButton.setOnClickListener {
            onSkinAction(skin)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = skins.size

    fun updatePlayer(newPlayer: Player) {
        player = newPlayer
        notifyDataSetChanged()
    }
}
package com.example.arachnophobia_galaxy_infestation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(
    private val players: List<HighScore>,
    private val loggedInUser: String
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rankText)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val scoreText: TextView = itemView.findViewById(R.id.scoreText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = players.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = players[position]
        holder.rankText.text = "${position + 1}."
        holder.usernameText.text = player.username
        holder.scoreText.text = player.score.toString()

        // Highlight logged-in user
        if (player.username == loggedInUser) {
            holder.itemView.setBackgroundColor(Color.rgb(85,48,101))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}

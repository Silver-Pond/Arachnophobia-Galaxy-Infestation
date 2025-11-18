package com.example.arachnophobia_galaxy_infestation

import android.graphics.Color
import android.view.View
import android.widget.TextView
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.Mockito.mockStatic

class LeaderboardAdapterTest {

    private lateinit var mockRankText: TextView
    private lateinit var mockUsernameText: TextView
    private lateinit var mockScoreText: TextView
    private lateinit var mockItemView: View
    private lateinit var mockHolder: LeaderboardAdapter.ViewHolder

    private val players = listOf(
        HighScore("Alice", 100),
        HighScore("Bob", 200)
    )
    private val loggedInUser = "Bob"

    private lateinit var adapter: LeaderboardAdapter

    @Before
    fun setup() {
        mockItemView = mock(View::class.java)
        mockRankText = mock(TextView::class.java)
        mockUsernameText = mock(TextView::class.java)
        mockScoreText = mock(TextView::class.java)

        `when`(mockItemView.findViewById<TextView>(R.id.rankText)).thenReturn(mockRankText)
        `when`(mockItemView.findViewById<TextView>(R.id.usernameText)).thenReturn(mockUsernameText)
        `when`(mockItemView.findViewById<TextView>(R.id.scoreText)).thenReturn(mockScoreText)

        mockHolder = LeaderboardAdapter.ViewHolder(mockItemView)

        adapter = LeaderboardAdapter(players, loggedInUser)
    }

    @Test
    fun `getItemCount returns correct size`() {
        assertEquals(players.size, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder sets texts correctly`() {
        // Mock Color.rgb to return a fixed int (ChatGPT-4, 2025)
        mockStatic(Color::class.java).use { colorMock ->
            `when`(Color.rgb(85, 48, 101)).thenReturn(123456)

            // Bind first player (Alice) (ChatGPT-4, 2025)
            adapter.onBindViewHolder(mockHolder, 0)
            verify(mockRankText).text = "1."
            verify(mockUsernameText).text = "Alice"
            verify(mockScoreText).text = "100"
            verify(mockItemView).setBackgroundColor(Color.TRANSPARENT)

            // Bind logged-in user (Bob) (ChatGPT-4, 2025)
            adapter.onBindViewHolder(mockHolder, 1)
            verify(mockRankText).text = "2."
            verify(mockUsernameText).text = "Bob"
            verify(mockScoreText).text = "200"
            verify(mockItemView).setBackgroundColor(123456) // mocked color
        }
    }
}
/*
* Reference List
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/
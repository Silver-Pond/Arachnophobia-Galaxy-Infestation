package com.example.arachnophobia_galaxy_infestation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class TrophyAdapterTest {

    private lateinit var trophies: List<Trophy>
    private lateinit var earnedTrophies: Set<String>
    private lateinit var mockClickListener: (Trophy) -> Unit
    private lateinit var adapter: TrophyAdapter

    @Before
    fun setUp() {
        trophies = listOf(
            Trophy("1", "First Blood", "Earned your first kill"),
            Trophy("2", "Explorer", "Visited 5 planets"),
            Trophy("3", "Collector", "Collected 100 gems")
        )
        earnedTrophies = setOf("1", "3")
        mockClickListener = mock()
        adapter = TrophyAdapter(trophies, earnedTrophies, mockClickListener)
    }

    @Test
    fun `getItemCount should return correct size`() {
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder should call bind with correct isEarned value`() {
        val mockHolder = mock<TrophyViewHolder>()
        trophies.forEachIndexed { index, trophy ->
            adapter.onBindViewHolder(mockHolder, index)
            verify(mockHolder).bind(eq(trophy), eq(earnedTrophies.contains(trophy.id)), any())
        }
    }
}
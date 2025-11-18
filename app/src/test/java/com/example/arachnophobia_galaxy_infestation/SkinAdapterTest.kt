package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import kotlin.test.assertEquals

class SkinAdapterTest {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockPrefs: SharedPreferences

    @Mock
    lateinit var mockView: View

    @Mock
    lateinit var skinImage: ImageView

    @Mock
    lateinit var skinName: TextView

    @Mock
    lateinit var skinPrice: TextView

    @Mock
    lateinit var actionButton: Button

    lateinit var adapter: SkinAdapter
    lateinit var skins: List<Skin>
    lateinit var player: Player

    @Mock
    lateinit var mockResources: Resources

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Sample data (ChatGPT-4, 2025)
        skins = listOf(
            Skin(id = "1", name = "Moth", price = 100.0, image_url = "moth_image"),
            Skin(id = "2", name = "Spider", price = 200.0, image_url = "spider_image"),
            Skin(id = "3", name = "Bee", price = 300.0, image_url = "bee_image"),
            Skin(id = "4", name = "Fly", price = 400.0, image_url = "fly_image")
        )
        player = Player(ownedSkins = listOf("Moth"))

        // Mock view hierarchy
        `when`(mockView.context).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        `when`(mockPrefs.getString("equippedSkin", "Moth")).thenReturn("Moth")

        // Mock Resources
        `when`(mockContext.resources).thenReturn(mockResources)
        `when`(mockResources.getIdentifier(any<String>(), any<String>(), any<String>())).thenReturn(123)

        // Mock view findViewById
        `when`(mockView.findViewById<ImageView>(any())).thenReturn(skinImage)
        `when`(mockView.findViewById<TextView>(eq(R.id.skinName))).thenReturn(skinName)
        `when`(mockView.findViewById<TextView>(eq(R.id.skinPrice))).thenReturn(skinPrice)
        `when`(mockView.findViewById<Button>(eq(R.id.actionButton))).thenReturn(actionButton)

        adapter = SkinAdapter(skins, player) {}
    }

    @Test
    fun `bind sets skin name and price correctly`() {
        val holder = adapter.SkinViewHolder(mockView)
        adapter.onBindViewHolder(holder, 0)

        verify(skinName).setText("Moth")
        verify(skinPrice).setText("Owned")
    }

    @Test
    fun `bind sets button text correctly for owned skin`() {
        val holder = adapter.SkinViewHolder(mockView)
        adapter.onBindViewHolder(holder, 0) // Moth, owned, equipped

        verify(actionButton).setText("Equipped")
        verify(actionButton).setEnabled(false)
    }

    @Test
    fun `bind sets button text correctly for not owned skin`() {
        val holder = adapter.SkinViewHolder(mockView)
        adapter.onBindViewHolder(holder, 1) // Spider, not owned

        verify(actionButton).setText(eq("Buy"))
        verify(actionButton).setEnabled(eq(true))
    }

    @Test
    fun `bind sets button text correctly when not owned`() {
        val newPlayer = Player(ownedSkins = emptyList())
        val newAdapter = SkinAdapter(skins, newPlayer) {}
        val holder = newAdapter.SkinViewHolder(mockView)
        newAdapter.onBindViewHolder(holder, 1)

        verify(actionButton).setText("Buy")
        verify(actionButton).setEnabled(true)
    }

    @Test
    fun `clicking actionButton calls onSkinAction`() {
        var clickedSkin: Skin? = null

        // Create the adapter with the callback
        val testAdapter = SkinAdapter(skins, player) { clickedSkin = it }

        // Prepare the ViewHolder
        val holder = testAdapter.SkinViewHolder(mockView)

        // Mock setOnClickListener to call the listener immediately (ChatGPT-4, 2025)
        doAnswer { invocation ->
            val listener = invocation.getArgument<View.OnClickListener>(0)
            // Simulate clicking immediately
            listener.onClick(mockView)
        }.`when`(actionButton).setOnClickListener(any())

        // Bind the view holder
        testAdapter.onBindViewHolder(holder, 0)

        // Verify the listener was set at least once (ChatGPT-4, 2025)
        verify(actionButton, atLeastOnce()).setOnClickListener(any())

        // Assert that the click callback was invoked with the correct skin object (ChatGPT-4, 2025)
        assertEquals(skins[0], clickedSkin)
    }
}
/*
* Reference List
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/
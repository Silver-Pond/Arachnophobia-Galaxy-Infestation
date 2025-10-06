package com.example.arachnophobia_galaxy_infestation

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull

class SkinViewHolderTest {

    private lateinit var itemView: View
    private lateinit var skinImage: ImageView
    private lateinit var skinName: TextView
    private lateinit var skinPrice: TextView
    private lateinit var actionButton: Button
    private lateinit var viewHolder: SkinViewHolder

    @Before
    fun setup() {
        // Mock the itemView and its child views
        itemView = mock(View::class.java)
        skinImage = mock(ImageView::class.java)
        skinName = mock(TextView::class.java)
        skinPrice = mock(TextView::class.java)
        actionButton = mock(Button::class.java)

        // Mock findViewById calls
        `when`(itemView.findViewById<ImageView>(R.id.skinImage)).thenReturn(skinImage)
        `when`(itemView.findViewById<TextView>(R.id.skinName)).thenReturn(skinName)
        `when`(itemView.findViewById<TextView>(R.id.skinPrice)).thenReturn(skinPrice)
        `when`(itemView.findViewById<Button>(R.id.actionButton)).thenReturn(actionButton)

        // Create the ViewHolder with the mocked itemView
        viewHolder = SkinViewHolder(itemView)
    }

    @Test
    fun `constructor initializes all views`() {
        assertNotNull(viewHolder.skinImage)
        assertNotNull(viewHolder.skinName)
        assertNotNull(viewHolder.skinPrice)
        assertNotNull(viewHolder.actionButton)
    }

    @Test
    fun `actionButton click can be triggered`() {
        // simulate click
        viewHolder.actionButton.performClick()
        // verify that the method can be called
        verify(actionButton).performClick()
    }

    @Test
    fun `views returned match mocked views`() {
        assertEquals(skinImage, viewHolder.skinImage)
        assertEquals(skinName, viewHolder.skinName)
        assertEquals(skinPrice, viewHolder.skinPrice)
        assertEquals(actionButton, viewHolder.actionButton)
    }
}
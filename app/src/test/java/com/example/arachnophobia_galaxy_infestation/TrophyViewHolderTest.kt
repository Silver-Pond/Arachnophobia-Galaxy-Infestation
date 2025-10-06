package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class TrophyViewHolderTest {

    private lateinit var context: Context
    private lateinit var itemView: View
    private lateinit var trophyImage: ImageView
    private lateinit var trophyName: TextView
    private lateinit var trophyCheckBox: CheckBox
    private lateinit var viewHolder: TrophyViewHolder

    @Before
    fun setup() {
        // Mock Android views
        context = mock(Context::class.java)
        itemView = mock(View::class.java)
        trophyImage = mock(ImageView::class.java)
        trophyName = mock(TextView::class.java)
        trophyCheckBox = mock(CheckBox::class.java)

        // When itemView.findViewById is called, return the mocks
        `when`(itemView.findViewById<ImageView>(R.id.trophyImage)).thenReturn(trophyImage)
        `when`(itemView.findViewById<TextView>(R.id.trophyName)).thenReturn(trophyName)
        `when`(itemView.findViewById<CheckBox>(R.id.trophyCheckBox)).thenReturn(trophyCheckBox)
        `when`(itemView.context).thenReturn(context)
        `when`(context.resources).thenReturn(mock(android.content.res.Resources::class.java))

        viewHolder = TrophyViewHolder(itemView)
    }

    @Test
    fun `bind sets trophy name correctly`() {
        val trophy = Trophy("Spider Slayer", "Spider Slayer")
        viewHolder.bind(trophy, false) {}

        verify(trophyName).text = "Spider Slayer"  // Now works because trophyName is a mock
    }

    @Test
    fun `bind sets checkbox state correctly when earned`() {
        val trophy = Trophy("Web Collector", "web_image")
        viewHolder.bind(trophy, true) {}

        verify(trophyCheckBox).isChecked = true
    }

    @Test
    fun `bind sets checkbox state correctly when not earned`() {
        val trophy = Trophy("Web Collector", "web_image")
        viewHolder.bind(trophy, false) {}

        verify(trophyCheckBox).isChecked = false
    }

    @Test
    fun `bind sets image resource if drawable exists`() {
        val trophy = Trophy("Spider Slayer", "ic_launcher_foreground")
        val resId = 123
        val resources = mock(android.content.res.Resources::class.java)
        `when`(context.resources.getIdentifier(trophy.image_url, "drawable", context.packageName)).thenReturn(resId)

        viewHolder.bind(trophy, false) {}

        verify(trophyImage).setImageResource(resId)
    }

    @Test
    fun `bind sets fallback image if drawable not found`() {
        val trophy = Trophy("Unknown Trophy", "non_existent_image")
        val resources = mock(android.content.res.Resources::class.java)
        `when`(context.resources.getIdentifier(trophy.image_url, "drawable", context.packageName)).thenReturn(0)

        viewHolder.bind(trophy, false) {}

        verify(trophyImage).setImageResource(R.drawable.ic_launcher_foreground)
    }

    @Test
    fun `bind calls onItemClick when itemView clicked`() {
        val trophy = Trophy("Spider Slayer", "ic_launcher_foreground")
        var clickedTrophy: Trophy? = null

        val captor = argumentCaptor<View.OnClickListener>()
        viewHolder.bind(trophy, false) { clickedTrophy = it }

        verify(itemView).setOnClickListener(captor.capture())
        captor.firstValue.onClick(mock(View::class.java))

        assertEquals(trophy, clickedTrophy)
    }
}
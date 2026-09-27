package com.commitpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetScalingTest {

    @Test
    fun `scale factor is 1 at the reference width`() {
        assertEquals(1f, widgetScaleFactor(110f, referenceWidthDp = 110f), 0.001f)
    }

    @Test
    fun `scale factor grows for a wider widget`() {
        assertEquals(1.5f, widgetScaleFactor(220f, referenceWidthDp = 110f), 0.001f)
    }

    @Test
    fun `scale factor shrinks for a narrower widget`() {
        assertEquals(0.6f, widgetScaleFactor(40f, referenceWidthDp = 110f), 0.001f)
    }

    @Test
    fun `scale factor is clamped to the configured min and max`() {
        assertEquals(0.6f, widgetScaleFactor(1f, referenceWidthDp = 110f, min = 0.6f, max = 1.5f), 0.001f)
        assertEquals(1.5f, widgetScaleFactor(1000f, referenceWidthDp = 110f, min = 0.6f, max = 1.5f), 0.001f)
    }
}

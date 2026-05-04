package com.harmonixia.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class EqGraphLabelFormatterTest {

    private val templates = EqGraphLabelTemplates(
        frequencyIntegerKFormat = "%1\$dkX",
        frequencyDecimalKFormat = "%1$.1fkX",
        gainPositiveFormat = "+%1\$d",
        gainDefaultFormat = "%1\$d"
    )

    @Test
    fun formatFrequencyLabel_usesIntegerKTemplateForWholeThousands() {
        assertEquals("1kX", EqGraphLabelFormatter.formatFrequencyLabel(1000, templates))
    }

    @Test
    fun formatFrequencyLabel_usesDecimalKTemplateForFractionalThousands() {
        assertEquals("1.5kX", EqGraphLabelFormatter.formatFrequencyLabel(1500, templates))
    }

    @Test
    fun formatFrequencyLabel_keepsRawHertzBelow1000() {
        assertEquals("500", EqGraphLabelFormatter.formatFrequencyLabel(500, templates))
    }

    @Test
    fun formatGainLabel_matchesPositiveAndNonPositiveBehavior() {
        assertEquals("+6", EqGraphLabelFormatter.formatGainLabel(6, templates))
        assertEquals("0", EqGraphLabelFormatter.formatGainLabel(0, templates))
        assertEquals("-12", EqGraphLabelFormatter.formatGainLabel(-12, templates))
    }
}

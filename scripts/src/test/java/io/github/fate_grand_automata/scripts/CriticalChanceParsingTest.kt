package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.locations.AttackScreenLocations
import io.github.fate_grand_automata.scripts.locations.Locations
import io.github.fate_grand_automata.scripts.modules.CardParser
import io.github.fate_grand_automata.scripts.modules.CriticalChanceDetector
import io.github.fate_grand_automata.scripts.modules.ServantTracker
import io.github.fate_grand_automata.scripts.prefs.IPreferences
import io.github.lib_automata.Region
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertTrue

class CriticalChanceParsingTest {
    @Test
    fun disabledParsingSkipsCriticalDetector() {
        val api = mockk<IFgoAutomataApi>(relaxed = true)
        val prefs = mockk<IPreferences>(relaxed = true)
        val locations = mockk<Locations>(relaxed = true)
        val attack = mockk<AttackScreenLocations>(relaxed = true)
        val tracker = mockk<ServantTracker>(relaxed = true)
        val detector = mockk<CriticalChanceDetector>(relaxed = true)

        every { api.prefs } returns prefs
        every { prefs.skipServantFaceCardCheck } returns true
        every { api.locations } returns locations
        every { locations.attack } returns attack
        every { attack.typeRegion(any()) } returns Region(0, 0, 100, 100)
        every { attack.affinityRegion(any()) } returns Region(0, 0, 100, 100)
        every { tracker.faceCardsGroupedByServant() } returns emptyMap()
        every { tracker.deployed } returns emptyMap()

        val cards = CardParser(api, tracker, detector).parse(includeCriticalChance = false)

        assertTrue(cards.all { it.criticalChance == null })
        verify(exactly = 0) { detector.detect(any()) }
    }
}
